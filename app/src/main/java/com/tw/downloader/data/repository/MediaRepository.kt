package com.tw.downloader.data.repository

import android.content.Context
import com.tw.downloader.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Data model for pektino.com Next.js payload parsing.
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
     * Fetch media list from pektino.com.
     *
     * The site is a Next.js App Router application. Its ranking data is embedded in
     * the server-rendered HTML as `self.__next_f.push([1, "..."])` flight chunks
     * instead of being returned by a JSON API / RSC request, so we fetch the page
     * itself and extract the `initialItems` array from those chunks.
     *
     * @param range "" for 日榜, "weekly" for 周榜, "monthly" for 月榜, "all" for 总榜
     */
    suspend fun fetchMedia(range: String = ""): List<MediaItem> = withContext(Dispatchers.IO) {
        val path = if (range.isEmpty()) BASE_URL else "$BASE_URL/$range"

        val request = Request.Builder()
            .url(path)
            .header("User-Agent", DEFAULT_UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("HTTP ${response.code}")
        }
        val body = response.body?.string()
            ?: throw IllegalStateException("Empty response body")

        parseNextData(body)
    }

    /**
     * Parse the Next.js page HTML to extract the initialItems JSON array.
     *
     * The page streams its RSC flight data through inline scripts:
     *   self.__next_f.push([1,"1:\"$Sreact.fragment\"\n4:..."])
     * Each push argument is a JSON array whose second element is a fragment of the
     * flight payload. We decode every chunk, concatenate them in document order and
     * then locate `"initialItems":[` inside the reconstructed payload.
     */
    private fun parseNextData(body: String): List<MediaItem> {
        val prefix = "\"initialItems\":"
        val payload = extractFlightPayload(body)?.takeIf { it.contains(prefix) } ?: body

        val startIdx = payload.indexOf(prefix)
        if (startIdx == -1) throw IllegalStateException("initialItems not found in Next.js payload")

        var idx = startIdx + prefix.length
        while (idx < payload.length && payload[idx].isWhitespace()) idx++
        if (idx >= payload.length || payload[idx] != '[') {
            throw IllegalStateException("Expected '[' after initialItems:")
        }

        val itemsJson = extractBalanced(payload, idx)
            ?: throw IllegalStateException("Unclosed initialItems array")
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

    /**
     * Rebuild the RSC flight payload from all `self.__next_f.push([...])` chunks.
     * Returns null when the page contains no such chunk (caller falls back to raw HTML).
     */
    private fun extractFlightPayload(html: String): String? {
        val marker = "self.__next_f.push("
        val chunks = StringBuilder()
        var searchFrom = 0
        var found = false

        while (true) {
            val markerIdx = html.indexOf(marker, searchFrom)
            if (markerIdx == -1) break

            var idx = markerIdx + marker.length
            while (idx < html.length && html[idx].isWhitespace()) idx++
            if (idx >= html.length || html[idx] != '[') {
                searchFrom = markerIdx + marker.length
                continue
            }

            val arrayJson = extractBalanced(html, idx)
            if (arrayJson != null) {
                try {
                    val chunk = json.parseToJsonElement(arrayJson).jsonArray
                    chunk.getOrNull(1)?.jsonPrimitive?.contentOrNull?.let {
                        chunks.append(it)
                        found = true
                    }
                } catch (_: Exception) {
                    // Ignore malformed chunks and keep scanning
                }
                searchFrom = idx + arrayJson.length
            } else {
                searchFrom = markerIdx + marker.length
            }
        }

        return if (found) chunks.toString() else null
    }

    /**
     * Extract a balanced JSON array starting at [start] (which must point at '[').
     * String contents are skipped so brackets inside strings do not affect depth.
     */
    private fun extractBalanced(text: String, start: Int): String? {
        var depth = 0
        var idx = start
        while (idx < text.length) {
            val ch = text[idx]
            when (ch) {
                '"' -> {
                    idx++
                    while (idx < text.length) {
                        val c = text[idx]
                        if (c == '\\') {
                            idx += 2
                            continue
                        }
                        if (c == '"') break
                        idx++
                    }
                }
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return text.substring(start, idx + 1)
                }
            }
            idx++
        }
        return null
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
    }
}
