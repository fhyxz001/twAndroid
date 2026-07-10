package com.tw.downloader.data.repository

import android.content.Context
import com.tw.downloader.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Data model for pektino.com RSC payload parsing.
 */
@Serializable
private data class RscItem(
    val id: Long = 0,
    val url_cd: String = "",
    val url: String = "",
    val time: Long = 0,
    val thumbnail: String = "",
    val pv: String = "0",
    val favorite: String = "0",
    val tweet_url: String? = null,
    val tweet_account: String? = null,
    val commentCount: Int = 0,
)

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
     * Fetch media list from pektino.com via Next.js RSC payload.
     * @param range "" for 日榜, "weekly" for 周榜, "monthly" for 月榜, "all" for 总榜
     */
    suspend fun fetchMedia(range: String = ""): List<MediaItem> = withContext(Dispatchers.IO) {
        val path = if (range.isEmpty()) BASE_URL else "$BASE_URL/$range"
        val url = "$path?_rsc=1q2w3"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DEFAULT_UA)
            .header("Accept", "text/x-component")
            .header("RSC", "1")
            .header("Next-Router-State-Tree", RSC_STATE_TREE)
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}")
        }
        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response body")

        parseRscPayload(body)
    }

    /**
     * Parse Next.js RSC payload to extract initialItems JSON array.
     * The payload is a streaming text format; we search for "initialItems":[ and
     * extract the full JSON array by tracking bracket depth.
     */
    private fun parseRscPayload(body: String): List<MediaItem> {
        val prefix = "\"initialItems\":"
        val startIdx = body.indexOf(prefix)
        if (startIdx == -1) throw IllegalStateException("initialItems not found in RSC payload")

        var idx = startIdx + prefix.length
        // Skip whitespace
        while (idx < body.length && body[idx] == ' ') idx++
        if (idx >= body.length || body[idx] != '[') {
            throw IllegalStateException("Expected '[' after initialItems:")
        }

        var depth = 0
        val sb = StringBuilder()
        while (idx < body.length) {
            val ch = body[idx]
            sb.append(ch)
            when (ch) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) break
                }
                '"' -> {
                    // Skip string content (handle escapes)
                    idx++
                    while (idx < body.length) {
                        val c = body[idx]
                        sb.append(c)
                        if (c == '\\') {
                            idx++
                            if (idx < body.length) sb.append(body[idx])
                        } else if (c == '"') {
                            break
                        }
                        idx++
                    }
                }
            }
            idx++
        }

        if (depth != 0) throw IllegalStateException("Unclosed initialItems array")

        val itemsJson = sb.toString()
        val rscItems: List<RscItem> = json.decodeFromString(itemsJson)

        return rscItems.map { item ->
            MediaItem(
                id = item.url_cd.ifEmpty { item.id.toString() },
                url = item.url,
                thumbnail = item.thumbnail,
                title = item.tweet_account ?: "",
                duration = item.time,
                favorite = item.favorite.toLongOrNull() ?: 0L,
                pv = item.pv.toLongOrNull() ?: 0L,
                tweetUrl = item.tweet_url ?: "",
                tweetAccount = item.tweet_account ?: "",
            )
        }
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

    companion object {
        private const val BASE_URL = "https://pektino.com/zh-CN"
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val RSC_STATE_TREE =
            "%5B%22%22%2C%7B%22children%22%3A%5B%22%5Blang%5D%22%2C%7B%22children%22%3A%5B%22__PAGE__%22%2C%7B%7D%5D%7D%5D%7D%2Cnull%2Cnull%2Ctrue%5D"
    }
}
