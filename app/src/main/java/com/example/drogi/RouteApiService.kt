package com.example.drogi

import retrofit2.http.GET

interface RouteApiService {
    @GET("Marcin4F/drogi-api/refs/heads/main/routes.json")
    suspend fun getRoutes(): List<Route>
}