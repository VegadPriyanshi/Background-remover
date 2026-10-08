package com.rb.background.features.backgroundremover.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.content.res.Resources

object CheckerboardGenerator {

    fun create(
        resources: Resources,
        width: Int,
        height: Int,
        cellSize: Int = 32
    ): Drawable {

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        val lightPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
        }

        val darkPaint = Paint().apply {
            color = Color.parseColor("#D9D9D9")
        }

        var drawLight = true

        for (y in 0 until height step cellSize) {

            drawLight = (y / cellSize) % 2 == 0

            for (x in 0 until width step cellSize) {

                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + cellSize).toFloat(),
                    (y + cellSize).toFloat(),
                    if (drawLight) lightPaint else darkPaint
                )

                drawLight = !drawLight
            }
        }

        return BitmapDrawable(resources, bitmap)
    }
}