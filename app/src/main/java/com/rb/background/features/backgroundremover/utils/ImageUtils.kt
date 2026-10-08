package com.rb.background.features.backgroundremover.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object ImageUtils {

    fun uriToBitmap(
        context: Context,
        imageUri: Uri
    ): Bitmap {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val source = ImageDecoder.createSource(
                context.contentResolver,
                imageUri
            )

            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->

                // Force software bitmap instead of hardware bitmap
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }

        } else {

            MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                imageUri
            )

        }

    }

}