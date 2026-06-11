package com.tw.downloader.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tw.downloader.data.model.MediaItem
import com.tw.downloader.data.model.ProxyScheme
import com.tw.downloader.ui.screens.*
import com.tw.downloader.viewmodel.WaterfallViewModel

object Routes {
    const val WATERFALL = "waterfall"
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
        composable(
            Routes.WATERFALL,
            popEnterTransition = { fadeIn(tween(300)) },
        ) {
            LaunchedEffect(Unit) {
                vm.refreshProxy()
            }
            WaterfallScreen(
                vm = vm,
                onNavigateToProxy = { navController.navigate(Routes.PROXY) },
                onNavigateToPlayer = { items, index ->
                    playerVideos = items.map { VideoEntry(id = it.id, src = it.url, poster = it.thumbnail, description = it.title) }
                    playerIndex = index
                    navController.navigate(Routes.PLAYER)
                },
            )
        }

        composable(
            Routes.PLAYER,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) },
            popEnterTransition = { fadeIn(tween(400)) },
            popExitTransition = { fadeOut(tween(400)) },
        ) {
            PlayerScreen(
                videos = playerVideos,
                initialIndex = playerIndex,
                onBack = { navController.popBackStack() },
                onDownload = { entry -> vm.downloadSingle(entry) },
                downloadingIds = vm.downloadingIds,
                downloadProgressMap = vm.downloadProgressMap,
                downloadedIds = vm.downloadedIds,
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
