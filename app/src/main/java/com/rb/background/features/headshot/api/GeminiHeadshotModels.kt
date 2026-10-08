package com.rb.background.features.headshot.api

data class GeminiHeadshotRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val inline_data: GeminiInlineData? = null
)

data class GeminiInlineData(
    val mime_type: String,
    val data: String
)

data class GeminiGenerationConfig(
    val responseModalities: List<String>
)

data class GeminiHeadshotResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiResponseContent?
)

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>?
)

data class GeminiResponsePart(
    val text: String? = null,
    val inlineData: GeminiResponseImage? = null
)

data class GeminiResponseImage(
    val mimeType: String? = null,
    val data: String? = null
)