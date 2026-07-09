package com.tw.downloader.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tw.downloader.TwApp
import com.tw.downloader.data.model.*
import com.tw.downloader.data.repository.MediaRepository
import com.tw.downloader.ui.screens.VideoEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy as JavaProxy

class WaterfallViewModel(app: Application) : AndroidViewModel(app) {
    val repo = MediaRepository(app)

    var items by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var loadError by mutableStateOf("")
        private set
    var hasNext by mutableStateOf(false)
        private set
    var selectMode by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf(emptySet<String>())
        private set
    var downloadedIds by mutableStateOf(emptySet<String>())
        private set
    var downloading by mutableStateOf(false)
        private set
    var downloadingIds by mutableStateOf(emptySet<String>())
        private set
    var downloadProgressMap by mutableStateOf(emptyMap<String, Int>())
        private set
    var showSettings by mutableStateOf(false)
        private set
    var currentRange by mutableStateOf("")
        private set

    // Update check state
    var checkingUpdate by mutableStateOf(false)
        private set
    var updateError by mutableStateOf("")
        private set
    var updateDownloading by mutableStateOf(false)
        private set
    var updateProgress by mutableStateOf(0)
        private set
    var showUpdateDialog by mutableStateOf(false)
        private set
    var latestVersion by mutableStateOf("")
        private set
    var latestApkUrl by mutableStateOf("")
        private set

    private var downloadJob: Job? = null
    private val fileSizeCache = mutableMapOf<String, Long>()
    private var fileSizeJob: Job? = null
    private var preloadJob: Job? = null

    private val notificationManager by lazy {
        app.getSystemService(NotificationManager::class.java)
    }

    init {
        loadData()
    }

    fun refreshProxy() {
        repo.refreshApi()
    }

    fun updateShowSettings(show: Boolean) {
        showSettings = show
    }

    fun toggleSelectMode() {
        selectMode = !selectMode
        if (!selectMode) selectedIds = emptySet()
    }

    fun toggleSelect(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun toggleSelectAll() {
        val allIds = items.map { it.id }.toSet()
        selectedIds = if (allIds == selectedIds) emptySet() else allIds
    }

    fun changeRange(range: String) {
        if (range == currentRange) return
        currentRange = range
        loadData()
    }

    fun loadData() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            loadError = ""
            selectedIds = emptySet()
            try {
                val result = repo.fetchMedia(currentRange)
                items = result
                hasNext = false
                preloadThumbnails(result)
                fetchFileSizes(result)
            } catch (e: Exception) {
                items = emptyList()
                loadError = e.message ?: "加载失败"
            } finally {
                loading = false
            }
        }
    }

    fun loadMore() {
        // Single-page response - no pagination
    }

    fun dismissUpdateDialog() {
        showUpdateDialog = false
        updateError = ""
    }

    /**
     * Check latest release on GitHub and prompt user to download.
     */
    fun checkUpdate() {
        if (checkingUpdate || updateDownloading) return
        viewModelScope.launch {
            checkingUpdate = true
            updateError = ""
            try {
                val (version, apkUrl) = withContext(Dispatchers.IO) {
                    val client = buildProxyClient()
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/fhyxz001/twAndroid/releases/latest")
                        .header("Accept", "application/json")
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                        ?: throw Exception("空响应")

                    val release = Json { ignoreUnknownKeys = true }
                        .decodeFromString<GitHubRelease>(body)
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                        ?: throw Exception("未找到 APK 下载文件")
                    Pair(release.tag_name, apkAsset.browser_download_url)
                }

                latestVersion = version
                latestApkUrl = apkUrl
                showUpdateDialog = true
            } catch (e: Exception) {
                updateError = e.message ?: "检查更新失败"
                latestVersion = ""
                latestApkUrl = ""
                showUpdateDialog = true
            } finally {
                checkingUpdate = false
            }
        }
    }

    /**
     * Download the latest APK to Downloads directory via the proxy client.
     */
    fun downloadUpdate() {
        if (latestApkUrl.isEmpty() || updateDownloading) return
        viewModelScope.launch(Dispatchers.IO) {
            updateDownloading = true
            updateProgress = 0
            try {
                val app = getApplication<Application>()
                val client = buildProxyClient()
                val request = Request.Builder().url(latestApkUrl).get().build()
                val response = client.newCall(request).execute()
                val body = response.body ?: throw Exception("下载失败")

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val fileName = "X下载器-$latestVersion.apk"

                // Save via MediaStore to Downloads
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("创建文件失败")

                app.contentResolver.openOutputStream(uri)?.use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0) {
                                val pct = ((downloadedBytes * 100) / totalBytes).toInt()
                                withContext(Dispatchers.Main) { updateProgress = pct }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    updateProgress = 100
                    showUpdateDialog = false
                }
                showCompletionNotification(TwApp.NOTIFICATION_DOWNLOAD_SINGLE, "更新包已下载: $fileName")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateError = e.message ?: "下载失败"
                }
            } finally {
                withContext(Dispatchers.Main) { updateDownloading = false }
            }
        }
    }

    private fun fetchFileSizes(newItems: List<MediaItem>) {
        fileSizeJob?.cancel()
        fileSizeJob = viewModelScope.launch(Dispatchers.IO) {
            val client = buildProxyClient()
            for (item in newItems) {
                if (fileSizeCache.containsKey(item.id)) continue
                try {
                    val request = Request.Builder().url(item.url).head().build()
                    val response = client.newCall(request).execute()
                    val size = response.body?.contentLength() ?: -1L
                    if (size > 0) {
                        fileSizeCache[item.id] = size
                        withContext(Dispatchers.Main) {
                            items = items.map {
                                if (it.id == item.id) it.copy(fileSize = size) else it
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Pre-download thumbnails through proxy and serve from local cache.
     * This bypasses Coil's internal proxy configuration, which may not work
     * reliably for all proxy types. Saved to cacheDir/thumbnails/{id}.jpg.
     */
    private fun preloadThumbnails(newItems: List<MediaItem>) {
        preloadJob?.cancel()
        preloadJob = viewModelScope.launch(Dispatchers.IO) {
            val client = buildProxyClient()
            val cacheDir = File(getApplication<Application>().cacheDir, "thumbnails")
            cacheDir.mkdirs()

            for (item in newItems) {
                if (item.thumbnail.isEmpty() || !item.thumbnail.startsWith("http")) continue

                val cacheFile = File(cacheDir, "${item.id}.jpg")
                if (cacheFile.exists()) {
                    val localUri = cacheFile.toURI().toString()
                    withContext(Dispatchers.Main) {
                        items = items.map {
                            if (it.id == item.id) it.copy(thumbnail = localUri) else it
                        }
                    }
                    continue
                }

                try {
                    val request = Request.Builder().url(item.thumbnail).get().build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) continue
                    val body = response.body ?: continue

                    body.byteStream().use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val localUri = cacheFile.toURI().toString()
                    withContext(Dispatchers.Main) {
                        items = items.map {
                            if (it.id == item.id) it.copy(thumbnail = localUri) else it
                        }
                    }
                } catch (_: Exception) {
                    // Fallback: keep original remote URL, Coil may still load it
                }
            }
        }
    }

    private fun buildProxyClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
        val proxyConfig = repo.getProxyConfig()
        if (proxyConfig.enabled && proxyConfig.selectedId.isNotEmpty()) {
            val scheme = proxyConfig.schemes.find { it.id == proxyConfig.selectedId }
            if (scheme != null) {
                builder.proxy(JavaProxy(JavaProxy.Type.HTTP, InetSocketAddress(scheme.host, scheme.port)))
            }
        }
        return builder.build()
    }

    private suspend fun performDownload(
        client: OkHttpClient,
        id: String,
        url: String,
        title: String,
        thumbnail: String,
        onProgress: suspend (Int) -> Unit,
    ) {
        val app = getApplication<Application>()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body ?: throw IllegalStateException("Empty response")
        val totalBytes = body.contentLength()
        var downloadedBytes = 0L

        val ext = url.substringAfterLast('?', "")
            .substringBefore('#')
            .substringAfterLast('.', "mp4")
            .take(4)
        val fileName = "${id}.$ext"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/TwDownloader")
        }
        val uri = app.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        app.contentResolver.openOutputStream(uri)?.use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0) {
                        onProgress(((downloadedBytes * 100) / totalBytes).toInt())
                    }
                }
            }
        }

        val record = DownloadRecord(
            id = id,
            title = title,
            thumbnail = thumbnail,
            url = url,
            filePath = uri.toString(),
            downloadedAt = System.currentTimeMillis(),
        )
        repo.saveDownloadRecord(record)
    }

    fun downloadSelected() {
        if (downloading) {
            downloadJob?.cancel()
            downloading = false
            notificationManager.cancel(TwApp.NOTIFICATION_DOWNLOAD_BATCH)
            return
        }
        val selected = items.filter { it.id in selectedIds }
        if (selected.isEmpty()) return

        downloading = true
        val total = selected.size
        val activeIds = selected.map { it.id }.toMutableSet()
        downloadingIds = downloadingIds + activeIds

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            val client = buildProxyClient()
            var completed = 0
            var failed = 0

            for ((i, item) in selected.withIndex()) {
                val fileIndex = i + 1
                updateBatchNotification(fileIndex, total, 0)
                try {
                    performDownload(
                        client = client,
                        id = item.id,
                        url = item.url,
                        title = item.title,
                        thumbnail = item.thumbnail,
                        onProgress = { percent ->
                            updateBatchNotification(fileIndex, total, percent)
                        },
                    )
                    completed++
                    withContext(Dispatchers.Main) {
                        downloadedIds = downloadedIds + item.id
                    }
                } catch (_: Exception) {
                    failed++
                }
                withContext(Dispatchers.Main) {
                    downloadingIds = downloadingIds - item.id
                }
            }

            withContext(Dispatchers.Main) {
                downloading = false
                selectedIds = emptySet()
            }

            showCompletionNotification(
                TwApp.NOTIFICATION_DOWNLOAD_BATCH,
                if (failed == 0) "下载完成 ($completed 个文件)"
                else "下载完成 ($completed 成功, $failed 失败)",
            )
        }
    }

    fun downloadSingle(entry: VideoEntry) {
        if (entry.id.isEmpty() || entry.src.isEmpty()) return
        if (entry.id in downloadingIds || entry.id in downloadedIds) return

        downloadingIds = downloadingIds + entry.id
        downloadProgressMap = downloadProgressMap + (entry.id to 0)

        viewModelScope.launch(Dispatchers.IO) {
            val client = buildProxyClient()
            val notifId = TwApp.NOTIFICATION_DOWNLOAD_SINGLE + entry.id.hashCode()

            try {
                updateSingleNotification(notifId, entry.description, 0)
                performDownload(
                    client = client,
                    id = entry.id,
                    url = entry.src,
                    title = entry.description,
                    thumbnail = entry.poster,
                    onProgress = { percent ->
                        updateSingleNotification(notifId, entry.description, percent)
                        withContext(Dispatchers.Main) {
                            downloadProgressMap = downloadProgressMap + (entry.id to percent)
                        }
                    },
                )
                withContext(Dispatchers.Main) {
                    downloadedIds = downloadedIds + entry.id
                }
                showCompletionNotification(notifId, "下载完成: ${entry.description.take(30)}")
            } catch (_: Exception) {
                showCompletionNotification(notifId, "下载失败: ${entry.description.take(30)}")
            } finally {
                withContext(Dispatchers.Main) {
                    downloadingIds = downloadingIds - entry.id
                    downloadProgressMap = downloadProgressMap - entry.id
                }
            }
        }
    }

    private fun updateBatchNotification(current: Int, total: Int, percent: Int) {
        val notification = NotificationCompat.Builder(getApplication(), TwApp.CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("下载中 ($current/$total)")
            .setContentText("$percent%")
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(TwApp.NOTIFICATION_DOWNLOAD_BATCH, notification)
    }

    private fun updateSingleNotification(notifId: Int, title: String, percent: Int) {
        val notification = NotificationCompat.Builder(getApplication(), TwApp.CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("下载中")
            .setContentText(title.take(40))
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(notifId, notification)
    }

    private fun showCompletionNotification(notifId: Int, text: String) {
        val notification = NotificationCompat.Builder(getApplication(), TwApp.CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(text)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notifId, notification)
    }
}

@Serializable
private data class GitHubRelease(
    val tag_name: String = "",
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = "",
)
