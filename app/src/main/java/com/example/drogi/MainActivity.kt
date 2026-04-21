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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable

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
                    // inicjalizacaj bazy
                    val db = AppDatabase.getDatabase(applicationContext)

                    val viewModel: RouteViewModel = viewModel(
                        factory = RouteViewModelFactory(db.routeResultDao())
                    )
                    AdaptiveAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AdaptiveAppScreen(viewModel: RouteViewModel) {
    val hasStarted = rememberSaveable { mutableStateOf(false) }

    if (!hasStarted.value) {
        HomeScreen(onStartClick = { hasStarted.value = true })
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

// dla telefonow
@Composable
fun PhoneNavigation(viewModel: RouteViewModel, onBackToHome: () -> Unit) {
    val navController = rememberNavController()
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()

    // widok tablet -> widok telefon z wybrana trasa
    LaunchedEffect(Unit) {
        if (selectedRouteId != null) {
            navController.navigate("routeDetail/$selectedRouteId") {
                popUpTo("routeList")
            }
        }
    }

    // zapisanie stanu
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        val currentRoute = navBackStackEntry?.destination?.route
        if (currentRoute == "routeList") {
            // powrot na liste tras
            viewModel.selectRouteForDetail(null)
        } else if (currentRoute?.startsWith("routeDetail") == true) {
            // wybranie trasy
            val routeId = navBackStackEntry?.arguments?.getString("routeId")
            if (routeId != null) {
                viewModel.selectRouteForDetail(routeId)
            }
        }
    }

    NavHost(navController = navController, startDestination = "routeList") {
        composable("routeList") {
            RouteListScreen(
                viewModel = viewModel,
                onRouteClick = { routeId ->
                    navController.navigate("routeDetail/$routeId")
                },
                onActiveTimerClick = { activeId ->
                    navController.navigate("routeDetail/$activeId")
                },
                onBackToHome = onBackToHome
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
fun TabletSplitScreen(viewModel: RouteViewModel, onBackToHome: () -> Unit) {
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
                },
                onBackToHome = onBackToHome
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

// ekran glowny
@Composable
fun HomeScreen(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ikona aplikacji TODO: zmienić ikone
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ikona_beztla),
            contentDescription = "Logo aplikacji",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // tytul
        Text(
            text = "Drogi & Trasy",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // opis
        Text(
            text = "Znajdź idealną trasę dla siebie. Monitoruj swoje czasy, bij rekordy i odkrywaj nowe ścieżki biegowe oraz rowerowe w okolicy Poznania.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Ruszajmy!",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

// ekran z lista
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    viewModel: RouteViewModel,
    onRouteClick: (String) -> Unit,
    onActiveTimerClick: (String) -> Unit,
    onBackToHome: () -> Unit
) {
    val routes by viewModel.filteredRoutes.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val activeTimerId by viewModel.activeTimerRouteId.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Dostępne trasy") },
                    navigationIcon = {
                            TextButton(onClick = onBackToHome) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Powrót")
                            }
                    })

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
    val showAllResults = remember { mutableStateOf(false) }
    val showResetConfirmation = remember { mutableStateOf(false) }

    BoxWithConstraints {
        val isShortScreen = this.maxHeight < 500.dp

        if (showAllResults.value && route != null) {
            AllResultsDialog(
                routeId = route.id,
                routeName = route.name,
                viewModel = viewModel,
                onDismiss = { showAllResults.value = false }
            )
        }

        if (showResetConfirmation.value) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation.value = false },
                title = { Text("Resetowanie stopera") },
                text = { Text("Czy na pewno chcesz przerwać i zresetować stoper? Niezapisany czas zostanie utracony.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetTimer()
                        showResetConfirmation.value = false
                    }) {
                        Text("Zresetuj", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmation.value = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(route?.name ?: "Wybierz trasę z listy")
                        }
                    },
                    navigationIcon = {
                        if (onBackClick != null) {
                            TextButton(onClick = onBackClick) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Powrót")
                            }
                        }
                    },
                    actions = {
                        if (isShortScreen && route != null) {
                            IconButton(onClick = { showAllResults.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Pokaż wyniki",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (route != null) {
                    if (activeTimerId == null || activeTimerId == route.id) {
                        Column {
                            if (!isShortScreen) {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    BestResultsTable(
                                        routeId = route.id,
                                        viewModel = viewModel,
                                        onTableClick = { showAllResults.value = true }
                                    )
                                }
                            }

                            StopwatchBar(
                                elapsedTime = viewModel.formatTime(elapsedSeconds),
                                isRunning = isRunning,
                                hasTime = elapsedSeconds > 0,
                                onStart = { viewModel.startTimer(route.id) },
                                onStop = { viewModel.stopTimer() },
                                onReset = { showResetConfirmation.value = true },
                                onSave = { viewModel.saveCurrentResult(route.id) }
                            )
                        }
                    } else {
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
                                    text = "Działa stoper trasy: ${viewModel.getRouteById(activeTimerId)?.name}.\nKliknij, aby wrócić.",
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
                    .verticalScroll(rememberScrollState())
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
                    if (!isShortScreen && !route.imageUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        AsyncImage(
                            model = route.imageUrl,
                            contentDescription = "Zdjęcie przedstawiające trasę: ${route.name}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (requestedRouteId != null) {
                    Text("Wystąpił błąd podczas ładowania danych.")
                }
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
                        Button(onClick = onSave) {
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
fun BestResultsTable(
    routeId: String,
    viewModel: RouteViewModel,
    onTableClick: () -> Unit // Akcja kliknięcia
) {
    // CollectAsState z Flow automatycznie odświeży UI po zapisie!
    val results by viewModel.getTopResultsForRoute(routeId).collectAsState(initial = emptyList())

    if (results.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onTableClick() }, // Tabela jest klikalna
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Twoje Najlepsze Wyniki (Kliknij po więcej)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Nagłówki z trzema kolumnami
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Czas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(text = "Data", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text(text = "Godzina", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            results.forEach { result ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = viewModel.formatTime(result.timeInSeconds), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    // Rozdzielamy datę i godzinę
                    Text(text = formatTimestamp(result.timestamp, "dd.MM.yyyy"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Text(text = formatTimestamp(result.timestamp, "HH:mm"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun AllResultsDialog(
    routeId: String,
    routeName: String,
    viewModel: RouteViewModel,
    onDismiss: () -> Unit
) {
    val allResults by viewModel.getAllResultsForRoute(routeId).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wszystkie czasy: $routeName") },
        text = {
            LazyColumn {
                items(allResults) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.formatTime(result.timeInSeconds),
                            modifier = Modifier.weight(1f),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = formatTimestamp(result.timestamp, "dd.MM.yyyy HH:mm"),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        // przycisk usuwania
                        IconButton(onClick = { viewModel.deleteResult(result) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Usuń wynik",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}

fun formatTimestamp(timestamp: Long, pattern: String): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}