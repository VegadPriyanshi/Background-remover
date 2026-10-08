package com.rb.background.features.home.repository

import android.graphics.Color
import com.rb.background.R
import com.rb.background.features.home.models.QuickTool
import com.rb.background.features.home.models.QuickToolAction

object QuickToolsRepository {

    fun getQuickTools(): List<QuickTool> {

        return listOf(

            QuickTool(
                title = "Remove Background",
                description = "Remove image background",
                iconResId = R.drawable.remove_background_visual,
                action = QuickToolAction.REMOVE_BACKGROUND,
                backgroundColor = Color.parseColor("#F1EBFF")
            ),

            QuickTool(
                title = "AI Headshot",
                description = "Create professional headshots",
                iconResId = R.drawable.ai_headshot_visual,
                action = QuickToolAction.AI_HEADSHOT,
                backgroundColor = Color.parseColor("#FFF0F7")
            ),

            QuickTool(
                title = "Passport Photo",
                description = "Create passport size photos",
                iconResId = R.drawable.passport_photo_visual,
                action = QuickToolAction.PASSPORT_PHOTO,
                backgroundColor = Color.parseColor("#EAF6FF")
            ),

            QuickTool(
                title = "Templates",
                description = "Choose beautiful backgrounds",
                iconResId = R.drawable.templates_visual,
                action = QuickToolAction.TEMPLATES,
                backgroundColor = Color.parseColor("#F5EEFF")
            )

        )

    }

}