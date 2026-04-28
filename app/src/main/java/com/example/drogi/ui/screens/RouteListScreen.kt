package com.example.drogi.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.drogi.RouteType
import com.example.drogi.RouteViewModel
import kotlinx.coroutines.launch
import com.example.drogi.ui.components.ThemeToggleIcon
import com.example.drogi.ui.components.FloatingTimer

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