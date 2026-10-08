package com.rb.background.features.headshot.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiHeadshotApiService {

    @POST("v1/models/gemini-3.1-flash-image:generateContent")
    suspend fun generateHeadshot(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiHeadshotRequest
    ): Response<GeminiHeadshotResponse>
}