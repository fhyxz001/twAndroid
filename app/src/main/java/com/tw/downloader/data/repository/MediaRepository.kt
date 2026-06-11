package com.tw.downloader.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.tw.downloader.data.api.MediaApi
import com.tw.downloader.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class MediaRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("tw_downloader", Context.MODE_PRIVATE)

    private var currentApi: MediaApi = buildApi()

    private fun buildApi(): MediaApi {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        val proxyConfig = getProxyConfig()
        if (proxyConfig.enabled && proxyConfig.selectedId.isNotEmpty()) {
            val scheme = proxyConfig.schemes.find { it.id == proxyConfig.selectedId }
            if (scheme != null) {
                builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(scheme.host, scheme.port)))
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://truvaze.com/")
            .client(builder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(MediaApi::class.java)
    }

    fun refreshApi() {
        currentApi = buildApi()
    }

    suspend fun fetchMedia(
        page: Int,
        config: WaterfallConfig,
        category: String,
    ): Pair<List<MediaItem>, Boolean> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "per_page" to config.perPage.toString(),
            "ids" to "",
            "isAnimeOnly" to "0",
            "sort" to config.sort,
        )
        if (config.minTime > 0) params["min_time"] = config.minTime.toString()
        if (config.maxTime < 86400) params["max_time"] = config.maxTime.toString()
        if (category.isNotEmpty()) params["category"] = category
        if (config.range != "daily") params["range"] = config.range

        val response = currentApi.getMedia(params)
        val items = response.items.mapNotNull { it.toMediaItem() }
        val hasNext = response.items.size >= config.perPage
        return items to hasNext
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

    // Config
    fun getWaterfallConfig(): WaterfallConfig {
        val raw = prefs.getString("waterfall_config", null) ?: return WaterfallConfig()
        return try {
            val map = json.decodeFromString<Map<String, String>>(raw)
            WaterfallConfig(
                perPage = map["per_page"]?.toIntOrNull() ?: 10,
                sort = map["sort"] ?: "pv",
                range = map["range"] ?: "daily",
                minTime = map["min_time"]?.toLongOrNull() ?: 0,
                maxTime = map["max_time"]?.toLongOrNull() ?: 86400,
            )
        } catch (e: Exception) {
            WaterfallConfig()
        }
    }

    fun saveWaterfallConfig(config: WaterfallConfig) {
        val map = mapOf(
            "per_page" to config.perPage.toString(),
            "sort" to config.sort,
            "range" to config.range,
            "min_time" to config.minTime.toString(),
            "max_time" to config.maxTime.toString(),
        )
        prefs.edit().putString("waterfall_config", json.encodeToString(map)).apply()
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
}
