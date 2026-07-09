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
import com.tw.downloader.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    checkingUpdate: Boolean = false,
    updateError: String = "",
    updateDownloading: Boolean = false,
    updateProgress: Int = 0,
    showUpdateDialog: Boolean = false,
    latestVersion: String = "",
    latestApkUrl: String = "",
    onDismissUpdateDialog: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {},
) {
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

    // Update dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = onDismissUpdateDialog,
            title = {
                Text(
                    if (latestApkUrl.isNotEmpty()) "发现新版本" else "检查更新",
                    color = TextPrimary,
                )
            },
            text = {
                when {
                    checkingUpdate -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Accent,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("正在检查更新...", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                    updateError.isNotEmpty() && latestApkUrl.isEmpty() -> {
                        Text(updateError, fontSize = 14.sp, color = Error)
                    }
                    updateDownloading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("正在下载 $latestVersion...", fontSize = 14.sp, color = TextSecondary)
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { updateProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Accent,
                                trackColor = Accent.copy(alpha = 0.15f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("$updateProgress%", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                    else -> {
                        Column {
                            Text("新版本: $latestVersion", fontSize = 14.sp, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("是否下载更新？", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                if (latestApkUrl.isNotEmpty() && !updateDownloading) {
                    TextButton(onClick = onDownloadUpdate) {
                        Text("下载", color = Accent)
                    }
                }
            },
            dismissButton = {
                if (!updateDownloading) {
                    TextButton(onClick = onDismissUpdateDialog) {
                        Text(if (updateError.isNotEmpty()) "关闭" else "取消", color = TextSecondary)
                    }
                }
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
                // Check update
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !checkingUpdate) { onCheckUpdate() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("检查更新", fontSize = 15.sp, color = TextPrimary)
                    if (checkingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Accent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            if (updateError.isNotEmpty()) "检查失败" else "",
                            fontSize = 13.sp,
                            color = TextTertiary,
                        )
                    }
                }

                // Divider
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Border))

                // Clear cache
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

            Spacer(Modifier.height(32.dp))
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
