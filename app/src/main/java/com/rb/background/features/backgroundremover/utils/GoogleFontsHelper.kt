package com.rb.background.features.backgroundremover.utils

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.rb.background.R

// Loads Google Fonts on demand via the downloadable-fonts provider that
// ships with Google Play services, so the app doesn't need to bundle font
// files for every family it offers in the text editor.
object GoogleFontsHelper {

    // A small curated set spanning sans, serif, script and display
    // styles for the in-app font picker.
    val FONT_NAMES = listOf(
        "Roboto",
        "Lato",
        "Montserrat",
        "Poppins",
        "Oswald",
        "Merriweather",
        "Playfair Display",
        "Pacifico",
        "Dancing Script",
        "Bebas Neue",
        "Anton",
        "Caveat",
        "Lobster",
        "Raleway",
        "Nunito"
    )

    private const val PROVIDER_AUTHORITY =
        "com.google.android.gms.fonts"

    private const val PROVIDER_PACKAGE =
        "com.google.android.gms"

    private val cache =
        mutableMapOf<String, Typeface?>()

    private val callbackHandler =
        Handler(Looper.getMainLooper())

    fun load(
        context: Context,
        fontName: String,
        onLoaded: (Typeface?) -> Unit
    ) {

        cache[fontName]?.let {
            onLoaded(it)
            return
        }

        val request = FontRequest(
            PROVIDER_AUTHORITY,
            PROVIDER_PACKAGE,
            "name=${fontName.replace(" ", "+")}",
            R.array.com_google_android_gms_fonts_certs
        )

        FontsContractCompat.requestFont(
            context.applicationContext,
            request,
            object : FontsContractCompat.FontRequestCallback() {

                override fun onTypefaceRetrieved(
                    typeface: Typeface
                ) {
                    cache[fontName] = typeface
                    onLoaded(typeface)
                }

                override fun onTypefaceRequestFailed(
                    reason: Int
                ) {
                    onLoaded(null)
                }
            },
            callbackHandler
        )
    }
}
