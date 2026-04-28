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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Replay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // baza danych
            val db = AppDatabase.getDatabase(applicationContext)
            val viewModel: RouteViewModel = viewModel(
                factory = RouteViewModelFactory(db.routeResultDao())
            )
            // motyw
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            DrogiTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdaptiveAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AdaptiveAppScreen(viewModel: RouteViewModel) {
    // ekran ładowania
    val showSplash = rememberSaveable { mutableStateOf(true) }
    // ekran powitalny
    val hasStarted = rememberSaveable { mutableStateOf(false) }

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // logika przełączania ekranó
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

// dla telefonow
@Composable
fun PhoneNavigation(viewModel: RouteViewModel, onBackToHome: () -> Unit) {
    val navController = rememberNavController()
    val selectedRouteId by viewModel.selectedRouteId.collectAsState()
    val isShowingResults by viewModel.isShowingResults.collectAsState()

    LaunchedEffect(Unit) {
        if (selectedRouteId != null) {
            val destination = if (isShowingResults) "routeResults/$selectedRouteId" else "routeDetail/$selectedRouteId"
            navController.navigate(destination) {
                popUpTo("routeList")
            }
        }
    }

    // zapisanie stanu
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        val currentRoute = navBackStackEntry?.destination?.route

        // czy pokazywane wyniki
        viewModel.setShowResults(currentRoute?.startsWith("routeResults") == true)

        if (currentRoute == "routeList") {
            viewModel.selectRouteForDetail(null)
        } else if (currentRoute?.startsWith("routeDetail") == true) {
            val routeId = navBackStackEntry?.arguments?.getString("routeId")
            if (routeId != null) viewModel.selectRouteForDetail(routeId)
        }
    }

    NavHost(navController = navController, startDestination = "routeList") {
        // lista tras
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

        // ekran szczegółów trasy
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
                },
                onNavigateToResults = { id ->
                    navController.navigate("routeResults/$id")
                }
            )
        }

        // ekrna wyników na danej trasie
        composable(
            route = "routeResults/{routeId}",
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")
            val route = viewModel.getRouteById(routeId)

            RouteResultsScreen(
                route = route,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
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
    val isShowingResults by viewModel.isShowingResults.collectAsState()

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
                onBackToHome = onBackToHome,
                showThemeToggle = false
            )
        }

        // podział ekranu
        VerticalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxHeight()
        )

        // prawa strona
        Box(modifier = Modifier.weight(1.5f)) {
            if (isShowingResults && selectedRouteId != null) {
                RouteResultsScreen(
                    route = selectedRoute,
                    viewModel = viewModel,
                    onBackClick = { viewModel.setShowResults(false) } // powrót do ekranu szczegółów
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

// ekran glowny
@Composable
fun HomeScreen(isDarkTheme: Boolean, onToggleTheme: () -> Unit, onStartClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            ThemeToggleIcon(isDarkTheme = isDarkTheme, onToggle = onToggleTheme)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
}

// ekran z lista
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    viewModel: RouteViewModel,
    onRouteClick: (String) -> Unit,
    onActiveTimerClick: (String) -> Unit,
    onBackToHome: () -> Unit,
    showThemeToggle: Boolean = true
) {
    val allRoutes by viewModel.allRoutes.collectAsState()
    val activeTimerId by viewModel.activeTimerRouteId.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val isError by viewModel.isError.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current
    val isSearchFocused = remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus() // schowanie klawiatury
            })
        },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Dostępne trasy") },
                    navigationIcon = {
                        TextButton(onClick = onBackToHome) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Powrót")
                        }
                    },
                    actions = {
                        if (showThemeToggle) {
                            val isDark by viewModel.isDarkTheme.collectAsState()
                            ThemeToggleIcon(isDarkTheme = isDark, onToggle = { viewModel.toggleTheme() })
                        }
                    }
                )

                // wyszukiwanie trasy
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .onFocusChanged { isSearchFocused.value = it.isFocused },
                    placeholder = { Text("Szukaj trasy...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Szukaj")
                    },
                    trailingIcon = {
                        // Jeśli wpisano tekst, pokazujemy krzyżyk do szybkiego czyszczenia
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Wyczyść")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        text = { Text("Biegowe") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        text = { Text("Rowerowe") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = { Text("Ulubione") }
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
        // sprawdzanie stanu pobrania danych z API
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                // kręcące się kółko ładowania
                CircularProgressIndicator()
            } else if (isError && allRoutes.isEmpty()) {
                // błąd i brak tras do pokazania
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Błąd pobrania tras.\nSprawdź połączenie z internetem.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadRoutes() }) {
                        Text("Spróbuj ponownie")
                    }
                }
            } else {
                // aby móc przesówać kolumny przeciągnięciem
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val tabFilteredRoutes = when (page) {
                        0 -> allRoutes.filter { it.type == RouteType.RUNNING }
                        1 -> allRoutes.filter { it.type == RouteType.CYCLING }
                        else -> allRoutes.filter { favoriteIds.contains(it.id) }
                    }

                    val pageRoutes = if (searchQuery.isBlank()) {
                        tabFilteredRoutes
                    } else {
                        tabFilteredRoutes.filter { route ->
                            // ignoreCase aby wielkość liter nie miała znaczenia
                            route.name.contains(searchQuery, ignoreCase = true) ||
                                    route.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (page == 2 && pageRoutes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nie masz jeszcze ulubionych tras",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(pageRoutes) { route ->
                                val interactionSource = remember { MutableInteractionSource() }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .height(100.dp)
                                        .clickable (
                                            interactionSource = interactionSource,
                                            // włączenie/wyłączenie animacji kliknięcia na karte
                                            indication = if (isSearchFocused.value) null else LocalIndication.current
                                        ) {
                                            if (isSearchFocused.value) {
                                                focusManager.clearFocus()
                                            } else {
                                                onRouteClick(route.id)
                                            }
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {

                                        // zdjęcie w tle
                                        if (!route.imageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = route.imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                alpha = 0.5f
                                            )
                                        }

                                        // tekst
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = route.name,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                // cień pod tekstem
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    onNavigateToActiveRoute: (String) -> Unit,
    onNavigateToResults: (String) -> Unit
) {
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val activeTimerId by viewModel.activeTimerRouteId.collectAsState()
    val showResetConfirmation = remember { mutableStateOf(false) }
    val hasTriggeredBack = remember { mutableStateOf(false) }
    val swipeBackModifier = if (onBackClick != null) {
        Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = { hasTriggeredBack.value = false },
                onDragCancel = { hasTriggeredBack.value = false }
            ) { change, dragAmount ->
                if (dragAmount > 20 && !hasTriggeredBack.value) {
                    hasTriggeredBack.value = true
                    onBackClick.invoke()
                }
            }
        }
    } else {
        Modifier
    }


    BoxWithConstraints(modifier = swipeBackModifier) {
        val isShortScreen = this.maxHeight < 500.dp

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
                        val isDark by viewModel.isDarkTheme.collectAsState()
                        val favorites by viewModel.favoriteIds.collectAsState()
                        val isFavorite = favorites.contains(route?.id)

                        // gwiazdka do dodwania do ulubionych
                        if (route != null) {
                            IconButton(onClick = { viewModel.toggleFavorite(route.id) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Ulubione",
                                    tint = Color(0xFFFFD700)
                                )
                            }
                        }

                        ThemeToggleIcon(isDarkTheme = isDark, onToggle = { viewModel.toggleTheme() })

                        // ikona z tabelą wynikow
                        if (isShortScreen && route != null) {
                            IconButton(onClick = { onNavigateToResults(route.id) }) {
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
                                        onTableClick = { onNavigateToResults(route.id) }
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
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Zapisz"
                            )
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
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Resetuj stoper"
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

fun formatTimestamp(timestamp: Long, pattern: String): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// zmiana motywu
@Composable
fun ThemeToggleIcon(isDarkTheme: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        if (isDarkTheme) {
            Icon(
                imageVector = Icons.Default.LightMode,
                contentDescription = "Jasny motyw",
                tint = Color(0xFFFFD700)
            )
        } else {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = "Ciemny motyw",
                tint = Color(0xFF1A237E)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteResultsScreen(
    route: Route?,
    viewModel: RouteViewModel,
    onBackClick: () -> Unit
) {
    val results by viewModel.getAllResultsForRoute(route?.id ?: "").collectAsState(initial = emptyList())
    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showDeleteConfirm = remember { mutableStateOf<RouteResultEntity?>(null) }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // powrót do ekrany szczegółów poprzez przeciągnięcie
    val hasTriggeredBack = remember { mutableStateOf(false) }
    val swipeBackModifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = { hasTriggeredBack.value = false },
            onDragCancel = { hasTriggeredBack.value = false }
        ) { change, dragAmount ->
            if (dragAmount > 20 && !hasTriggeredBack.value) {
                hasTriggeredBack.value = true
                onBackClick()
            }
        }
    }

    // logika sortowania
    val sortedResults = remember(results, sortType, sortOrder) {
        when (sortType) {
            ResultSortType.TIME -> if (sortOrder == ResultSortOrder.ASC) results.sortedBy { it.timeInSeconds } else results.sortedByDescending { it.timeInSeconds }
            ResultSortType.DATE -> if (sortOrder == ResultSortOrder.ASC) results.sortedBy { it.timestamp } else results.sortedByDescending { it.timestamp }
        }
    }

    // potwierdzenie usunięcia wyniku
    if (showDeleteConfirm.value != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = null },
            title = { Text("Usunąć wynik?") },
            text = { Text("Czy na pewno chcesz trwale usunąć ten czas? Tej operacji nie można cofnąć.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteResult(showDeleteConfirm.value!!)
                    showDeleteConfirm.value = null
                }) { Text("Usuń", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = null }) { Text("Anuluj") }
            }
        )
    }

    Box(modifier = swipeBackModifier)
    {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Wyniki: ${route?.name}") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Powrót")
                        }
                    },

                    actions = {
                        ThemeToggleIcon(
                            isDarkTheme = isDarkTheme,
                            onToggle = { viewModel.toggleTheme() }
                        )
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // statystyki
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Najlepszy czas",
                        value = if (results.isNotEmpty()) viewModel.formatTime(results.minOf { it.timeInSeconds }) else "--",
                        Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Średni czas",
                        value = if (results.isNotEmpty()) viewModel.formatTime(viewModel.getAverageTime(results)) else "--",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Ilość",
                        value = results.size.toString(),
                        Modifier.weight(1f)
                    )
                }

                // nagłówek z sortowaniem
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 50.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Czas do lewej strony
                        SortHeaderItem(
                            label = "Czas",
                            type = ResultSortType.TIME,
                            currentType = sortType,
                            order = sortOrder,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start
                        ) { viewModel.toggleSort(it) }

                        // Data wyśrodkowana
                        SortHeaderItem(
                            label = "Data",
                            type = ResultSortType.DATE,
                            currentType = sortType,
                            order = sortOrder,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center
                        ) { viewModel.toggleSort(it) }

                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // dane
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedResults) { result ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    viewModel.formatTime(result.timeInSeconds),
                                    Modifier.weight(1f).padding(start = 20.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    formatTimestamp(result.timestamp, "dd.MM.yyyy HH:mm"),
                                    Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { showDeleteConfirm.value = result }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Usuń",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SortHeaderItem(
    label: String,
    type: ResultSortType,
    currentType: ResultSortType,
    order: ResultSortOrder,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onClick: (ResultSortType) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onClick(type) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)

        Icon(
            imageVector = if (currentType == type && order == ResultSortOrder.DESC)
                Icons.Default.ArrowDropDown
            else
                Icons.Default.ArrowDropUp,
            contentDescription = null,
            // przeźroczysta ikona
            tint = if (currentType == type) MaterialTheme.colorScheme.primary else Color.Transparent
        )
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // animowane 2 właściwości
    val offsetX = remember { Animatable(-300f) } // rower poza ekranem
    val alpha = remember { Animatable(0f) }      // przeźroczysty tekst

    // uruchomienie animacji
    LaunchedEffect(Unit) {
        launch {
            // wjazd roweru
            offsetX.animateTo(
                targetValue = 0f, // zatrzymanie na środku
                animationSpec = tween(durationMillis = 1200)
            )
        }
        launch {
            // pojawianie się tekstu
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500)
            )
        }

        delay(1800)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // rower
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = "Rower",
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = offsetX.value.dp), // podpięcie animowanej właściwości
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // tekst
            Text(
                text = "Drogi & Trasy",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(alpha.value) // podpięcie animowanej właściwości
            )
        }
    }
}