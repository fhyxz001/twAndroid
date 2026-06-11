package com.tw.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tw.downloader.data.model.*
import com.tw.downloader.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    config: WaterfallConfig,
    onDismiss: () -> Unit,
    onSave: (WaterfallConfig) -> Unit,
) {
    var perPage by remember { mutableIntStateOf(config.perPage) }
    var sortIndex by remember { mutableIntStateOf(SORT_OPTIONS.indexOfFirst { it.value == config.sort }.coerceAtLeast(0)) }
    var rangeIndex by remember { mutableIntStateOf(RANGE_OPTIONS.indexOfFirst { it.value == config.range }.coerceAtLeast(0)) }
    var timeFilterIndex by remember { mutableIntStateOf(TIME_FILTER_OPTIONS.indexOfFirst { it.min == config.minTime && it.max == config.maxTime }.coerceAtLeast(0)) }

    var showPerPageMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showRangeMenu by remember { mutableStateOf(false) }
    var showTimeMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var cacheSize by remember { mutableStateOf(calcCacheSize(context.cacheDir)) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除缓存", color = TextPrimary) },
            text = { Text("确定清除所有缓存数据？", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    context.cacheDir.deleteRecursively()
                    context.cacheDir.mkdirs()
                    cacheSize = "0 B"
                    showClearDialog = false
                }) { Text("清除", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消", color = Accent) }
            },
            containerColor = Surface,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextTertiary)
                )
                Spacer(Modifier.height(16.dp))
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("设置", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
            ) {
                // Per page
                SettingRow(label = "每页数量", value = perPage.toString(), showDivider = true) {
                    showPerPageMenu = true
                }
                DropdownMenu(expanded = showPerPageMenu, onDismissRequest = { showPerPageMenu = false }) {
                    PER_PAGE_OPTIONS.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt.toString()) }, onClick = {
                            perPage = opt; showPerPageMenu = false
                        })
                    }
                }

                // Sort
                SettingRow(label = "排序方式", value = SORT_OPTIONS[sortIndex].label, showDivider = true) {
                    showSortMenu = true
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SORT_OPTIONS.forEachIndexed { i, opt ->
                        DropdownMenuItem(text = { Text(opt.label) }, onClick = {
                            sortIndex = i; showSortMenu = false
                        })
                    }
                }

                // Range
                SettingRow(label = "时间范围", value = RANGE_OPTIONS[rangeIndex].label, showDivider = true) {
                    showRangeMenu = true
                }
                DropdownMenu(expanded = showRangeMenu, onDismissRequest = { showRangeMenu = false }) {
                    RANGE_OPTIONS.forEachIndexed { i, opt ->
                        DropdownMenuItem(text = { Text(opt.label) }, onClick = {
                            rangeIndex = i; showRangeMenu = false
                        })
                    }
                }

                // Time filter
                SettingRow(label = "时长筛选", value = TIME_FILTER_OPTIONS[timeFilterIndex].label, showDivider = false) {
                    showTimeMenu = true
                }
                DropdownMenu(expanded = showTimeMenu, onDismissRequest = { showTimeMenu = false }) {
                    TIME_FILTER_OPTIONS.forEachIndexed { i, opt ->
                        DropdownMenuItem(text = { Text(opt.label) }, onClick = {
                            timeFilterIndex = i; showTimeMenu = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Clear cache
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("清除缓存", fontSize = 15.sp, color = TextPrimary)
                    Text(cacheSize, fontSize = 15.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent)
                    .clickable {
                        val sort = SORT_OPTIONS[sortIndex]
                        val range = RANGE_OPTIONS[rangeIndex]
                        val tf = TIME_FILTER_OPTIONS[timeFilterIndex]
                        onSave(
                            WaterfallConfig(
                                perPage = perPage,
                                sort = sort.value,
                                range = range.value,
                                minTime = tf.min,
                                maxTime = tf.max,
                            )
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("完成", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Background)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 15.sp, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 15.sp, color = TextSecondary)
                Spacer(Modifier.width(4.dp))
                Text("›", fontSize = 18.sp, color = TextTertiary)
            }
        }
        if (showDivider) {
            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

private fun calcCacheSize(dir: File): String {
    val bytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}
