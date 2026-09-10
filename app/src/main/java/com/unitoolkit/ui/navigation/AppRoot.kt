package com.unitoolkit.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.unitoolkit.ui.components.AppIcon
import com.unitoolkit.ui.home.HomeScreen
import com.unitoolkit.ui.settings.SettingsScreen
import com.unitoolkit.ui.toolbox.ToolboxScreen

object Routes {
    const val HOME = "home"
    const val TOOLBOX = "toolbox"
    const val SETTINGS = "settings"

    const val COURSE = "course"
    const val COURSE_EDIT = "course_edit/{id}"
    const val NOTE_LIST = "note_list"
    const val NOTE_EDIT = "note_edit/{id}"
    const val TODO = "todo"
    const val POMODORO = "pomodoro"
    const val POMODORO_IMMERSIVE = "pomodoro_immersive"
    const val UTILITY = "utility"
    const val SCHEDULE = "schedule"
    const val GPA = "gpa"
    const val LINKS = "links"
    const val LEDGER = "ledger"
    const val WEBVIEW = "webview/{title}/{url}"
    const val OVERLAY = "overlay"
    const val QUICK_ENTRIES = "quick_entries"
    const val SHOPPING = "shopping"
    const val SHOPPING_ACTIVITY = "shopping_activity/{id}"
    const val CHANGELOG = "changelog"
    const val PROFILE_CARD = "profile_card"
    const val PROFILE_EDIT = "profile_edit"
    const val COURSE_PRESETS = "course_presets"
    const val THEMES = "themes"

    fun courseEdit(id: Long) = "course_edit/$id"
    fun noteEdit(id: Long) = "note_edit/$id"
    fun webview(title: String, url: String) = "webview/${java.net.URLEncoder.encode(title, "UTF-8")}/${java.net.URLEncoder.encode(url, "UTF-8")}"
    fun shoppingActivity(id: Long) = "shopping_activity/$id"
}

private data class TopLevel(val route: String, val icon: String, val label: String)

private val topLevels = listOf(
    TopLevel(Routes.HOME, "house", "概览"),
    TopLevel(Routes.TOOLBOX, "layout-grid", "工具箱"),
    TopLevel(Routes.SETTINGS, "settings", "设置"),
)

@Composable
fun AppRoot(
    navRequest: String? = null,
    onNavRequestHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    val isTopLevel = topLevels.any { tl ->
        currentDestination?.hierarchy?.any { it.route == tl.route } == true
    }

    LaunchedEffect(navRequest) {
        val route = navRequest ?: return@LaunchedEffect
        navController.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
        onNavRequestHandled()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    topLevels.forEach { tl ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == tl.route } == true,
                            onClick = {
                                navController.navigate(tl.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { AppIcon(tl.icon, size = 24.dp) },
                            label = { Text(tl.label, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(280),
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(280),
                )
            },
        ) {
            composable(Routes.HOME) { TopLevelContent { HomeScreen(navController) } }
            composable(Routes.TOOLBOX) { TopLevelContent { ToolboxScreen(navController) } }
            composable(Routes.SETTINGS) { TopLevelContent { SettingsScreen(navController) } }

            composable(Routes.COURSE) { com.unitoolkit.ui.course.CourseScreen(navController) }
            composable(
                Routes.COURSE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { com.unitoolkit.ui.course.CourseEditScreen(navController) }

            composable(Routes.NOTE_LIST) { com.unitoolkit.ui.note.NoteListScreen(navController) }
            composable(
                Routes.NOTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { com.unitoolkit.ui.note.NoteEditScreen(navController) }

            composable(Routes.TODO) { com.unitoolkit.ui.todo.TodoScreen(navController) }
            composable(Routes.POMODORO) { com.unitoolkit.ui.pomodoro.PomodoroScreen(navController) }
            composable(Routes.POMODORO_IMMERSIVE) { com.unitoolkit.ui.pomodoro.PomodoroImmersiveScreen(onBack = { navController.popBackStack() }) }

            composable(Routes.UTILITY) { com.unitoolkit.ui.life.UtilityScreen(navController) }
            composable(Routes.SCHEDULE) { com.unitoolkit.ui.life.ScheduleScreen(navController) }
            composable(Routes.GPA) { com.unitoolkit.ui.life.GpaScreen(navController) }
            composable(Routes.LINKS) { com.unitoolkit.ui.life.LinksScreen(navController) }
            composable(Routes.LEDGER) { com.unitoolkit.ui.life.LedgerScreen(navController) }

            composable(
                Routes.WEBVIEW,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { com.unitoolkit.ui.life.WebViewScreen(navController) }

            composable(Routes.OVERLAY) { com.unitoolkit.ui.overlay.OverlayScreen(navController) }

            composable(Routes.QUICK_ENTRIES) { com.unitoolkit.ui.settings.QuickEntriesScreen(navController) }
            composable(Routes.CHANGELOG) { com.unitoolkit.ui.settings.ChangelogScreen(navController) }
            composable(Routes.PROFILE_CARD) { com.unitoolkit.ui.profile.ProfileCardScreen(navController) }
            composable(Routes.PROFILE_EDIT) { com.unitoolkit.ui.profile.ProfileCardEditScreen(navController) }
            composable(Routes.COURSE_PRESETS) { com.unitoolkit.ui.course.CoursePresetScreen(navController) }
            composable(Routes.THEMES) { com.unitoolkit.ui.settings.ThemeManagerScreen(navController) }

            composable(Routes.SHOPPING) { com.unitoolkit.ui.shopping.ShoppingScreen(navController) }
            composable(
                Routes.SHOPPING_ACTIVITY,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { com.unitoolkit.ui.shopping.ShoppingActivityScreen(navController) }
        }
    }
}

@Composable
private fun TopLevelContent(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        content()
    }
}
