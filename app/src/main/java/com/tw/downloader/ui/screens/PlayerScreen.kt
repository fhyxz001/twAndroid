package com.tw.downloader.ui.screens

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.tw.downloader.ui.theme.Accent

data class VideoEntry(
    val src: String,
    val poster: String = "",
    val description: String = "",
)

@Composable
fun PlayerScreen(
    videos: List<VideoEntry>,
    initialIndex: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(currentIndex) {
        if (videos.isNotEmpty()) {
            val entry = videos[currentIndex]
            player.setMediaItem(MediaItem.fromUri(entry.src))
            player.prepare()
            player.play()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 6.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // Right controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Prev
            ControlButton(
                icon = { Icon(Icons.Filled.KeyboardArrowUp, "上一个", tint = Accent, modifier = Modifier.size(32.dp)) },
                enabled = currentIndex > 0,
                onClick = { if (currentIndex > 0) currentIndex-- },
            )
            // Next
            ControlButton(
                icon = { Icon(Icons.Filled.KeyboardArrowDown, "下一个", tint = Accent, modifier = Modifier.size(32.dp)) },
                enabled = currentIndex < videos.size - 1,
                onClick = { if (currentIndex < videos.size - 1) currentIndex++ },
            )
            // Landscape
            ControlButton(
                icon = { Icon(Icons.Filled.ScreenRotation, "横屏", tint = Accent, modifier = Modifier.size(24.dp)) },
                enabled = true,
                onClick = {
                    val activity = context as? android.app.Activity
                    activity?.requestedOrientation =
                        if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                },
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (enabled) 0.4f else 0.2f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
