package com.tw.downloader.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.tw.downloader.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class MediaRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("tw_downloader", Context.MODE_PRIVATE)

    private var httpClient: OkHttpClient = buildClient()

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        val proxyConfig = getProxyConfig()
        if (proxyConfig.enabled && proxyConfig.selectedId.isNotEmpty()) {
            val scheme = proxyConfig.schemes.find { it.id == proxyConfig.selectedId }
            if (scheme != null) {
                builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(scheme.host, scheme.port)))
            }
        }

        return builder.build()
    }

    fun refreshApi() {
        httpClient = buildClient()
    }

    /**
     * Fetch media list from `https://ttt.monsnode.com/`.
     * The response is a full HTML page; we parse <div class="listn"> blocks,
     * extracting the video url from <a href="..."> and the cover url from <img src="...">.
     */
    suspend fun fetchMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BASE_URL)
            .header("User-Agent", DEFAULT_UA)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response body")
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}")
        }

        val doc = Jsoup.parse(body)
        val items = mutableListOf<MediaItem>()
        for (div in doc.select("div.listn")) {
            val anchor = div.selectFirst("a[href]") ?: continue
            val videoUrl = anchor.absUrl("href").ifEmpty { anchor.attr("href") }.trim()
            if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) continue

            val img = div.selectFirst("img[src]")
            val coverUrl = img?.absUrl("src")?.ifEmpty { img.attr("src") }?.trim() ?: ""

            items.add(
                MediaItem(
                    id = videoUrl,
                    url = videoUrl,
                    thumbnail = coverUrl,
                    title = "",
                )
            )
        }
        items
    }

    // Download records
    fun getDownloadRecords(): List<DownloadRecord> {
        val raw = prefs.getString("download_records", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<DownloadRecord>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDownloadRecord(record: DownloadRecord) {
        val records = getDownloadRecords().toMutableList()
        records.add(0, record)
        prefs.edit().putString("download_records", json.encodeToString(records)).apply()
    }

    fun deleteDownloadRecords(indices: Set<Int>) {
        val records = getDownloadRecords().toMutableList()
        indices.sortedDescending().forEach { i ->
            if (i in records.indices) records.removeAt(i)
        }
        prefs.edit().putString("download_records", json.encodeToString(records)).apply()
    }

    fun getDownloadedIds(): Set<String> {
        return getDownloadRecords().map { it.id }.toSet()
    }

    // Proxy config
    fun getProxyConfig(): ProxyConfig {
        val raw = prefs.getString("proxy_config", null) ?: return ProxyConfig()
        return try {
            json.decodeFromString<ProxyConfig>(raw)
        } catch (e: Exception) {
            ProxyConfig()
        }
    }

    fun saveProxyConfig(config: ProxyConfig) {
        prefs.edit().putString("proxy_config", json.encodeToString(config)).apply()
    }

    /**
     * Save a video file to Movies/TwDownloader via MediaStore.
     * Returns the content URI string on success, or null on failure.
     */
    fun saveVideoToMediaStore(fileName: String, mimeType: String = "video/mp4"): android.net.Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/TwDownloader")
        }
        return try {
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val BASE_URL = "https://ttt.monsnode.com/"
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
