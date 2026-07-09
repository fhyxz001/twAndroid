package com.tw.downloader.data.model

import kotlinx.serialization.Serializable

data class MediaItem(
    val id: String,
    val url: String,
    val thumbnail: String,
    val title: String = "",
    val duration: Long = 0,
    val favorite: Long = 0,
    val pv: Long = 0,
    val fileSize: Long = -1L, // -1 means unknown
    val tweetUrl: String = "",
    val tweetAccount: String = "",
)

@Serializable
data class DownloadRecord(
    val id: String,
    val title: String,
    val thumbnail: String = "",
    val url: String,
    val filePath: String = "",
    val downloadedAt: Long = 0,
)

@Serializable
data class ProxyScheme(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
)

@Serializable
data class ProxyConfig(
    val enabled: Boolean = false,
    val schemes: List<ProxyScheme> = emptyList(),
    val selectedId: String = "",
)
