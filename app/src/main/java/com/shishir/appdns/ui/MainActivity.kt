package com.shishir.appdns.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shishir.appdns.ui.components.Pressable
import com.shishir.appdns.ui.screens.AppsScreen
import com.shishir.appdns.ui.screens.HomeScreen
import com.shishir.appdns.ui.screens.ProfilesScreen
import com.shishir.appdns.ui.screens.SettingsScreen
import com.shishir.appdns.ui.screens.SetupScreen
import com.shishir.appdns.ui.theme.AppDNSTheme
import com.shishir.appdns.ui.theme.LocalHapticsOn

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_SETUP = "open_setup"
    }

    private var setupSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra(EXTRA_OPEN_SETUP, false) == true) setupSignal++
        setContent {
            val vm: AppViewModel = viewModel()
            val snap by vm.store.snapshot.collectAsStateWithLifecycle()
            AppDNSTheme(snap.prefs.theme) {
                CompositionLocalProvider(LocalHapticsOn provides snap.prefs.haptics) {
                    AppRoot(vm, setupSignal)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_SETUP, false)) setupSignal++
    }
}

private object Routes {
    const val HOME = "home"
    const val APPS = "apps"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val SETUP = "setup"
}

@Composable
private fun AppRoot(vm: AppViewModel, setupSignal: Int) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.HOME
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        vm.refreshStatus()
    }

    LaunchedEffect(setupSignal) {
        if (setupSignal > 0) {
            nav.navigate(Routes.SETUP) { launchSingleTop = true }
        }
    }

    val enterSpring = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (route != Routes.SETUP) {
                MonoBottomBar(route) { target ->
                    nav.navigate(target) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = {
                slideInHorizontally(enterSpring) { it / 6 } + fadeIn(tween(240))
            },
            exitTransition = {
                slideOutHorizontally(enterSpring) { -it / 8 } + fadeOut(tween(180))
            },
            popEnterTransition = {
                slideInHorizontally(enterSpring) { -it / 6 } + fadeIn(tween(240))
            },
            popExitTransition = {
                slideOutHorizontally(enterSpring) { it / 8 } + fadeOut(tween(180))
            }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onSetup = { nav.navigate(Routes.SETUP) },
                    onApps = { nav.navigate(Routes.APPS) },
                    onProfiles = { nav.navigate(Routes.PROFILES) }
                )
            }
            composable(Routes.APPS) { AppsScreen(vm, snackbar) }
            composable(Routes.PROFILES) { ProfilesScreen(vm, snackbar) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    snackbar = snackbar,
                    onSetup = { nav.navigate(Routes.SETUP) },
                    onProfiles = { nav.navigate(Routes.PROFILES) }
                )
            }
            composable(Routes.SETUP) {
                SetupScreen(vm = vm, onDone = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun MonoBottomBar(current: String, onSelect: (String) -> Unit) {
    val tabs = listOf(
        Tab(Routes.HOME, "Home", Icons.Rounded.Home),
        Tab(Routes.APPS, "Apps", Icons.Rounded.Apps),
        Tab(Routes.PROFILES, "DNS", Icons.Rounded.Dns),
        Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            val bg by androidx.compose.animation.animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.background,
                tween(240), label = "tabBg"
            )
            val fg by androidx.compose.animation.animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(240), label = "tabFg"
            )
            Pressable(
                onClick = { onSelect(tab.route) },
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
            ) {
                Row(
                    Modifier.padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(tab.icon, tab.label, Modifier.size(20.dp), tint = fg)
                    if (selected) {
                        Text(
                            tab.label,
                            color = fg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}