package com.tw.downloader.viewmodel

import android.app.Application
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tw.downloader.data.model.*
import com.tw.downloader.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

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
    var currentPage by mutableIntStateOf(1)
        private set
    var currentTag by mutableStateOf("")
        private set
    var config by mutableStateOf(WaterfallConfig())
        private set
    var selectMode by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf(emptySet<String>())
        private set
    var downloadedIds by mutableStateOf(emptySet<String>())
        private set
    var downloading by mutableStateOf(false)
        private set
    var downloadProgress by mutableIntStateOf(0)
        private set
    var downloadCurrentIndex by mutableIntStateOf(0)
        private set
    var downloadTotalCount by mutableIntStateOf(0)
        private set
    var showSettings by mutableStateOf(false)
        private set

    private var downloadJob: Job? = null

    init {
        config = repo.getWaterfallConfig()
        downloadedIds = repo.getDownloadedIds()
        loadData()
    }

    fun refreshDownloadedIds() {
        downloadedIds = repo.getDownloadedIds()
    }

    fun refreshProxy() {
        repo.refreshApi()
    }

    fun setShowSettings(show: Boolean) {
        showSettings = show
    }

    fun switchTab(tag: String) {
        if (tag == currentTag || loading) return
        currentTag = tag
        currentPage = 1
        hasNext = false
        loadData()
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

    fun saveConfig(newConfig: WaterfallConfig) {
        config = newConfig
        repo.saveWaterfallConfig(newConfig)
        showSettings = false
        currentPage = 1
        hasNext = false
        loadData()
    }

    fun loadData() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            loadError = ""
            selectedIds = emptySet()
            try {
                val (result, next) = repo.fetchMedia(1, config, currentTag)
                items = result
                currentPage = 1
                hasNext = next
            } catch (e: Exception) {
                items = emptyList()
                loadError = e.message ?: "加载失败"
            } finally {
                loading = false
            }
        }
    }

    fun loadMore() {
        if (loadingMore || !hasNext || loading) return
        viewModelScope.launch {
            loadingMore = true
            val nextPage = currentPage + 1
            try {
                val (result, next) = repo.fetchMedia(nextPage, config, currentTag)
                items = items + result
                currentPage = nextPage
                hasNext = next
            } catch (_: Exception) {
            } finally {
                loadingMore = false
            }
        }
    }

    fun downloadSelected() {
        if (downloading) {
            downloadJob?.cancel()
            downloading = false
            return
        }
        val selected = items.filter { it.id in selectedIds }
        if (selected.isEmpty()) return

        downloading = true
        downloadTotalCount = selected.size
        downloadCurrentIndex = 0
        downloadProgress = 0

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "TwDownloader"
            )
            downloadDir.mkdirs()

            var failed = 0
            for ((i, item) in selected.withIndex()) {
                downloadCurrentIndex = i + 1
                downloadProgress = 0
                try {
                    val ext = item.url.substringAfterLast('.', "mp4").take(4)
                    val file = File(downloadDir, "${item.id}.$ext")

                    val proxyConfig = repo.getProxyConfig()
                    val connection = if (proxyConfig.enabled && proxyConfig.selectedId.isNotEmpty()) {
                        val scheme = proxyConfig.schemes.find { it.id == proxyConfig.selectedId }
                        if (scheme != null) {
                            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(scheme.host, scheme.port))
                            URL(item.url).openConnection(proxy) as HttpURLConnection
                        } else {
                            URL(item.url).openConnection() as HttpURLConnection
                        }
                    } else {
                        URL(item.url).openConnection() as HttpURLConnection
                    }

                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    connection.connect()

                    val totalBytes = connection.contentLength.toLong()
                    var downloadedBytes = 0L

                    connection.inputStream.use { input ->
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                if (totalBytes > 0) {
                                    downloadProgress = ((downloadedBytes * 100) / totalBytes).toInt()
                                }
                            }
                        }
                    }

                    val record = DownloadRecord(
                        id = item.id,
                        title = item.title,
                        thumbnail = item.thumbnail,
                        url = item.url,
                        filePath = file.absolutePath,
                        downloadedAt = System.currentTimeMillis(),
                    )
                    repo.saveDownloadRecord(record)
                    withContext(Dispatchers.Main) {
                        downloadedIds = downloadedIds + item.id
                    }
                } catch (_: Exception) {
                    failed++
                }
            }

            withContext(Dispatchers.Main) {
                downloading = false
                downloadProgress = 0
                downloadCurrentIndex = 0
                downloadTotalCount = 0
                selectedIds = emptySet()
            }
        }
    }
}
