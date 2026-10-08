package com.rb.background.features.templates.api

import com.rb.background.features.templates.model.PexelsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApi {

    @GET("v1/search")
    suspend fun searchPhotos(

        @Header("Authorization")
        apiKey: String,

        @Query("query")
        query: String,

        @Query("page")
        page: Int = 1,

        @Query("per_page")
        perPage: Int = 20,

        @Query("orientation")
        orientation: String? = null

    ): Response<PexelsResponse>

}