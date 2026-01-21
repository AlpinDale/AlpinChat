package com.alpin.chat.ui.navigation

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alpin.chat.domain.model.ModelConfig
import com.alpin.chat.ui.screens.chat.ChatScreen
import com.alpin.chat.ui.screens.conversations.ConversationsDrawer
import com.alpin.chat.ui.screens.settings.AddModelScreen
import com.alpin.chat.ui.screens.settings.SettingsScreen
import com.alpin.chat.ui.screens.tokenizer.TokenizerScreen
import kotlinx.coroutines.launch

private const val NAV_ANIMATION_DURATION = 150

sealed class Screen(val route: String) {
    data object Chat : Screen("chat?conversationId={conversationId}") {
        fun createRoute(conversationId: String? = null): String {
            return if (conversationId != null) {
                "chat?conversationId=$conversationId"
            } else {
                "chat"
            }
        }
    }
    data object Settings : Screen("settings")
    data object Tokenizer : Screen("tokenizer")
    data object AddModel : Screen("add_model?modelId={modelId}") {
        fun createRoute(modelId: String? = null): String {
            return if (modelId != null) {
                "add_model?modelId=$modelId"
            } else {
                "add_model"
            }
        }
    }
}

@Composable
fun ChatNavGraph() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingModel by remember { mutableStateOf<ModelConfig?>(null) }

    // Animate blur based on drawer state
    val isDrawerOpen = drawerState.isOpen || drawerState.isAnimationRunning
    val blurRadius by animateDpAsState(
        targetValue = if (isDrawerOpen) 16.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "blurAnimation"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Transparent, // Remove dimming, use blur instead
        drawerContent = {
            ConversationsDrawer(
                selectedConversationId = currentConversationId,
                onConversationSelected = { conversationId ->
                    currentConversationId = conversationId
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                },
                onNewConversation = {
                    currentConversationId = null
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Screen.Chat.createRoute()) {
                        popUpTo(Screen.Chat.route) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Screen.Settings.route)
                }
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        // Apply blur to content when drawer is open (API 31+)
        val contentModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(blurRadius)
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = contentModifier) {
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(NAV_ANIMATION_DURATION)
                    ) + fadeIn(animationSpec = tween(NAV_ANIMATION_DURATION))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(NAV_ANIMATION_DURATION)
                    ) + fadeOut(animationSpec = tween(NAV_ANIMATION_DURATION))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(NAV_ANIMATION_DURATION)
                    ) + fadeIn(animationSpec = tween(NAV_ANIMATION_DURATION))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(NAV_ANIMATION_DURATION)
                    ) + fadeOut(animationSpec = tween(NAV_ANIMATION_DURATION))
                }
            ) {
                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        navArgument("conversationId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId")
                        ?: currentConversationId

                    ChatScreen(
                        onOpenDrawer = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        conversationId = conversationId
                    )
                }

                composable(route = Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToAddModel = { model ->
                            editingModel = model
                            navController.navigate(Screen.AddModel.createRoute(model?.id))
                        },
                        onNavigateToTokenizer = {
                            navController.navigate(Screen.Tokenizer.route)
                        }
                    )
                }

                composable(route = Screen.Tokenizer.route) {
                    TokenizerScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.AddModel.route,
                    arguments = listOf(
                        navArgument("modelId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    AddModelScreen(
                        existingModel = editingModel,
                        onNavigateBack = {
                            editingModel = null
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
