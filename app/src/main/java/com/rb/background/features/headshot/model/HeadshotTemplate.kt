package com.rb.background.features.headshot.model

import com.rb.background.features.templates.model.Photo

data class HeadshotTemplate(
    val id: String,
    val profession: String,
    val name: String,
    val photo: Photo,
    val prompt: String,
    val negativePrompt: String
)