package com.rb.background.features.headshot.data

import com.google.gson.JsonElement

data class InstantIdGenerateRequest(
    val data: List<JsonElement>
)

data class InstantIdFileData(
    val path: String,
    val meta: InstantIdFileMeta = InstantIdFileMeta()
)

data class InstantIdFileMeta(
    val `_type`: String = "gradio.FileData"
)

data class InstantIdEventResponse(
    val event_id: String
)