package com.solium.Unitoolkit.ui.navigation

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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.solium.Unitoolkit.ui.components.AppIcon
import com.solium.Unitoolkit.ui.home.HomeScreen
import com.solium.Unitoolkit.ui.settings.SettingsScreen
import com.solium.Unitoolkit.ui.toolbox.ToolboxScreen

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
    const val COURSE_PERIODS = "course_periods"
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

/** 底部 tab 的相对序号，非 tab 路由返回 -1。 */
private fun tabIndex(route: String?): Int = when (route) {
    Routes.HOME -> 0
    Routes.TOOLBOX -> 1
    Routes.SETTINGS -> 2
    else -> -1
}

private fun isTopLevelTab(route: String?): Boolean = tabIndex(route) >= 0

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
                // 底部栏 tab 之间：最原始的左右平移（两页同速平行移动），方向为
                // 向右切（to > from）内容整体向左平移，向左切内容整体向右平移。
                if (isTopLevelTab(initialState.destination.route) && isTopLevelTab(targetState.destination.route)) {
                    val forward = tabIndex(targetState.destination.route) > tabIndex(initialState.destination.route)
                    slideInHorizontally(animationSpec = tween(280)) { it -> if (forward) it else -it }
                } else {
                    // 非顶级 tab（push 进入子页面）：新页面从右侧进入（向左推进）
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(280))
                }
            },
            exitTransition = {
                if (isTopLevelTab(initialState.destination.route) && isTopLevelTab(targetState.destination.route)) {
                    val forward = tabIndex(targetState.destination.route) > tabIndex(initialState.destination.route)
                    // 与进入页同速向同方向平移，构成纯平移效果
                    slideOutHorizontally(animationSpec = tween(280)) { it -> if (forward) -it else it }
                } else {
                    // push 时旧页面向左侧退出，与新页从右进入方向一致（统一向左推进）
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(280))
                }
            },
            popEnterTransition = {
                // 返回下层也是底部 tab（如从设置 / 工具箱点底下「概览」）时，与 tab 平移方向保持一致：
                // AInitial = 当前顶层 tab，targetState = 下层 tab；targetToRight 表示下层在右侧（应向左平移）还是左侧（向右平移）。
                if (isTopLevelTab(initialState.destination.route) && isTopLevelTab(targetState.destination.route)) {
                    val targetToRight = tabIndex(targetState.destination.route) > tabIndex(initialState.destination.route)
                    slideInHorizontally(animationSpec = tween(280)) { it -> if (targetToRight) it else -it }
                } else {
                    // 普通返回（pop）：下层页面从左侧进入，与当前页向右退出方向一致（统一向右拉回）
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(280))
                }
            },
            popExitTransition = {
                if (isTopLevelTab(initialState.destination.route) && isTopLevelTab(targetState.destination.route)) {
                    val targetToRight = tabIndex(targetState.destination.route) > tabIndex(initialState.destination.route)
                    // 与进入页同速向同方向平移，构成纯平移效果
                    slideOutHorizontally(animationSpec = tween(280)) { it -> if (targetToRight) -it else it }
                } else {
                    // 普通返回（pop）：当前页面向右侧退出（向右拉回）
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(280))
                }
            },
        ) {
            composable(Routes.HOME) { TopLevelContent { HomeScreen(navController) } }
            composable(Routes.TOOLBOX) { TopLevelContent { ToolboxScreen(navController) } }
            composable(Routes.SETTINGS) { TopLevelContent { SettingsScreen(navController) } }

            composable(Routes.COURSE) { com.solium.Unitoolkit.ui.course.CourseScreen(navController) }
            composable(
                Routes.COURSE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { com.solium.Unitoolkit.ui.course.CourseEditScreen(navController) }

            composable(Routes.NOTE_LIST) { com.solium.Unitoolkit.ui.note.NoteListScreen(navController) }
            composable(
                Routes.NOTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { com.solium.Unitoolkit.ui.note.NoteEditScreen(navController) }

            composable(Routes.TODO) { com.solium.Unitoolkit.ui.todo.TodoScreen(navController) }
            composable(Routes.POMODORO) { com.solium.Unitoolkit.ui.pomodoro.PomodoroScreen(navController) }
            composable(Routes.POMODORO_IMMERSIVE) { com.solium.Unitoolkit.ui.pomodoro.PomodoroImmersiveScreen(onBack = { navController.popBackStack() }) }

            composable(Routes.UTILITY) { com.solium.Unitoolkit.ui.life.UtilityScreen(navController) }
            composable(Routes.SCHEDULE) { com.solium.Unitoolkit.ui.life.ScheduleScreen(navController) }
            composable(Routes.GPA) { com.solium.Unitoolkit.ui.life.GpaScreen(navController) }
            composable(Routes.LINKS) { com.solium.Unitoolkit.ui.life.LinksScreen(navController) }
            composable(Routes.LEDGER) { com.solium.Unitoolkit.ui.life.LedgerScreen(navController) }

            composable(
                Routes.WEBVIEW,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { com.solium.Unitoolkit.ui.life.WebViewScreen(navController) }

            composable(Routes.OVERLAY) { com.solium.Unitoolkit.ui.overlay.OverlayScreen(navController) }

            composable(Routes.QUICK_ENTRIES) { com.solium.Unitoolkit.ui.settings.QuickEntriesScreen(navController) }
            composable(Routes.CHANGELOG) { com.solium.Unitoolkit.ui.settings.ChangelogScreen(navController) }
            composable(Routes.PROFILE_CARD) { com.solium.Unitoolkit.ui.profile.ProfileCardScreen(navController) }
            composable(Routes.PROFILE_EDIT) { com.solium.Unitoolkit.ui.profile.ProfileCardEditScreen(navController) }
            composable(Routes.COURSE_PRESETS) { com.solium.Unitoolkit.ui.course.CoursePresetScreen(navController) }
            composable(Routes.COURSE_PERIODS) { com.solium.Unitoolkit.ui.course.CoursePeriodScreen(navController) }
            composable(Routes.THEMES) { com.solium.Unitoolkit.ui.settings.ThemeManagerScreen(navController) }

            composable(Routes.SHOPPING) { com.solium.Unitoolkit.ui.shopping.ShoppingScreen(navController) }
            composable(
                Routes.SHOPPING_ACTIVITY,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { com.solium.Unitoolkit.ui.shopping.ShoppingActivityScreen(navController) }
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
