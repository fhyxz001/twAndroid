package com.tw.downloader.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaResponse(
    val items: List<MediaItemRaw> = emptyList(),
)

@Serializable
data class MediaItemRaw(
    val id: Long = 0,
    val url_cd: String = "",
    val url: String = "",
    val thumbnail: String = "",
    val title: String = "",
    val name: String = "",
    val duration: Long = 0,
    val time: Long = 0,
    val favorite: String = "0",
    val favorites: String = "0",
    val pv: String = "0",
    val views: String = "0",
)

data class MediaItem(
    val id: String,
    val url: String,
    val thumbnail: String,
    val title: String,
    val duration: Long,
    val favorite: Long,
    val pv: Long,
    val fileSize: Long = -1L, // -1 means unknown
)

fun MediaItemRaw.toMediaItem(): MediaItem? {
    val idStr = id.toString().trim()
    val videoUrl = url.trim()
    if (idStr.isEmpty() || videoUrl.isEmpty()) return null
    if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) return null
    val thumb = thumbnail.trim()
    val pvLong = pv.toLongOrNull() ?: 0L
    val viewsLong = views.toLongOrNull() ?: 0L
    val favLong = favorite.toLongOrNull() ?: 0L
    val favsLong = favorites.toLongOrNull() ?: 0L
    return MediaItem(
        id = idStr,
        url = videoUrl,
        thumbnail = if (thumb.startsWith("http")) thumb else "",
        title = title.ifBlank { name.ifBlank { idStr } },
        duration = if (duration > 0) duration else time,
        favorite = if (favLong > 0) favLong else favsLong,
        pv = if (pvLong > 0) pvLong else viewsLong,
    )
}

data class Tag(val code: String, val name: String)

val ALL_TAGS = listOf(
    Tag("kyonyu", "巨乳"), Tag("creampie", "中出"), Tag("uncensored", "无码"),
    Tag("lolita", "萝莉"), Tag("married-woman", "人妻"), Tag("beautiful-girl", "美少女"),
    Tag("masturbation", "自慰"), Tag("shaved", "无毛"), Tag("anal", "后门"),
    Tag("facial", "颜射"), Tag("small-breasts", "贫乳"), Tag("jk", "女高中生"),
    Tag("female-pervert", "痴女"), Tag("gal", "辣妹"), Tag("cum-swallowing", "吞精"),
    Tag("fellatio", "口交"), Tag("handjob", "手交"), Tag("titjob", "乳交"),
    Tag("deep-throat", "深喉"), Tag("bukkake", "颜面"), Tag("shirouto", "素人"),
    Tag("incest", "乱伦"), Tag("rape", "强奸"), Tag("molestation", "痴汉"),
    Tag("orgy", "乱交"), Tag("outdoor", "户外"), Tag("voyeur", "偷拍"),
    Tag("pickup", "搭讪"), Tag("cosplay", "cosplay"), Tag("anime", "二次元"),
    Tag("sm", "SM"), Tag("hamedori", "自拍"), Tag("personal-filming", "私人"),
    Tag("female-teacher", "女教师"), Tag("nurse", "护士"), Tag("big-sister", "姐姐"),
    Tag("swimsuit", "泳装"), Tag("special-feature", "企划"), Tag("massage", "按摩"),
    Tag("gay", "gay/伪娘"), Tag("ja", "日本"), Tag("zh-CN", "中国"),
    Tag("th", "泰国"), Tag("en", "英语"), Tag("zh-TW", "繁体"),
    Tag("ko", "韩语"), Tag("id", "印尼"), Tag("pt", "葡萄牙"),
    Tag("fr", "法语"), Tag("de", "德语"),
)

data class SortOption(val value: String, val label: String)
val SORT_OPTIONS = listOf(
    SortOption("created", "最近添加"),
    SortOption("time", "按时长"),
    SortOption("favorite", "按点赞"),
    SortOption("pv", "按观看数"),
)

data class RangeOption(val value: String, val label: String)
val RANGE_OPTIONS = listOf(
    RangeOption("daily", "每日"),
    RangeOption("weekly", "每周"),
    RangeOption("monthly", "每月"),
    RangeOption("all", "全部"),
)

data class TimeFilterOption(val label: String, val min: Long, val max: Long)
val TIME_FILTER_OPTIONS = listOf(
    TimeFilterOption("全部", 0, 86400),
    TimeFilterOption("0-5分钟", 0, 300),
    TimeFilterOption("5-15分钟", 300, 900),
    TimeFilterOption("15-30分钟", 900, 1800),
    TimeFilterOption("30分钟-1小时", 1800, 3600),
    TimeFilterOption("一小时以上", 3600, 86400),
)

val PER_PAGE_OPTIONS = listOf(10, 20, 30, 50, 100)

data class WaterfallConfig(
    val perPage: Int = 10,
    val sort: String = "pv",
    val range: String = "daily",
    val minTime: Long = 0,
    val maxTime: Long = 86400,
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
