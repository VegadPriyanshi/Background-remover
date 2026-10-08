package com.rb.background.features.backgroundremover.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

object ImageSaver {

    fun saveBitmap(
        context: Context,
        bitmap: Bitmap
    ): Boolean {

        return try {

            val fileName =
                "BG_${System.currentTimeMillis()}.png"

            val values = ContentValues().apply {

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/png"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/BackgroundRemover"
                    )

                }

            }

            val uri =
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return false

            context.contentResolver.openOutputStream(uri)?.use {

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    it
                )

            }

            true

        } catch (e: Exception) {

            e.printStackTrace()

            false

        }

    }

}