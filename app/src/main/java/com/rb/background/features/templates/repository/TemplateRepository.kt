package com.rb.background.features.templates.repository

import com.rb.background.features.templates.api.RetrofitClient
import com.rb.background.BuildConfig


class TemplateRepository {

    suspend fun searchPhotos(
        query: String,
        page: Int = 1,
        orientation: String? = null
    ) =
        RetrofitClient.api.searchPhotos(

            apiKey = BuildConfig.PEXELS_API_KEY,

            query = query,
            page = page,
            orientation = orientation

        )

}