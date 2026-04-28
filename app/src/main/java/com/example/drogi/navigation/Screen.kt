package com.example.drogi.navigation

sealed class Screen(val route: String) {
    object RouteList : Screen("routeList")
    object RouteDetail : Screen("routeDetail/{routeId}") {
        fun createRoute(routeId: String) = "routeDetail/$routeId"
    }
    object RouteResults : Screen("routeResults/{routeId}") {
        fun createRoute(routeId: String) = "routeResults/$routeId"
    }
}