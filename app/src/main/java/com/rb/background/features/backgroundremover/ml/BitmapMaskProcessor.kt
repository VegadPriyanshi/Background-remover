package com.rb.background.features.backgroundremover.ml

import android.graphics.Bitmap
import java.nio.FloatBuffer
import android.graphics.Color

object BitmapMaskProcessor {

    fun createTransparentBitmap(
        originalBitmap: Bitmap,
        confidenceMask: FloatBuffer,
        threshold: Float = 0.5f
    ): Bitmap {

        val width = originalBitmap.width
        val height = originalBitmap.height

        val outputBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for (y in 0 until height) {

            for (x in 0 until width) {

                val index = y * width + x
                val confidence = confidenceMask.get(index)
                if (confidence >= threshold) {
                    val pixel = originalBitmap.getPixel(x, y)
                    outputBitmap.setPixel(x, y, pixel)

                } else {

                    outputBitmap.setPixel(
                        x,
                        y,
                        Color.TRANSPARENT
                    )

                }

            }

        }

        return outputBitmap
    }

}