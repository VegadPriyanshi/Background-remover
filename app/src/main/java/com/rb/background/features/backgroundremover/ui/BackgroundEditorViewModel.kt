package com.rb.background.features.backgroundremover.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import androidx.lifecycle.ViewModel

class BackgroundEditorViewModel : ViewModel() {

    // Original photo selected by the user
    var selectedBitmap: Bitmap? = null

    // Photo after background removal
    var editableBitmap: Bitmap? = null

    // Current customized result
    var resultBitmap: Bitmap? = null

    // Background image selected from gallery
    var backgroundBitmap: Bitmap? = null

    // Currently selected background color
    var selectedBackgroundColor: Int = Color.WHITE

    // --------------------------------------------------
    // TEXT LAYERS
    // --------------------------------------------------

    val textItems = mutableListOf<TextItem>()

    var editingTextItem: TextItem? = null

    // Information belonging to one text object
    data class TextItem(
        var text: String,
        var x: Float = 0f,
        var y: Float = 0f,
        var rotation: Float = 0f,
        var textColor: Int = Color.WHITE,
        // Google Fonts family name, e.g. "Pacifico". Null means the
        // app's default typeface.
        var fontFamily: String? = null,
        var textAlignment: Int = Gravity.CENTER,
        var letterSpacingEm: Float = 0f,
        var lineSpacingMultiplier: Float = 1f
    )

    fun clear() {
        selectedBitmap = null
        editableBitmap = null
        resultBitmap = null
        backgroundBitmap = null
        selectedBackgroundColor = Color.WHITE

        textItems.clear()
    }
}