package com.example.drogi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import java.util.Locale
import kotlinx.coroutines.flow.Flow

enum class RouteType { RUNNING, CYCLING }

// do sortowań
enum class ResultSortType { TIME, DATE }
enum class ResultSortOrder { ASC, DESC }

data class Route(
    val id: String,
    val name: String,
    val description: String,
    val type: RouteType,
    val imageUrl: String? = null)

class RouteViewModel(private val dao: RouteResultDao) : ViewModel() {
    private val _allRoutes = MutableStateFlow<List<Route>>(emptyList())
    val allRoutes: StateFlow<List<Route>> = _allRoutes.asStateFlow()

    // ---- motywy ----
    private val _isDarkTheme = MutableStateFlow(false) // Domyślnie jasny
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()


    // ---- ładowanie API ----
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    // ---- ulubione trasy ----
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    // ---- stoper ----
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _activeTimerRouteId = MutableStateFlow<String?>(null)
    val activeTimerRouteId: StateFlow<String?> = _activeTimerRouteId.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    // ---- wyszukiwanie ----
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ---- sortowanie wyników na danej trasie ----
    private val _sortType = MutableStateFlow(ResultSortType.TIME)
    val sortType = _sortType.asStateFlow()

    private val _sortOrder = MutableStateFlow(ResultSortOrder.ASC)
    val sortOrder = _sortOrder.asStateFlow()

    // ---- ekran wyników ----
    private val _isShowingResults = MutableStateFlow(false)
    val isShowingResults: StateFlow<Boolean> = _isShowingResults.asStateFlow()

    // ---- funkcje ----
    init {
        loadRoutes()

        viewModelScope.launch {
            dao.getAllFavoriteIds().collect { ids ->
                _favoriteIds.value = ids.toSet()
            }
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _isLoading.value = true
            _isError.value = false
            // z obslugą błędu API
            try {
                val downloadedRoutes = RetrofitClient.apiService.getRoutes()
                _allRoutes.value = downloadedRoutes
                _isError.value = false
            } catch (e: Exception) {
                e.printStackTrace()
                _isError.value = true
                _allRoutes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRouteById(id: String?): Route? {
        return _allRoutes.value.find { it.id == id }
    }

    // dla tabletu
    private val _selectedRouteId = MutableStateFlow<String?>(null)
    val selectedRouteId: StateFlow<String?> = _selectedRouteId.asStateFlow()

    fun selectRouteForDetail(routeId: String?) {
        _selectedRouteId.value = routeId
        _isShowingResults.value = false
    }

    // ---- stoper ----
    fun startTimer(routeId: String) {
        if (_activeTimerRouteId.value == null) {
            _activeTimerRouteId.value = routeId
        }

        if (_activeTimerRouteId.value == routeId && !_isTimerRunning.value) {
            _isTimerRunning.value = true
            timerJob = viewModelScope.launch {
                while (_isTimerRunning.value) {
                    delay(1000)
                    _elapsedSeconds.value++

                    // Limit 99 godzin
                    if (_elapsedSeconds.value >= 356400) {
                        stopTimer()
                        break
                    }
                }
            }
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        stopTimer()
        _elapsedSeconds.value = 0
        _activeTimerRouteId.value = null
    }

    // ---- formatowanie czasu ----
    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    // ---- baza danych z czasami ----
    fun saveCurrentResult(routeId: String) {
        val currentTime = _elapsedSeconds.value
        if (currentTime <= 0) return

        viewModelScope.launch {
            val newResult = RouteResultEntity(
                routeId = routeId,
                timeInSeconds = currentTime,
                timestamp = System.currentTimeMillis()
            )
            dao.insertResult(newResult) // zapis do bazy
        }

        resetTimer()
    }

    fun getTopResultsForRoute(routeId: String): Flow<List<RouteResultEntity>> {
        return dao.getTop3ForRoute(routeId)
    }

    fun getAllResultsForRoute(routeId: String): Flow<List<RouteResultEntity>> {
        return dao.getAllForRoute(routeId)
    }

    fun deleteResult(result: RouteResultEntity) {
        viewModelScope.launch {
            dao.deleteResult(result)
        }
    }

    // ---- zmiana motywu aplikacji ----
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // ---- baza danych z ulubionymi ----
    fun toggleFavorite(routeId: String) {
        viewModelScope.launch {
            if (_favoriteIds.value.contains(routeId)) {
                dao.removeFavorite(FavoriteEntity(routeId))
            } else {
                dao.addFavorite(FavoriteEntity(routeId))
            }
        }
    }

    // ---- wyszukiwanie ----
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // ---- sortowanie wyników na danej trasie ----
    fun toggleSort(type: ResultSortType) {
        if (_sortType.value == type) {
            // zmiana kierunku sortowania
            _sortOrder.value = if (_sortOrder.value == ResultSortOrder.ASC) ResultSortOrder.DESC else ResultSortOrder.ASC
        } else {
            // sortowanie (domyślnie rosnąco) według nowego parametru
            _sortType.value = type
            _sortOrder.value = ResultSortOrder.ASC
        }
    }

    // ---- ekran wyników ----
    fun setShowResults(show: Boolean) {
        _isShowingResults.value = show
    }

    fun getAverageTime(results: List<RouteResultEntity>): Long {
        if (results.isEmpty()) return 0L
        return results.map { it.timeInSeconds }.average().toLong()
    }
}

class RouteViewModelFactory(private val dao: RouteResultDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}