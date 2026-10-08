package com.rb.background.features.headshot.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface InstantIdApiService {

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Header("Authorization")
        authorization: String,

        @Part file: MultipartBody.Part
    ): Response<List<String>>

    @POST("call/generate_image")
    suspend fun generateImage(
        @Header("Authorization")
        authorization: String,

        @Body request: RequestBody
    ): Response<ResponseBody>

    @GET("call/generate_image/{eventId}")
    suspend fun getGenerationResult(
        @Header("Authorization")
        authorization: String,

        @Path("eventId")
        eventId: String
    ): Response<ResponseBody>
}