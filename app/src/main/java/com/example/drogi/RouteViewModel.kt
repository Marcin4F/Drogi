package com.example.drogi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}