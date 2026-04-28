package com.example.drogi.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.drogi.RouteViewModel
import com.example.drogi.ui.screens.*

@Composable
fun AdaptiveAppScreen(viewModel: RouteViewModel) {
    val showSplash = rememberSaveable { mutableStateOf(true) }
    val hasStarted = rememberSaveable { mutableStateOf(false) }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    if (showSplash.value) {
        SplashScreen(onTimeout = { showSplash.value = false })
    } else if (!hasStarted.value) {
        HomeScreen(
            isDarkTheme = isDarkTheme,
            onToggleTheme = { viewModel.toggleTheme() },
            onStartClick = { hasStarted.value = true }
        )
    } else {
        BoxWithConstraints {
            if (this.maxWidth < 600.dp) {
                PhoneNavigation(viewModel, onBackToHome = { hasStarted.value = false })
            } else {
                TabletSplitScreen(viewModel, onBackToHome = { hasStarted.value = false })
            }
        }
    }
}

@Composable
fun PhoneNavigation(viewModel: RouteViewModel, onBackToHome: () -> Unit) {
    val navController = rememberNavController()
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()
    val isShowingResults by viewModel.isShowingResults.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(selectedRouteId, isShowingResults) {
        if (selectedRouteId != null) {
            val destination = if (isShowingResults) {
                Screen.RouteResults.createRoute(selectedRouteId!!)
            } else {
                Screen.RouteDetail.createRoute(selectedRouteId!!)
            }
            navController.navigate(destination) {
                popUpTo(Screen.RouteList.route)
            }
        }
    }

    LaunchedEffect(navBackStackEntry) {
        val currentRoute = navBackStackEntry?.destination?.route
        viewModel.setShowResults(currentRoute?.startsWith("routeResults") == true)

        when {
            currentRoute == Screen.RouteList.route -> viewModel.selectRouteForDetail(null)
            currentRoute?.startsWith("routeDetail") == true -> {
                val routeId = navBackStackEntry?.arguments?.getString("routeId")
                if (routeId != null) viewModel.selectRouteForDetail(routeId)
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.RouteList.route) {
        composable(Screen.RouteList.route) {
            RouteListScreen(
                viewModel = viewModel,
                onRouteClick = { routeId -> navController.navigate(Screen.RouteDetail.createRoute(routeId)) },
                onActiveTimerClick = { activeId -> navController.navigate(Screen.RouteDetail.createRoute(activeId)) },
                onBackToHome = onBackToHome
            )
        }

        composable(
            route = Screen.RouteDetail.route,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")
            RouteDetailScreen(
                route = viewModel.getRouteById(routeId),
                requestedRouteId = routeId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToActiveRoute = { activeId ->
                    navController.navigate(Screen.RouteDetail.createRoute(activeId)) { popUpTo(Screen.RouteList.route) }
                },
                onNavigateToResults = { id -> navController.navigate(Screen.RouteResults.createRoute(id)) }
            )
        }

        composable(
            route = Screen.RouteResults.route,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")
            RouteResultsScreen(
                route = viewModel.getRouteById(routeId),
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun TabletSplitScreen(viewModel: RouteViewModel, onBackToHome: () -> Unit) {
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()
    val isShowingResults by viewModel.isShowingResults.collectAsState()
    val selectedRoute = viewModel.getRouteById(selectedRouteId)

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            RouteListScreen(
                viewModel = viewModel,
                onRouteClick = { viewModel.selectRouteForDetail(it) },
                onActiveTimerClick = { viewModel.selectRouteForDetail(it) },
                onBackToHome = onBackToHome,
                showThemeToggle = false
            )
        }
        VerticalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxHeight()
        )
        Box(modifier = Modifier.weight(1.5f)) {
            if (isShowingResults && selectedRouteId != null) {
                RouteResultsScreen(
                    route = selectedRoute,
                    viewModel = viewModel,
                    onBackClick = { viewModel.setShowResults(false) }
                )
            } else {
                RouteDetailScreen(
                    route = selectedRoute,
                    requestedRouteId = selectedRouteId,
                    viewModel = viewModel,
                    onBackClick = null,
                    onNavigateToActiveRoute = { viewModel.selectRouteForDetail(it) },
                    onNavigateToResults = { viewModel.setShowResults(true) }
                )
            }
        }
    }
}