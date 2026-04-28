package com.example.drogi.ui.screens

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.drogi.ResultSortOrder
import com.example.drogi.ResultSortType
import com.example.drogi.Route
import com.example.drogi.RouteResultEntity
import com.example.drogi.RouteViewModel
import com.example.drogi.ui.components.ThemeToggleIcon
import com.example.drogi.ui.components.StatCard
import com.example.drogi.ui.components.SortHeaderItem
import com.example.drogi.utils.formatTimestamp

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