package com.tw.downloader.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tw.downloader.R
import com.tw.downloader.data.model.*
import com.tw.downloader.ui.components.SettingsSheet
import com.tw.downloader.ui.theme.*
import com.tw.downloader.viewmodel.WaterfallViewModel

@Composable
fun WaterfallScreen(
    vm: WaterfallViewModel,
    onNavigateToProxy: () -> Unit,
    onNavigateToPlayer: (List<MediaItem>, Int) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState.canScrollForward) {
        if (!gridState.canScrollForward && vm.items.isNotEmpty() && !vm.loadingMore) {
            vm.loadMore()
        }
    }

    if (vm.showSettings) {
        SettingsSheet(
            onDismiss = { vm.updateShowSettings(false) },
        )
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (vm.selectMode) {
                SelectionToolbar(
                    selectedCount = vm.selectedIds.size,
                    downloading = vm.downloading,
                    isAllSelected = vm.items.isNotEmpty() && vm.items.all { it.id in vm.selectedIds },
                    onCancel = { vm.toggleSelectMode() },
                    onSelectAll = { vm.toggleSelectAll() },
                    onDownload = { vm.downloadSelected() },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Nav bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("探索", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Accent)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavButton(
                        icon = {
                            Icon(
                                if (vm.selectMode) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = "选择",
                                tint = if (vm.selectMode) Accent else TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = { vm.toggleSelectMode() },
                    )
                    NavButton(
                        icon = { Icon(painterResource(R.drawable.clash), "代理", tint = TextSecondary, modifier = Modifier.size(22.dp)) },
                        onClick = onNavigateToProxy,
                    )
                    NavButton(
                        icon = { Icon(painterResource(R.drawable.set), "设置", tint = TextSecondary, modifier = Modifier.size(22.dp)) },
                        onClick = { vm.updateShowSettings(true) },
                    )
                    NavButton(
                        icon = { Icon(painterResource(R.drawable.refresh), "刷新", tint = TextSecondary, modifier = Modifier.size(22.dp)) },
                        onClick = { vm.loadData() },
                    )
                }
            }

            // Category tabs removed
            // Loading indicator for refresh
            if (vm.loading && vm.items.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(bottom = 12.dp),
                    color = Accent,
                    trackColor = Accent.copy(alpha = 0.1f),
                )
            }

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    vm.loading && vm.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("加载中...", fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    }
                    vm.loadError.isNotEmpty() && vm.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(vm.loadError, color = Error, fontSize = 14.sp)
                        }
                    }
                    vm.items.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📹", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("暂无内容", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("下拉刷新重试", fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    }
                    else -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp,
                        ) {
                            itemsIndexed(vm.items, key = { _, item -> item.id }) { index, item ->
                                MediaCard(
                                    item = item,
                                    selectMode = vm.selectMode,
                                    isSelected = item.id in vm.selectedIds,
                                    isDownloaded = item.id in vm.downloadedIds,
                                    onClick = {
                                        if (vm.selectMode) {
                                            vm.toggleSelect(item.id)
                                        } else {
                                            onNavigateToPlayer(vm.items, index)
                                        }
                                    },
                                )
                            }

                            if (vm.loadingMore) {
                                item {
                                    Column(
                                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                                            color = Accent,
                                            trackColor = Accent.copy(alpha = 0.1f),
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text("加载更多...", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            } else if (!vm.hasNext && vm.items.isNotEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text("-- 已经到底了 --", fontSize = 12.sp, color = TextTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(icon: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Surface)
            .border(0.5.dp, Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    selectMode: Boolean,
    isSelected: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        if (selectMode && isSelected) Accent else Border,
        label = "cardBorder",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(
            width = if (selectMode && isSelected) 2.dp else 0.5.dp,
            color = borderColor,
        ),
    ) {
        // Poster
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(SurfaceVariant)
        ) {
            if (item.thumbnail.isNotEmpty()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", fontSize = 32.sp, color = Color.White.copy(alpha = 0.25f))
                }
            }

            // Downloaded badge
            if (isDownloaded) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "已下载",
                    tint = Accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp),
                )
            }

            // Select indicator
            if (selectMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Accent else Color.Black.copy(alpha = 0.3f))
                        .border(1.5.dp, if (isSelected) Accent else Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Background)
                    }
                }
            }

            // Play badge
            if (item.url.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // File size badge
            if (item.fileSize > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        formatFileSize(item.fileSize),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                    )
                }
            }
        }

        // Stats
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (item.pv > 0) {
                Text("热度 ${formatCount(item.pv)}", fontSize = 11.sp, color = TextSecondary)
            }
            if (item.favorite > 0) {
                Text("♥ ${formatCount(item.favorite)}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SelectionToolbar(
    selectedCount: Int,
    downloading: Boolean,
    isAllSelected: Boolean,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onDownload: () -> Unit,
) {
    Surface(
        color = Surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "取消",
                fontSize = 14.sp,
                color = Accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onCancel),
            )
            Spacer(Modifier.width(16.dp))
            Box(Modifier.width(0.5.dp).height(16.dp).background(Border))
            Spacer(Modifier.width(16.dp))
            Text(
                if (isAllSelected) "取消全选" else "全选",
                fontSize = 14.sp,
                color = Accent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onSelectAll),
            )
            Spacer(Modifier.weight(1f))
            Text("已选 $selectedCount 项", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedCount == 0 && !downloading) TextTertiary.copy(alpha = 0.6f) else Accent)
                    .clickable(enabled = selectedCount > 0 || downloading, onClick = onDownload)
                    .padding(horizontal = 18.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (downloading) "停止" else "下载",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Background,
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}

private fun formatCount(num: Long): String {
    if (num <= 0) return ""
    if (num >= 10000) return "${"%.1f".format(num / 10000.0)}w"
    if (num >= 1000) return "${"%.1f".format(num / 1000.0)}k"
    return num.toString()
}
