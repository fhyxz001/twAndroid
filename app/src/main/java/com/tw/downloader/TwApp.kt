package com.tw.downloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.tw.downloader.data.model.ProxyConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

class TwApp : Application(), ImageLoaderFactory {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_DOWNLOAD,
            "下载",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "下载进度通知"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun newImageLoader(): ImageLoader {
        val appContext = this

        val proxySelector = object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> {
                val prefs = appContext.getSharedPreferences("tw_downloader", MODE_PRIVATE)
                val raw = prefs.getString("proxy_config", null) ?: return listOf(Proxy.NO_PROXY)
                val config = try {
                    json.decodeFromString<ProxyConfig>(raw)
                } catch (_: Exception) {
                    return listOf(Proxy.NO_PROXY)
                }
                if (!config.enabled || config.selectedId.isEmpty()) return listOf(Proxy.NO_PROXY)
                val scheme = config.schemes.find { it.id == config.selectedId }
                    ?: return listOf(Proxy.NO_PROXY)
                return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress(scheme.host, scheme.port)))
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
        }

        val okHttpClient = OkHttpClient.Builder()
            .proxySelector(proxySelector)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    companion object {
        const val CHANNEL_DOWNLOAD = "downloads"
        const val NOTIFICATION_DOWNLOAD_BATCH = 1001
        const val NOTIFICATION_DOWNLOAD_SINGLE = 1002
    }
}
