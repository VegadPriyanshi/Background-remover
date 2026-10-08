package com.rb.background.features.backgroundremover.editor

import android.graphics.Bitmap

object BitmapEditor {

    fun createEditableBitmap(
        bitmap: Bitmap
    ): Bitmap {

        return bitmap.copy(
            Bitmap.Config.ARGB_8888,
            true
        )

    }

}