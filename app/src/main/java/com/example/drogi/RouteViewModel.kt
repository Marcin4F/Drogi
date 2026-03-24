package com.example.drogi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Route(val id: String, val name: String, val description: String)

class RouteViewModel : ViewModel() {

    private val _routes = MutableStateFlow<List<Route>>(emptyList())

    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    init {
        loadRoutes()
    }

    private fun loadRoutes() {
        _routes.value = listOf(
            Route("1", "Rusałka Loop", "Świetna trasa biegowa dookoła jeziora Rusałka w Poznaniu. Dystans ok. 4.5 km. Nawierzchnia szutrowa, brak większych wzniesień."),
            Route("2", "Wartostrada Rowerowa", "Asfaltowa trasa rowerowa wzdłuż rzeki Warty. Idealna na szybki trening szosowy z dala od ruchu samochodowego."),
            Route("3", "Rezerwat Morasko", "Wymagająca trasa z podbiegami w pobliżu rezerwatu meteorytów. Dużo korzeni i nierówności.")
        )
    }

    fun getRouteById(id: String?): Route? {
        return _routes.value.find { it.id == id }
    }
}