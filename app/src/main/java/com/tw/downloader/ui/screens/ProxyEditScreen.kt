package com.tw.downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tw.downloader.data.model.ProxyScheme
import com.tw.downloader.ui.theme.*

@Composable
fun ProxyEditScreen(
    scheme: ProxyScheme?,
    onBack: () -> Unit,
    onSave: (ProxyScheme) -> Unit,
) {
    var name by remember { mutableStateOf(scheme?.name ?: "") }
    var host by remember { mutableStateOf(scheme?.host ?: "") }
    var port by remember { mutableStateOf(scheme?.port?.toString() ?: "") }

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
                Text(
                    if (scheme != null) "编辑方案" else "添加方案",
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                )
                Spacer(Modifier.width(48.dp))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(0.5.dp, Border, RoundedCornerShape(12.dp)),
            ) {
                FormRow(label = "方案名称", value = name, placeholder = "如：工作代理", onValueChange = { name = it })
                HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                FormRow(label = "服务器地址", value = host, placeholder = "如：127.0.0.1", onValueChange = { host = it })
                HorizontalDivider(color = Border, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                FormRow(
                    label = "端口",
                    value = port,
                    placeholder = "如：7890",
                    onValueChange = { port = it },
                    keyboardType = KeyboardType.Number,
                )
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent)
                    .clickable {
                        val trimName = name.trim()
                        val trimHost = host.trim()
                        val trimPort = port.trim().toIntOrNull()

                        if (trimName.isEmpty() || trimHost.isEmpty() || trimPort == null || trimPort !in 1..65535) {
                            return@clickable
                        }

                        onSave(
                            ProxyScheme(
                                id = scheme?.id ?: "proxy_${System.currentTimeMillis()}",
                                name = trimName,
                                host = trimHost,
                                port = trimPort,
                            )
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("保存", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Background)
            }
        }
    }
}

@Composable
private fun FormRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.width(90.dp),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = TextTertiary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedIndicatorColor = Background,
                unfocusedIndicatorColor = Background,
                cursorColor = Accent,
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}
