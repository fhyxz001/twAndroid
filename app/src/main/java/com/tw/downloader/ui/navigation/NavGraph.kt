package com.tw.downloader.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tw.downloader.data.model.DownloadRecord
import com.tw.downloader.data.model.MediaItem
import com.tw.downloader.data.model.ProxyScheme
import com.tw.downloader.ui.screens.*
import com.tw.downloader.viewmodel.WaterfallViewModel

object Routes {
    const val WATERFALL = "waterfall"
    const val FILES = "files"
    const val PLAYER = "player"
    const val PROXY = "proxy"
    const val PROXY_EDIT = "proxy_edit"
}

@Composable
fun TwNavGraph(navController: NavHostController) {
    val vm: WaterfallViewModel = viewModel()

    // Shared state for player navigation
    var playerVideos by remember { mutableStateOf<List<VideoEntry>>(emptyList()) }
    var playerIndex by remember { mutableIntStateOf(0) }
    var editingScheme by remember { mutableStateOf<ProxyScheme?>(null) }

    NavHost(navController = navController, startDestination = Routes.WATERFALL) {
        composable(Routes.WATERFALL) {
            LaunchedEffect(Unit) {
                vm.refreshDownloadedIds()
                vm.refreshProxy()
            }
            WaterfallScreen(
                vm = vm,
                onNavigateToFiles = { navController.navigate(Routes.FILES) },
                onNavigateToProxy = { navController.navigate(Routes.PROXY) },
                onNavigateToPlayer = { items, index ->
                    playerVideos = items.map { VideoEntry(src = it.url, poster = it.thumbnail, description = it.title) }
                    playerIndex = index
                    navController.navigate(Routes.PLAYER)
                },
            )
        }

        composable(Routes.FILES) {
            FilesScreen(
                repo = vm.repo,
                onBack = { navController.popBackStack() },
                onPlay = { files, index ->
                    playerVideos = files.map {
                        VideoEntry(
                            src = it.filePath.ifEmpty { it.url },
                            poster = it.thumbnail,
                            description = it.title,
                        )
                    }
                    playerIndex = index
                    navController.navigate(Routes.PLAYER)
                },
            )
        }

        composable(Routes.PLAYER) {
            PlayerScreen(
                videos = playerVideos,
                initialIndex = playerIndex,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROXY) {
            ProxyScreen(
                repo = vm.repo,
                onBack = { navController.popBackStack() },
                onEditScheme = { scheme ->
                    editingScheme = scheme
                    navController.navigate(Routes.PROXY_EDIT)
                },
            )
        }

        composable(Routes.PROXY_EDIT) {
            ProxyEditScreen(
                scheme = editingScheme,
                onBack = { navController.popBackStack() },
                onSave = { saved ->
                    val config = vm.repo.getProxyConfig()
                    val newSchemes = if (editingScheme != null) {
                        config.schemes.map { if (it.id == saved.id) saved else it }
                    } else {
                        config.schemes + saved
                    }
                    val newSelected = if (config.selectedId.isEmpty() && newSchemes.size == 1) {
                        saved.id
                    } else config.selectedId
                    vm.repo.saveProxyConfig(config.copy(schemes = newSchemes, selectedId = newSelected))
                    vm.repo.refreshApi()
                    navController.popBackStack()
                },
            )
        }
    }
}
