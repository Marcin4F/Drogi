package com.example.drogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.drogi.ui.theme.DrogiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrogiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(routeViewModel: RouteViewModel = viewModel()) {
    val navController = rememberNavController()

    val routes by routeViewModel.routes.collectAsState()

    NavHost(navController = navController, startDestination = "routeList") {
        // pierwszy ekran
        composable("routeList") {
            RouteListScreen(
                routes = routes,
                onRouteClick = { routeId ->
                    navController.navigate("routeDetail/$routeId")
                }
            )
        }
        //drugi ekran
        composable(
            route = "routeDetail/{routeId}",
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")

            val selectedRoute = routeViewModel.getRouteById(routeId)

            RouteDetailScreen(
                route = selectedRoute,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// pierwszy ekran
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(routes: List<Route>, onRouteClick: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Dostępne trasy") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(routes) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onRouteClick(route.id) }
                ) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// drugi ekran
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(route: Route?, onBackClick: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(route?.name ?: "Nie znaleziono trasy") },
            navigationIcon = { TextButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Powrót")}}
        )}
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (route != null) {
                Text(
                    text = "Opis trasy",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    style = MaterialTheme.typography.bodyLarge,
                    text = route.description
                )
            } else {
                Text("Wystąpił błąd podczas ładowania danych.")
            }
        }
    }
}