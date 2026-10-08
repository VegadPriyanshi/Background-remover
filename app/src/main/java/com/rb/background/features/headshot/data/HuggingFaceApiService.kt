package com.rb.background.features.headshot.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface HuggingFaceApiService {

    @POST("fal-ai/fal-ai/flux-kontext/dev")
    suspend fun generateHeadshot(
        @Header("Authorization") authorization: String,
        @Body request: HuggingFaceImageRequest
    ): Response<ResponseBody>
}