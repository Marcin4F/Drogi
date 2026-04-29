package com.example.drogi

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS) // na połączenie
        .readTimeout(5, TimeUnit.SECONDS)    // na pobranie danych
        .writeTimeout(5, TimeUnit.SECONDS)   // na wysłanie
        .build()

    val apiService: RouteApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RouteApiService::class.java)
    }
}