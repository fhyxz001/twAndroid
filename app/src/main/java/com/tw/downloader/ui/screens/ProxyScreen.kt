package com.tw.downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tw.downloader.data.model.ProxyConfig
import com.tw.downloader.data.model.ProxyScheme
import com.tw.downloader.data.repository.MediaRepository
import com.tw.downloader.ui.theme.*

@Composable
fun ProxyScreen(
    repo: MediaRepository,
    onBack: () -> Unit,
    onEditScheme: (ProxyScheme?) -> Unit,
) {
    var config by remember { mutableStateOf(repo.getProxyConfig()) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    fun save(newConfig: ProxyConfig) {
        config = newConfig
        repo.saveProxyConfig(newConfig)
        repo.refreshApi()
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除", color = TextPrimary) },
            text = { Text("确定要删除该代理方案吗？", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    val id = showDeleteDialog!!
                    val newSchemes = config.schemes.filter { it.id != id }
                    val newSelected = if (config.selectedId == id) {
                        newSchemes.firstOrNull()?.id ?: ""
                    } else config.selectedId
                    save(config.copy(schemes = newSchemes, selectedId = newSelected))
                    showDeleteDialog = null
                }) { Text("删除", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消", color = Accent) }
            },
            containerColor = Surface,
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
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
                Text("代理设置", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.width(48.dp))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(0.5.dp, Border, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("全局代理", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { enabled ->
                            save(config.copy(enabled = enabled))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Accent, checkedTrackColor = Accent.copy(alpha = 0.3f)),
                    )
                }
                Text("开启后将使用选中的代理方案", fontSize = 12.sp, color = TextSecondary)
            }

            Spacer(Modifier.height(12.dp))

            // Schemes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(0.5.dp, Border, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("代理方案", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Accent.copy(alpha = 0.12f))
                            .clickable { onEditScheme(null) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text("+ 添加", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Accent)
                    }
                }

                HorizontalDivider(color = Border, thickness = 0.5.dp)

                if (config.schemes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("暂无代理方案", fontSize = 15.sp, color = TextSecondary)
                        Text("点击右上角「添加」创建代理方案", fontSize = 12.sp, color = TextTertiary)
                    }
                } else {
                    config.schemes.forEachIndexed { index, scheme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { save(config.copy(selectedId = scheme.id)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Radio
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(
                                        1.5.dp,
                                        if (config.selectedId == scheme.id) Accent else TextTertiary,
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (config.selectedId == scheme.id) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Accent)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(scheme.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("${scheme.host}:${scheme.port}", fontSize = 12.sp, color = TextSecondary)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Accent.copy(alpha = 0.1f))
                                        .clickable { onEditScheme(scheme) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text("编辑", fontSize = 12.sp, color = Accent)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Error.copy(alpha = 0.1f))
                                        .clickable { showDeleteDialog = scheme.id }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text("删除", fontSize = 12.sp, color = Error)
                                }
                            }
                        }

                        if (index < config.schemes.size - 1) {
                            HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
