package com.example.drogi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.drogi.ui.components.BestResultsTable
import com.example.drogi.Route
import com.example.drogi.RouteViewModel
import com.example.drogi.ui.components.ThemeToggleIcon
import com.example.drogi.ui.components.StopwatchBar

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
    val hasTriggeredBack = remember { mutableStateOf(false) } // BYŁO VAR ZMIENIĆ JAK NIE BĘDXIE DZIAŁĄC
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