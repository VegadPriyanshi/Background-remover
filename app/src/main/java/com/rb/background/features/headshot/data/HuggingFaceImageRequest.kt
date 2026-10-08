package com.rb.background.features.headshot.data

data class HuggingFaceImageRequest(
    val inputs: String,
    val parameters: HuggingFaceParameters
)

data class HuggingFaceParameters(
    val prompt: String
)