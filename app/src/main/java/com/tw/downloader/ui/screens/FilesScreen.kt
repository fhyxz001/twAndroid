package com.tw.downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tw.downloader.data.model.DownloadRecord
import com.tw.downloader.data.repository.MediaRepository
import com.tw.downloader.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilesScreen(
    repo: MediaRepository,
    onBack: () -> Unit,
    onPlay: (List<DownloadRecord>, Int) -> Unit,
) {
    var files by remember { mutableStateOf(repo.getDownloadRecords()) }
    var editMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(emptySet<Int>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val isAllSelected = files.isNotEmpty() && selectedIndices.size == files.size

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", color = TextPrimary) },
            text = { Text("确定删除选中的 ${deleteTarget.size} 个文件？", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget.sortedDescending().forEach { i ->
                        val file = files.getOrNull(i)
                        if (file != null && file.filePath.isNotEmpty()) {
                            File(file.filePath).delete()
                        }
                    }
                    repo.deleteDownloadRecords(deleteTarget)
                    files = repo.getDownloadRecords()
                    selectedIndices = emptySet()
                    editMode = false
                    showDeleteDialog = false
                }) { Text("删除", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消", color = Accent) }
            },
            containerColor = Surface,
        )
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (editMode) {
                Surface(color = Surface.copy(alpha = 0.95f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (isAllSelected) "取消全选" else "全选",
                            fontSize = 14.sp, color = Accent, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                selectedIndices = if (isAllSelected) emptySet()
                                else files.indices.toSet()
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        Text("已选 ${selectedIndices.size} 项", fontSize = 13.sp, color = TextSecondary)
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedIndices.isEmpty()) Error.copy(alpha = 0.4f) else Error)
                                .clickable(enabled = selectedIndices.isNotEmpty()) {
                                    deleteTarget = selectedIndices
                                    showDeleteDialog = true
                                }
                                .padding(horizontal = 18.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("删除", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Nav bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹ 返回", fontSize = 15.sp, color = Accent,
                    modifier = Modifier.clickable(onClick = onBack),
                )
                Text("本地文件", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    if (editMode) "完成" else "编辑",
                    fontSize = 15.sp, color = Accent, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        editMode = !editMode
                        if (!editMode) selectedIndices = emptySet()
                    },
                )
            }

            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("暂无下载文件", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("去探索页面下载视频吧", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(files, key = { i, f -> "${f.id}-$i" }) { index, file ->
                        FileItem(
                            file = file,
                            editMode = editMode,
                            isSelected = index in selectedIndices,
                            onTap = {
                                if (editMode) {
                                    selectedIndices = if (index in selectedIndices) selectedIndices - index
                                    else selectedIndices + index
                                } else {
                                    onPlay(files, index)
                                }
                            },
                            onDelete = {
                                deleteTarget = setOf(index)
                                showDeleteDialog = true
                            },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    file: DownloadRecord,
    editMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (editMode && isSelected) Accent.copy(alpha = 0.08f) else Surface)
            .clickable(onClick = onTap)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (editMode) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Accent else TextTertiary.copy(alpha = 0.3f))
                    .then(if (!isSelected) Modifier else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Background)
                }
            }
        }

        // Thumbnail
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(45.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (file.thumbnail.isNotEmpty()) {
                AsyncImage(
                    model = file.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(16.dp),
            )
        }

        // Info
        Column(Modifier.weight(1f)) {
            Text(
                file.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (file.downloadedAt > 0) {
                Text(
                    dateFormat.format(Date(file.downloadedAt)),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (!editMode) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Error, modifier = Modifier.size(18.dp))
            }
        }
    }
}
