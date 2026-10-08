package com.rb.background.features.home.models

data class QuickTool(

    val title: String,

    val description: String,

    val iconResId: Int,
    val action: QuickToolAction,
    val backgroundColor: Int

)