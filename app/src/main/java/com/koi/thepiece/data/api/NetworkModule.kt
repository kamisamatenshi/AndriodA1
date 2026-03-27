package com.koi.thepiece.data.api

import com.koi.thepiece.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Centralized network configuration module.
 *
 * This object is responsible for:
 * - Configuring JSON serialization (Moshi)
 * - Configuring HTTP client behavior (OkHttp)
 * - Creating Retrofit instance
 * - Providing API service interfaces
 *
 * Ensures consistent networking behavior across the application.
 */
object NetworkModule {

    /**
     * Moshi instance configured with Kotlin reflection support.
     *
     * Used to serialize and deserialize JSON responses
     * between the Android client and backend APIs.
     */
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * OkHttp client configuration.
     *
     * Includes an HTTP logging interceptor to log request/response metadata.
     * Logging level is set to BASIC for lightweight debugging visibility.
     */
    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    /**
     * Retrofit instance configured with:
     * - Base URL from BuildConfig
     * - OkHttp client
     * - Moshi JSON converter
     *
     * This instance is reused across all API interfaces.
     */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val retrofit_PRICE: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.PRICE_URL)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * API service for catalogue-related endpoints.
     */
    val catalogApi: CatalogApi = retrofit.create(CatalogApi::class.java)

    val PriceApi: CatalogApi = retrofit_PRICE.create(CatalogApi::class.java)

    /**
     * API service for authentication-related endpoints.
     */
    val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    /**
     * API service for deck-related endpoints.
     */
    val deckApi: DeckApi = retrofit.create(DeckApi::class.java)
}