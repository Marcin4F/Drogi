package com.example.drogi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RouteType { RUNNING, CYCLING }

data class Route(val id: String, val name: String, val description: String, val type: RouteType)

class RouteViewModel : ViewModel() {

    private val _allRoutes = MutableStateFlow<List<Route>>(emptyList())
    private val _selectedType = MutableStateFlow(RouteType.RUNNING)
    val selectedType: StateFlow<RouteType> = _selectedType.asStateFlow()

    private val _filteredRoutes = MutableStateFlow<List<Route>>(emptyList())
    val filteredRoutes: StateFlow<List<Route>> = _filteredRoutes.asStateFlow()

    init {
        loadRoutes()
        updateFilteredList()
    }

    private fun loadRoutes() {
        _allRoutes.value = listOf(
            Route("1", "Rusałka Loop", "Świetna trasa biegowa dookoła jeziora Rusałka w Poznaniu. Dystans ok. 4.5 km. Nawierzchnia szutrowa, brak większych wzniesień.", RouteType.RUNNING),
            Route("2", "Wartostrada", "Asfaltowa trasa rowerowa wzdłuż rzeki Warty. Idealna na szybki trening szosowy z dala od ruchu samochodowego.", RouteType.CYCLING),
            Route("3", "Rezerwat Morasko", "Wymagająca trasa z podbiegami w pobliżu rezerwatu meteorytów. Dużo korzeni i nierówności.", RouteType.RUNNING),
            Route("4", "Puszcza Zielonka", "Leśna trasa dla rowerów górskich.", RouteType.CYCLING)
        )
        updateFilteredList()
    }

    fun selectType(type: RouteType) {
        _selectedType.value = type
        updateFilteredList()
    }

    private fun updateFilteredList() {
        _filteredRoutes.value = _allRoutes.value.filter { it.type == _selectedType.value }
    }

    fun getRouteById(id: String?): Route? {
        return _allRoutes.value.find { it.id == id }
    }

    // dla tabletu
    private val _selectedRouteId = MutableStateFlow<String?>(null)
    val selectedRouteId: StateFlow<String?> = _selectedRouteId.asStateFlow()

    fun selectRouteForDetail(routeId: String) {
        _selectedRouteId.value = routeId
    }

    // stoper
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _activeTimerRouteId = MutableStateFlow<String?>(null)
    val activeTimerRouteId: StateFlow<String?> = _activeTimerRouteId.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

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

    // Pomocnicza funkcja do formatowania czasu
    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return if (h > 0) {
            String.format("%02d:%02d", h, m)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}