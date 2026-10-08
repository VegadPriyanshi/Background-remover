package com.rb.background.features.templates.model

data class PexelsResponse(

    val page: Int,

    val per_page: Int,

    val photos: List<Photo>

)