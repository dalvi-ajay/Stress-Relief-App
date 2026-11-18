package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.data.CalmPreferencesRepository
import com.example.ui.BreathingViewModel
import com.example.ui.TasksViewModel
import com.example.ui.UnwindViewModel
import com.example.ui.VoiceAssistantViewModel
import com.example.ui.screens.BreathingScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.UnwindScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.StillnessTheme

const val ROUTE_TASKS = "tasks"
const val ROUTE_BREATHE = "breathe"
const val ROUTE_UNWIND = "unwind"
const val ROUTE_VOICE = "voice"

private enum class StillnessDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    TASKS(
        route = ROUTE_TASKS,
        labelRes = R.string.nav_tasks,
        selectedIcon = Icons.Rounded.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        testTag = "nav_tab_tasks",
    ),
    BREATHE(
        route = ROUTE_BREATHE,
        labelRes = R.string.nav_breathe,
        selectedIcon = Icons.Rounded.Air,
        unselectedIcon = Icons.Outlined.Air,
        testTag = "nav_tab_breathe",
    ),
    UNWIND(
        route = ROUTE_UNWIND,
        labelRes = R.string.nav_unwind,
        selectedIcon = Icons.Rounded.WaterDrop,
        unselectedIcon = Icons.Outlined.WaterDrop,
        testTag = "nav_tab_unwind",
    ),
    VOICE(
        route = ROUTE_VOICE,
        labelRes = R.string.nav_voice,
        selectedIcon = Icons.Rounded.Mic,
        unselectedIcon = Icons.Outlined.Mic,
        testTag = "nav_tab_voice",
    ),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            StillnessTheme {
                StillnessAppRoot()
            }
        }
    }
}

@Composable
fun StillnessAppRoot() {
    val context = LocalContext.current.applicationContext
    val repository = remember { CalmPreferencesRepository(context) }

    val tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory(repository))
    val breathingViewModel: BreathingViewModel = viewModel(factory = BreathingViewModel.Factory(repository))
    val unwindViewModel: UnwindViewModel = viewModel()
    val voiceAssistantViewModel: VoiceAssistantViewModel = viewModel()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_TASKS

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        if (useNavRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
                    ),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    StillnessDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToDestination(navController, dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes),
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.testTag),
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    StillnessNavHost(
                        navController = navController,
                        tasksViewModel = tasksViewModel,
                        breathingViewModel = breathingViewModel,
                        unwindViewModel = unwindViewModel,
                        voiceAssistantViewModel = voiceAssistantViewModel,
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                    ) {
                        StillnessDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToDestination(navController, dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                StillnessNavHost(
                    navController = navController,
                    tasksViewModel = tasksViewModel,
                    breathingViewModel = breathingViewModel,
                    unwindViewModel = unwindViewModel,
                    voiceAssistantViewModel = voiceAssistantViewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun StillnessNavHost(
    navController: NavHostController,
    tasksViewModel: TasksViewModel,
    breathingViewModel: BreathingViewModel,
    unwindViewModel: UnwindViewModel,
    voiceAssistantViewModel: VoiceAssistantViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_TASKS,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(ROUTE_TASKS) {
            TasksScreen(
                viewModel = tasksViewModel,
                onNavigateToBreathe = { pattern ->
                    if (pattern != null) {
                        breathingViewModel.selectPattern(pattern)
                    }
                    navigateToDestination(navController, ROUTE_BREATHE)
                },
            )
        }
        composable(ROUTE_BREATHE) {
            BreathingScreen(viewModel = breathingViewModel)
        }
        composable(ROUTE_UNWIND) {
            UnwindScreen(viewModel = unwindViewModel)
        }
        composable(ROUTE_VOICE) {
            VoiceAssistantScreen(
                viewModel = voiceAssistantViewModel,
                onOpenBreathingPattern = { pattern ->
                    breathingViewModel.selectPattern(pattern)
                    navigateToDestination(navController, ROUTE_BREATHE)
                },
            )
        }
    }
}

private fun navigateToDestination(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
