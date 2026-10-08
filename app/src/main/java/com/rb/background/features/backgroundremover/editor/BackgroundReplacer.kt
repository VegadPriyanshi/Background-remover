package com.rb.background.features.backgroundremover.editor

import android.graphics.Bitmap
import android.graphics.Canvas

object BackgroundReplacer {

    fun replaceWithColor(
        foregroundBitmap: Bitmap,
        backgroundColor: Int
    ): Bitmap {

        val resultBitmap = Bitmap.createBitmap(
            foregroundBitmap.width,
            foregroundBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)

        canvas.drawColor(backgroundColor)

        canvas.drawBitmap(
            foregroundBitmap,
            0f,
            0f,
            null
        )

        return resultBitmap
    }

    fun replaceWithBitmap(
        foregroundBitmap: Bitmap,
        backgroundBitmap: Bitmap
    ): Bitmap {

        // Convert hardware bitmap to software bitmap
        val softwareBackground =
            if (backgroundBitmap.config == Bitmap.Config.HARDWARE) {
                backgroundBitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                backgroundBitmap
            }

        val scaledBackground = Bitmap.createScaledBitmap(
            softwareBackground,
            foregroundBitmap.width,
            foregroundBitmap.height,
            true
        )

        val resultBitmap = Bitmap.createBitmap(
            foregroundBitmap.width,
            foregroundBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)

        canvas.drawBitmap(
            scaledBackground,
            0f,
            0f,
            null
        )

        canvas.drawBitmap(
            foregroundBitmap,
            0f,
            0f,
            null
        )

        return resultBitmap
    }
}