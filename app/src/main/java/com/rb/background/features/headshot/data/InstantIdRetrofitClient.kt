package com.rb.background.features.headshot.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object InstantIdRetrofitClient {

    private const val BASE_URL =
        "https://instantx-instantid.hf.space/"

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    // The generation-result endpoint is a long-held SSE
    // connection that blocks until the ZeroGPU job finishes
    // (queueing + inference can take well over a minute), so
    // this needs a much longer read timeout than OkHttp's
    // 10s default.
    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    val apiService: InstantIdApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(InstantIdApiService::class.java)
}