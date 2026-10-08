//package com.rb.background.features.templates.utils
package com.rb.background.features.backgroundremover.utils

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

object ImageDownloader {

    fun downloadBitmap(
        context: Context,
        imageUrl: String,
        onBitmapReady: (Bitmap?) -> Unit
    ) {
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .listener(object : RequestListener<Bitmap> {

                override fun onLoadFailed(

                    e: GlideException?,

                    model: Any?,

                    target: Target<Bitmap>,

                    isFirstResource: Boolean

                ): Boolean {

                    android.widget.Toast.makeText(

                        context,

                        "No Internet Connection",

                        android.widget.Toast.LENGTH_SHORT

                    ).show()
                    onBitmapReady(null)
                    return true

                }

                override fun onResourceReady(

                    resource: Bitmap,

                    model: Any,

                    target: Target<Bitmap>?,

                    dataSource: com.bumptech.glide.load.DataSource,

                    isFirstResource: Boolean

                ): Boolean {

                    return false

                }

            })

            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {

                    onBitmapReady(resource)

                }

                override fun onLoadCleared(
                    placeholder: android.graphics.drawable.Drawable?
                ) {
                }

            })

    }

}