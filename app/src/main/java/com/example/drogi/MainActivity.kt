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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.drogi.ui.theme.DrogiTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop

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
                    AdaptiveAppScreen()
                }
            }
        }
    }
}

@Composable
fun AdaptiveAppScreen(viewModel: RouteViewModel = viewModel()) {
    // sprawdzenie szerokosci ekranu
    BoxWithConstraints {
        if (maxWidth < 600.dp) {
            // telefon
            PhoneNavigation(viewModel)
        } else {
            // tablet
            TabletSplitScreen(viewModel)
        }
    }
}

// dla telefonow
@Composable
fun PhoneNavigation(viewModel: RouteViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "routeList") {
        composable("routeList") {
            RouteListScreen(
                viewModel = viewModel,
                onRouteClick = { routeId ->
                    navController.navigate("routeDetail/$routeId")
                },
                onActiveTimerClick = { activeId ->
                    // przejscie do trasy ze stoperem
                    navController.navigate("routeDetail/$activeId")
                }
            )
        }
        composable(
            route = "routeDetail/{routeId}",
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")
            val selectedRoute = viewModel.getRouteById(routeId)

            RouteDetailScreen(
                route = selectedRoute,
                requestedRouteId = routeId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToActiveRoute = { activeId ->
                    navController.navigate("routeDetail/$activeId") {
                        popUpTo("routeList")
                    }
                }
            )
        }
    }
}

// dla tabletow
@Composable
fun TabletSplitScreen(viewModel: RouteViewModel) {
    // aktualnie wybrana trasa
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()
    val selectedRoute = viewModel.getRouteById(selectedRouteId)

    Row(modifier = Modifier.fillMaxSize()) {
        // lewa strona z trasami
        Box(modifier = Modifier.weight(1f)) {
            RouteListScreen(
                viewModel = viewModel,
                onRouteClick = { routeId ->
                    viewModel.selectRouteForDetail(routeId)
                },
                onActiveTimerClick = { activeId ->
                    viewModel.selectRouteForDetail(activeId)
                }
            )
        }

        // podzial ekranow
        VerticalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxHeight()
        )

        // prawa strona
        Box(modifier = Modifier.weight(1.5f)) {
            RouteDetailScreen(
                route = selectedRoute,
                requestedRouteId = selectedRouteId,
                viewModel = viewModel,
                onBackClick = null, // ukrycie przycisku powrotu
                onNavigateToActiveRoute = { activeId ->
                    viewModel.selectRouteForDetail(activeId)
                }
            )
        }
    }
}

// pierwszy ekran
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    viewModel: RouteViewModel,
    onRouteClick: (String) -> Unit,
    onActiveTimerClick: (String) -> Unit
) {
    val routes by viewModel.filteredRoutes.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val activeTimerId by viewModel.activeTimerRouteId.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Dostępne trasy") })

                TabRow(selectedTabIndex = if (selectedType == RouteType.RUNNING) 0 else 1) {
                    Tab(
                        selected = selectedType == RouteType.RUNNING,
                        onClick = { viewModel.selectType(RouteType.RUNNING) },
                        text = { Text("Biegowe") }
                    )
                    Tab(
                        selected = selectedType == RouteType.CYCLING,
                        onClick = { viewModel.selectType(RouteType.CYCLING) },
                        text = { Text("Rowerowe") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTimerId != null) {
                val activeRoute = viewModel.getRouteById(activeTimerId)
                FloatingTimer(
                    elapsedTime = viewModel.formatTime(elapsedSeconds),
                    routeName = activeRoute?.name ?: "",
                    onClick = { onActiveTimerClick(activeTimerId!!) }
                )
            }
        }
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
                        .clickable { onRouteClick(route.id) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
fun RouteDetailScreen(
    route: Route?,
    requestedRouteId: String?,
    viewModel: RouteViewModel,
    onBackClick: (() -> Unit)? = null,
    onNavigateToActiveRoute: (String) -> Unit
) {
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val activeTimerId by viewModel.activeTimerRouteId.collectAsState()
    val allSavedResults by viewModel.savedResults.collectAsState()
    val topResults = if (route != null) viewModel.getTopResultsForRoute(route.id) else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route?.name ?: "Wybierz trasę z listy") },
                navigationIcon = {
                    if (onBackClick != null) {
                        TextButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Powrót")
                        }
                    }
                }
            )
        },
        // stoper
        bottomBar = {
            if (route != null) {
                if (activeTimerId == null || activeTimerId == route.id) {
                    Column {
                        // tabela wyników
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BestResultsTable(results = topResults, viewModel = viewModel)
                        }
                        StopwatchBar(
                            elapsedTime = viewModel.formatTime(elapsedSeconds),
                            isRunning = isRunning,
                            hasTime = elapsedSeconds > 0,
                            onStart = { viewModel.startTimer(route.id) },
                            onStop = { viewModel.stopTimer() },
                            onReset = { viewModel.resetTimer() },
                            onSave = { viewModel.saveCurrentResult(route.id) }
                        )
                    }
                } else {
                    val activeRoute = viewModel.getRouteById(activeTimerId)

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToActiveRoute(activeTimerId!!) }
                                .navigationBarsPadding()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Działa stoper trasy: ${activeRoute?.name}.\nKliknij, aby wrócić.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
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
            } else if (requestedRouteId != null) {
                Text("Wystąpił błąd podczas ładowania danych.")
            }
        }
    }
}

// stoper
@Composable
fun StopwatchBar(
    elapsedTime: String,
    isRunning: Boolean,
    hasTime: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = elapsedTime,
                style = MaterialTheme.typography.headlineMedium
            )
            Row {
                if (!isRunning) {
                    if (hasTime) {
                        Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            Text("Zapisz")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(onClick = onStart) {Icon(
                            imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Start"
                    )
                        }
                } else {
                    Button(onClick = onStop) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pauza"
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onReset) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Przerwij"
                    )
                }
            }
        }
    }
}

// pływający stoper
@Composable
fun FloatingTimer(
    elapsedTime: String,
    routeName: String,
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        icon = { },
        text = {
            Column {
                Text(text = elapsedTime, style = MaterialTheme.typography.titleMedium)
                Text(text = routeName, style = MaterialTheme.typography.labelSmall)
            }
        }
    )
}

// tabela z najlepszymi wynikami
@Composable
fun BestResultsTable(results: List<RouteResult>, viewModel: RouteViewModel) {
    if (results.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Twoje Najlepsze Wyniki",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Czas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(text = "Data", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            results.forEach { result ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = viewModel.formatTime(result.timeInSeconds), style = MaterialTheme.typography.bodyMedium)
                    Text(text = result.date, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}