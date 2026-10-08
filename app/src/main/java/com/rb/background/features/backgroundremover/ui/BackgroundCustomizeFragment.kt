package com.rb.background.features.backgroundremover.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rb.background.R
import com.rb.background.databinding.FragmentBackgroundCustomizeBinding
import com.rb.background.features.backgroundremover.bottomsheet.BackgroundOptionsBottomSheet
import com.rb.background.features.backgroundremover.bottomsheet.CanvasSizeBottomSheet
import com.rb.background.features.backgroundremover.bottomsheet.ColorPickerBottomSheet
import com.rb.background.features.backgroundremover.editor.BackgroundReplacer
import com.rb.background.features.backgroundremover.utils.GoogleFontsHelper
import com.rb.background.features.backgroundremover.utils.ImageDownloader
import com.rb.background.features.backgroundremover.utils.ImageUtils
import com.rb.background.features.templates.ui.TemplateBottomSheet
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class BackgroundCustomizeFragment : Fragment() {

    private var _binding: FragmentBackgroundCustomizeBinding? = null
    private val binding get() = _binding!!



    private val editorViewModel: BackgroundEditorViewModel by activityViewModels()

    // Background-removed foreground
    private var editableBitmap: Bitmap? = null

    private var selectedTextView: TextView? = null


    private var deleteButton: TextView? = null

    private var resizeButton: TextView? = null

    private var rotateButton: TextView? = null

    private var pencilButton: TextView? = null

    private var fontOptionAdapter: FontOptionAdapter? = null

    // Current background selected by user
    private var backgroundBitmap: Bitmap? = null

    // Selected color
    private var selectedBackgroundColor = Color.WHITE

    // Whether the user selected a real background
    private var hasCustomBackground = false

    private var selectedCanvasSize =
        CanvasSizeBottomSheet.CanvasSize.SQUARE

    // Pixel size actually applied to previewCanvas. Cached across view
    // recreation (e.g. returning from TextEditorFragment) so the canvas
    // doesn't get re-measured/resized from scratch on every return trip.
    private var appliedCanvasWidthPx: Int? = null
    private var appliedCanvasHeightPx: Int? = null

    private var resizeStartX = 0f
    private var resizeStartY = 0f
    private var resizeStartTextSize = 0f

    private var rotateStartAngle = 0f
    private var rotateStartRotation = 0f




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBackgroundCustomizeBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        editableBitmap = editorViewModel.editableBitmap

        if (editableBitmap == null) {

            Toast.makeText(
                requireContext(),
                "Image is not available.",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

            return
        }

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        /*
         * Initially show the background-removed foreground.
         */
        binding.imgForeground.setImageBitmap(
            editableBitmap
        )
        binding.imgForeground.isTransformEnabled = true
        binding.imgBackground.isTransformEnabled = false

        /*
         * Initially show checkerboard only as a visual
         * indication of transparency.
         */
        binding.imgBackground.setBackgroundResource(
            R.drawable.checkerboard
        )

        /*
         * Position the foreground in the center.
         */
        binding.imgForeground.post {
            fitForegroundToPreview()
        }

        /*
         * Open:
         * Solid Color / Gallery / Templates
         */
        /*binding.btnSelectBackground.setOnClickListener {

            showBackgroundOptions()
        }*/

        binding.toolBackground.setOnClickListener {
            showBackgroundOptions()
        }

        /*
         * Create final image using the current
         * foreground position and scale.
         */
        binding.btnDone.setOnClickListener {

            val finalBitmap = createFinalBitmap()

            if (finalBitmap == null) {

                Toast.makeText(
                    requireContext(),
                    "Unable to create final image.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }
            Log.d(
                "CanvasExport",
                "Final bitmap: ${finalBitmap.width} x ${finalBitmap.height}"
            )

            editorViewModel.resultBitmap = finalBitmap

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(
                    R.id.fragmentContainer,
                    BackgroundSaveProgressFragment()
                )
                .addToBackStack(null)
                .commit()
        }
        binding.toolCanvas.setOnClickListener {

            val bottomSheet =
                CanvasSizeBottomSheet()

            bottomSheet.setOnCanvasSizeSelectedListener { size ->

                selectedCanvasSize = size

                applyCanvasSize(size)
            }

            bottomSheet.show(
                childFragmentManager,
                "CanvasSizeBottomSheet"
            )
        }
        val cachedWidth = appliedCanvasWidthPx
        val cachedHeight = appliedCanvasHeightPx

        if (cachedWidth != null && cachedHeight != null) {

            // Re-apply the previously computed size directly instead of
            // re-measuring editorCanvasCard, which can race with layout
            // during the back-stack return transition and produce a
            // wrong (much smaller) canvas.
            val params = binding.previewCanvas.layoutParams
            params.width = cachedWidth
            params.height = cachedHeight
            binding.previewCanvas.layoutParams = params

        } else {

            binding.editorCanvasCard.post {
                applyCanvasSize(selectedCanvasSize)
            }
        }
        restoreTextItems()



        binding.toolText.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(
                    R.id.fragmentContainer,
                    TextEditorFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        parentFragmentManager.setFragmentResultListener(
            TextEditorFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->

            val text =
                bundle.getString(
                    TextEditorFragment.RESULT_TEXT
                )

            val isEdit =
                bundle.getBoolean(
                    TextEditorFragment.RESULT_IS_EDIT,
                    false
                )

            if (text.isNullOrEmpty()) {
                return@setFragmentResultListener
            }

            if (isEdit) {

                // Update the existing TextView
                updateEditedText(text)

            } else {

                // Create a completely new TextView
                addTextToCanvas(text)
            }
        }

        binding.previewCanvas.setOnClickListener {

            clearTextSelection()
        }

        setupTextFormatToolbar()
    }


    private fun updateEditedText(
        newText: String
    ) {

        val editingItem =
            editorViewModel.editingTextItem
                ?: return

        for (i in 0 until binding.previewCanvas.childCount) {

            val child =
                binding.previewCanvas.getChildAt(i)

            if (child is TextView) {

                val item =
                    child.tag
                            as? BackgroundEditorViewModel.TextItem

                if (item === editingItem) {

                    // Replace the old visible text
                    child.text = newText

                    break
                }
            }
        }

        // Editing is finished
        editorViewModel.editingTextItem = null
    }

    private fun deleteSelectedText() {

        val textView = selectedTextView
            ?: return

        val textItem =
            textView.tag
                    as? BackgroundEditorViewModel.TextItem
                ?: return

        closeTextFormatToolbar()

        editorViewModel.textItems.remove(textItem)

        binding.previewCanvas.removeView(textView)

        deleteButton?.let { button ->
            binding.previewCanvas.removeView(button)
        }

        resizeButton?.let { button ->
            binding.previewCanvas.removeView(button)
        }

        rotateButton?.let { button ->
            binding.previewCanvas.removeView(button)
        }

        pencilButton?.let { button ->
            binding.previewCanvas.removeView(button)
        }

        deleteButton = null
        resizeButton = null
        rotateButton = null
        pencilButton = null
        selectedTextView = null
    }


    // --------------------------------------------------
    // TEXT FORMAT TOOLBAR
    // (opened via the pencil handle on a selected text)
    // --------------------------------------------------

    private enum class FormatTab {
        FONT, COLOR, ALIGN, SPACE, LINE, CAPS
    }

    private fun setupTextFormatToolbar() {

        val adapter = FontOptionAdapter(
            GoogleFontsHelper.FONT_NAMES
        ) { fontName ->
            applyFontToSelectedText(fontName)
        }

        fontOptionAdapter = adapter

        binding.rvFontList.adapter = adapter

        buildColorSwatchRow()

        binding.btnAlignLeft.setOnClickListener {
            applyAlignmentToSelectedText(Gravity.START)
        }

        binding.btnAlignCenter.setOnClickListener {
            applyAlignmentToSelectedText(Gravity.CENTER)
        }

        binding.btnAlignRight.setOnClickListener {
            applyAlignmentToSelectedText(Gravity.END)
        }

        binding.btnCapsFirst.setOnClickListener {
            applyTextCase { text ->
                text.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
            }
        }

        binding.btnCapsSmall.setOnClickListener {
            applyTextCase { it.lowercase() }
        }

        binding.btnCapsAll.setOnClickListener {
            applyTextCase { it.uppercase() }
        }

        binding.seekLetterSpacing.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) applyLetterSpacing(progress)
                }

                override fun onStartTrackingTouch(
                    seekBar: android.widget.SeekBar?
                ) = Unit

                override fun onStopTrackingTouch(
                    seekBar: android.widget.SeekBar?
                ) = Unit
            }
        )

        binding.seekLineSpacing.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) applyLineSpacing(progress)
                }

                override fun onStartTrackingTouch(
                    seekBar: android.widget.SeekBar?
                ) = Unit

                override fun onStopTrackingTouch(
                    seekBar: android.widget.SeekBar?
                ) = Unit
            }
        )

        binding.tabFont.setOnClickListener { switchFormatTab(FormatTab.FONT) }
        binding.tabColor.setOnClickListener { switchFormatTab(FormatTab.COLOR) }
        binding.tabAlign.setOnClickListener { switchFormatTab(FormatTab.ALIGN) }
        binding.tabSpace.setOnClickListener { switchFormatTab(FormatTab.SPACE) }
        binding.tabLine.setOnClickListener { switchFormatTab(FormatTab.LINE) }
        binding.tabCaps.setOnClickListener { switchFormatTab(FormatTab.CAPS) }
    }

    private fun buildColorSwatchRow() {

        val presetColors = listOf(
            Color.WHITE,
            Color.rgb(190, 190, 190),
            Color.rgb(140, 140, 140),
            Color.rgb(74, 74, 74),
            Color.BLACK,
            Color.rgb(255, 226, 75),
            Color.rgb(255, 193, 7),
            Color.rgb(255, 152, 0),
            Color.rgb(238, 66, 102),
            Color.rgb(105, 69, 181),
            Color.rgb(46, 134, 171),
            Color.rgb(46, 204, 113)
        )

        val swatchSize = dpToPx(32f)

        presetColors.forEach { color ->

            val swatch = View(requireContext()).apply {

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(dpToPx(1f), Color.rgb(220, 214, 232))
                }

                setOnClickListener {
                    applyColorToSelectedText(color)
                }
            }

            val params = LinearLayout.LayoutParams(
                swatchSize,
                swatchSize
            )

            params.marginEnd = dpToPx(10f)

            binding.colorSwatchRow.addView(swatch, params)
        }
    }

    private fun openTextFormatToolbar(textView: TextView) {

        syncFormatToolbarToSelection(textView)

        binding.editorToolsCard.visibility = View.GONE
        binding.textFormatCard.visibility = View.VISIBLE

        switchFormatTab(FormatTab.FONT)
    }

    private fun closeTextFormatToolbar() {

        binding.textFormatCard.visibility = View.GONE
        binding.editorToolsCard.visibility = View.VISIBLE
    }

    private fun syncFormatToolbarToSelection(textView: TextView) {

        val textItem =
            textView.tag as? BackgroundEditorViewModel.TextItem
                ?: return

        fontOptionAdapter?.selectedFontName = textItem.fontFamily

        binding.tabColorIcon.setTextColor(textItem.textColor)

        updateAlignChipStates(textItem.textAlignment)

        binding.seekLetterSpacing.progress =
            ((textItem.letterSpacingEm / 0.5f) * 100f)
                .roundToInt()
                .coerceIn(0, 100)

        binding.seekLineSpacing.progress =
            (((textItem.lineSpacingMultiplier - 1f) / 1.5f) * 100f)
                .roundToInt()
                .coerceIn(0, 100)
    }

    private fun switchFormatTab(tab: FormatTab) {

        binding.rvFontList.visibility =
            if (tab == FormatTab.FONT) View.VISIBLE else View.GONE

        binding.colorPanel.visibility =
            if (tab == FormatTab.COLOR) View.VISIBLE else View.GONE

        binding.alignPanel.visibility =
            if (tab == FormatTab.ALIGN) View.VISIBLE else View.GONE

        binding.spacePanel.visibility =
            if (tab == FormatTab.SPACE) View.VISIBLE else View.GONE

        binding.linePanel.visibility =
            if (tab == FormatTab.LINE) View.VISIBLE else View.GONE

        binding.capsPanel.visibility =
            if (tab == FormatTab.CAPS) View.VISIBLE else View.GONE

        binding.tabFontIcon.setBackgroundResource(
            if (tab == FormatTab.FONT) R.drawable.bg_circle_button_active
            else R.drawable.bg_circle_button
        )

        binding.tabColorIcon.setBackgroundResource(
            if (tab == FormatTab.COLOR) R.drawable.bg_circle_button_active
            else R.drawable.bg_circle_button
        )

        binding.tabAlignIcon.setBackgroundResource(
            if (tab == FormatTab.ALIGN) R.drawable.bg_circle_button_active
            else R.drawable.bg_circle_button
        )

        binding.tabSpaceIcon.setBackgroundResource(
            if (tab == FormatTab.SPACE) R.drawable.bg_circle_button_active
            else R.drawable.bg_circle_button
        )

        binding.tabLineIcon.setBackgroundResource(
            if (tab == FormatTab.LINE) R.drawable.bg_circle_button_active
            else R.drawable.bg_circle_button
        )

        binding.tabCapsIcon.setBackgroundResource(
            if (tab == FormatTab.CAPS) R.drawable.bg_circle_button_active
            else R.drawable.bg_circle_button
        )

        val activeTextColor = Color.WHITE
        val inactiveTextColor = Color.parseColor("#403A48")

        binding.tabFontIcon.setTextColor(
            if (tab == FormatTab.FONT) activeTextColor else inactiveTextColor
        )

        // The color tab's dot always shows the selected text's actual
        // color, so its text color is never overridden here.

        binding.tabAlignIcon.setTextColor(
            if (tab == FormatTab.ALIGN) activeTextColor else inactiveTextColor
        )

        binding.tabSpaceIcon.setTextColor(
            if (tab == FormatTab.SPACE) activeTextColor else inactiveTextColor
        )

        binding.tabLineIcon.setTextColor(
            if (tab == FormatTab.LINE) activeTextColor else inactiveTextColor
        )

        binding.tabCapsIcon.setTextColor(
            if (tab == FormatTab.CAPS) activeTextColor else inactiveTextColor
        )
    }

    private fun currentTextItem():
            BackgroundEditorViewModel.TextItem? {

        return selectedTextView?.tag
                as? BackgroundEditorViewModel.TextItem
    }

    private fun repositionHandles(textView: TextView) {

        textView.post {
            positionDeleteButton(textView)
            positionResizeButton(textView)
            positionRotateButton(textView)
            positionPencilButton(textView)
        }
    }

    private fun applyFontToSelectedText(fontName: String) {

        val textView = selectedTextView ?: return
        val textItem = currentTextItem() ?: return

        if (fontName.isEmpty()) {

            textView.setTypeface(null, Typeface.BOLD)
            textItem.fontFamily = null
            fontOptionAdapter?.selectedFontName = null

            repositionHandles(textView)

            return
        }

        textItem.fontFamily = fontName
        fontOptionAdapter?.selectedFontName = fontName

        GoogleFontsHelper.load(requireContext(), fontName) { typeface ->

            if (typeface != null && selectedTextView == textView) {

                textView.typeface = typeface

                repositionHandles(textView)
            }
        }
    }

    private fun applyColorToSelectedText(color: Int) {

        val textView = selectedTextView ?: return
        val textItem = currentTextItem() ?: return

        textView.setTextColor(color)
        textItem.textColor = color

        binding.tabColorIcon.setTextColor(color)
    }

    private fun applyAlignmentToSelectedText(gravity: Int) {

        val textView = selectedTextView ?: return
        val textItem = currentTextItem() ?: return

        textView.gravity = gravity
        textItem.textAlignment = gravity

        updateAlignChipStates(gravity)
    }

    private fun updateAlignChipStates(gravity: Int) {

        setChipActive(binding.btnAlignLeft, gravity == Gravity.START)
        setChipActive(binding.btnAlignCenter, gravity == Gravity.CENTER)
        setChipActive(binding.btnAlignRight, gravity == Gravity.END)
    }

    private fun setChipActive(chip: TextView, active: Boolean) {

        chip.setBackgroundResource(
            if (active) R.drawable.bg_format_chip_active
            else R.drawable.bg_format_chip
        )

        chip.setTextColor(
            if (active) Color.WHITE else Color.parseColor("#403A48")
        )
    }

    private fun applyLetterSpacing(progress: Int) {

        val textView = selectedTextView ?: return
        val textItem = currentTextItem() ?: return

        val em = progress / 100f * 0.5f

        textView.letterSpacing = em
        textItem.letterSpacingEm = em

        repositionHandles(textView)
    }

    private fun applyLineSpacing(progress: Int) {

        val textView = selectedTextView ?: return
        val textItem = currentTextItem() ?: return

        val multiplier = 1f + progress / 100f * 1.5f

        textView.setLineSpacing(0f, multiplier)
        textItem.lineSpacingMultiplier = multiplier

        repositionHandles(textView)
    }

    private fun applyTextCase(transform: (String) -> String) {

        val textView = selectedTextView ?: return
        val textItem = currentTextItem() ?: return

        val transformed = transform(textItem.text)

        textView.text = transformed
        textItem.text = transformed

        repositionHandles(textView)
    }


    private fun restoreTextItems() {

        editorViewModel.textItems.forEach { textItem ->

            addTextViewOnly(textItem)
        }
    }

    private fun addTextViewOnly(
        textItem: BackgroundEditorViewModel.TextItem
    ) {

        val textView = TextView(requireContext())

        textView.text = textItem.text

        textView.setTextColor(textItem.textColor)

        textView.textSize = 28f

        textView.setTypeface(
            null,
            Typeface.BOLD
        )

        textView.gravity = textItem.textAlignment

        textView.letterSpacing = textItem.letterSpacingEm

        textView.setLineSpacing(0f, textItem.lineSpacingMultiplier)

        textItem.fontFamily?.let { fontName ->

            GoogleFontsHelper.load(requireContext(), fontName) { typeface ->

                if (typeface != null) {
                    textView.typeface = typeface
                }
            }
        }

        textView.setPadding(
            16,
            8,
            16,
            8
        )

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        binding.previewCanvas.addView(
            textView,
            params
        )

        textView.post {

            // Restore the position where
            // the user previously placed this text.
            textView.x = textItem.x
            textView.y = textItem.y
            textView.rotation = textItem.rotation

            // Keep the TextItem connected
            // to this TextView.
            textView.tag = textItem

            makeTextMovable(textView)
        }
    }


    private fun addTextToCanvas(text: String) {

        val textView = TextView(requireContext())

        textView.text = text

        textView.setTextColor(Color.WHITE)

        textView.textSize = 28f

        textView.setTypeface(
            null,
            Typeface.BOLD
        )

        textView.gravity = Gravity.CENTER

        textView.setPadding(
            16,
            8,
            16,
            8
        )

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        params.gravity = Gravity.CENTER

        binding.previewCanvas.addView(
            textView,
            params
        )

        textView.post {

            val textItem =
                BackgroundEditorViewModel.TextItem(
                    text = text,
                    x = textView.x,
                    y = textView.y
                )

            editorViewModel.textItems.add(
                textItem
            )

            textView.tag = textItem

            makeTextMovable(textView)
        }
    }


    // ----------------------------------------------------------------
    // BACKGROUND OPTIONS
    // ----------------------------------------------------------------

    private fun showBackgroundOptions() {

        val bottomSheet = BackgroundOptionsBottomSheet()


        /*
         * SOLID COLOR
         */
        bottomSheet.setOnSolidColorClickListener {

            val colorSheet = ColorPickerBottomSheet()

            colorSheet.setInitialColor(
                selectedBackgroundColor
            )

            colorSheet.setOnColorSelectedListener { color ->

                applyBackgroundColor(color)
            }

            colorSheet.show(
                childFragmentManager,
                "ColorPickerBottomSheet"
            )
        }


        /*
         * GALLERY
         */
        bottomSheet.setOnGalleryClickListener {

            backgroundPickerLauncher.launch("image/*")
        }


        /*
         * TEMPLATES
         */
        bottomSheet.setOnTemplatesClickListener {

            val templateSheet = TemplateBottomSheet()

            templateSheet.setOnTemplateSelectedListener { photo ->

                val foreground = editableBitmap

                if (foreground == null) {

                    Toast.makeText(
                        requireContext(),
                        "Please remove the background first.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnTemplateSelectedListener
                }

                ImageDownloader.downloadBitmap(
                    context = requireContext(),
                    imageUrl = photo.src.large2x
                ) { background ->

                    if (!isAdded || _binding == null) {
                        return@downloadBitmap
                    }

                    if (background == null) {

                        Toast.makeText(
                            requireContext(),
                            "No Internet Connection",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@downloadBitmap
                    }

                    /*
                     * Keep the downloaded template as a
                     * separate background layer.
                     */
                    backgroundBitmap = background
                    hasCustomBackground = true

                    binding.imgBackground.background = null

                    backgroundBitmap = background
                    hasCustomBackground = true

                    binding.imgBackground.background = null

                    binding.imgBackground.setImageBitmap(
                        background
                    )

                    binding.imgBackground.resetTransform()

                    binding.imgBackground.post {
                        fitBackgroundToPreview()

                        binding.imgBackground.isTransformEnabled = true
                        binding.imgForeground.isTransformEnabled = true
                    }
                }
            }

            templateSheet.show(
                childFragmentManager,
                "TemplateBottomSheet"
            )
        }


        bottomSheet.show(
            childFragmentManager,
            "BackgroundOptionsBottomSheet"
        )
    }


    // ----------------------------------------------------------------
    // SOLID COLOR
    // ----------------------------------------------------------------

    private fun applyBackgroundColor(color: Int) {

        selectedBackgroundColor = color

        backgroundBitmap = null
        hasCustomBackground = true

        binding.imgBackground.isTransformEnabled = false
        binding.imgForeground.isTransformEnabled = true

        binding.imgBackground.setImageDrawable(null)

        binding.imgBackground.setBackgroundColor(
            color
        )
    }


    // ----------------------------------------------------------------
    // GALLERY BACKGROUND
    // ----------------------------------------------------------------

    private val backgroundPickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri ?: return@registerForActivityResult

            val bitmap = ImageUtils.uriToBitmap(
                requireContext(),
                uri
            )

            if (bitmap == null) {

                Toast.makeText(
                    requireContext(),
                    "Unable to load background.",
                    Toast.LENGTH_SHORT
                ).show()

                return@registerForActivityResult
            }

            backgroundBitmap = bitmap
            hasCustomBackground = true

            binding.imgBackground.background = null

            binding.imgBackground.setImageBitmap(
                bitmap
            )

            binding.imgBackground.resetTransform()

            binding.imgBackground.post {
                fitBackgroundToPreview()

                // Enable background dragging and resizing
                binding.imgBackground.isTransformEnabled = true

                // Disable foreground transformation
                binding.imgForeground.isTransformEnabled = true
            }

            Toast.makeText(
                requireContext(),
                "Background applied successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }


    // ----------------------------------------------------------------
    // INITIAL FOREGROUND POSITION
    // ----------------------------------------------------------------

    private fun fitForegroundToPreview() {

        val bitmap = editableBitmap
            ?: return

        val viewWidth = binding.imgForeground.width
        val viewHeight = binding.imgForeground.height

        if (viewWidth <= 0 || viewHeight <= 0) {
            return
        }

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val scaleX = viewWidth / bitmapWidth
        val scaleY = viewHeight / bitmapHeight

        /*
         * Start at approximately 65% of the
         * available preview size.
         */
        val scale = minOf(
            scaleX,
            scaleY
        ) * 0.65f

        val scaledWidth =
            bitmapWidth * scale

        val scaledHeight =
            bitmapHeight * scale

        val left =
            (viewWidth - scaledWidth) / 2f

        val top =
            (viewHeight - scaledHeight) / 2f

        val matrix = Matrix()

        matrix.postScale(
            scale,
            scale
        )

        matrix.postTranslate(
            left,
            top
        )

        binding.imgForeground.imageMatrix =
            matrix
    }


    // ----------------------------------------------------------------
    // CREATE FINAL IMAGE
    // ----------------------------------------------------------------

    private fun createFinalBitmap(): Bitmap? {

        // Deselecting first removes the selection outline and the
        // handle buttons so neither gets baked into the export.
        clearTextSelection()

        // -------------------------------------------------
        // PREVIEW DIMENSIONS
        // -------------------------------------------------

        val previewWidth =
            binding.previewCanvas.width

        val previewHeight =
            binding.previewCanvas.height

        if (
            previewWidth <= 0 ||
            previewHeight <= 0
        ) {
            return null
        }


        // -------------------------------------------------
        // EXPORT DIMENSIONS
        // -------------------------------------------------

        val (exportWidth, exportHeight) =
            getExportSize()


        // -------------------------------------------------
        // CREATE FINAL BITMAP
        // -------------------------------------------------

        val finalBitmap = Bitmap.createBitmap(
            exportWidth,
            exportHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas =
            android.graphics.Canvas(finalBitmap)


        // -------------------------------------------------
        // SCALE PREVIEW COMPOSITION TO EXPORT SIZE
        // -------------------------------------------------

        val scaleX =
            exportWidth.toFloat() /
                    previewWidth.toFloat()

        val scaleY =
            exportHeight.toFloat() /
                    previewHeight.toFloat()

        canvas.scale(
            scaleX,
            scaleY
        )


        // -------------------------------------------------
        // DRAW BACKGROUND
        // -------------------------------------------------

        /*
         * Draw the selected background only.
         *
         * The checkerboard is only a transparency
         * indicator and must not be exported.
         */

        if (hasCustomBackground) {

            binding.imgBackground.draw(
                canvas
            )
        }


        // -------------------------------------------------
        // DRAW FOREGROUND
        // -------------------------------------------------

        /*
         * Draw using the foreground's current Matrix.
         *
         * Therefore:
         * - Current position is preserved
         * - Current scale is preserved
         * - Current transformation is preserved
         */

        binding.imgForeground.draw(
            canvas
        )


        // -------------------------------------------------
        // DRAW TEXT LAYERS
        // -------------------------------------------------

        for (i in 0 until binding.previewCanvas.childCount) {

            val child = binding.previewCanvas.getChildAt(i)

            if (
                child is TextView &&
                child.tag is BackgroundEditorViewModel.TextItem
            ) {

                canvas.save()

                canvas.translate(child.x, child.y)

                if (child.rotation != 0f) {

                    canvas.rotate(
                        child.rotation,
                        child.width / 2f,
                        child.height / 2f
                    )
                }

                child.draw(canvas)

                canvas.restore()
            }
        }


        return finalBitmap
    }
    private fun fitBackgroundToPreview() {

        val bitmap = backgroundBitmap
            ?: return

        val viewWidth = binding.imgBackground.width
        val viewHeight = binding.imgBackground.height

        if (viewWidth <= 0 || viewHeight <= 0) {
            return
        }

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        /*
         * Scale the image so that it completely
         * covers the preview.
         */
        val scaleX = viewWidth / bitmapWidth
        val scaleY = viewHeight / bitmapHeight

        val scale = maxOf(
            scaleX,
            scaleY
        )

        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale

        val left =
            (viewWidth - scaledWidth) / 2f

        val top =
            (viewHeight - scaledHeight) / 2f

        val matrix = Matrix()

        matrix.postScale(
            scale,
            scale
        )

        matrix.postTranslate(
            left,
            top
        )

        binding.imgBackground.imageMatrix = matrix
    }

    private fun applyCanvasSize(
        size: CanvasSizeBottomSheet.CanvasSize
    ) {

        val aspectRatio = when (size) {

            CanvasSizeBottomSheet.CanvasSize.SQUARE ->
                1f

            CanvasSizeBottomSheet.CanvasSize.VERTICAL ->
                4f / 5f

            CanvasSizeBottomSheet.CanvasSize.STORY ->
                9f / 16f
        }

        val availableWidth = binding.editorCanvasCard.width
        val availableHeight = binding.editorCanvasCard.height

        if (availableWidth <= 0 || availableHeight <= 0) {
            return
        }

        var canvasWidth = availableWidth

        var canvasHeight =
            (canvasWidth / aspectRatio).toInt()

        if (canvasHeight > availableHeight) {

            canvasHeight = availableHeight

            canvasWidth =
                (canvasHeight * aspectRatio).toInt()
        }

        appliedCanvasWidthPx = canvasWidth
        appliedCanvasHeightPx = canvasHeight

        val params =
            binding.previewCanvas.layoutParams

        params.width = canvasWidth
        params.height = canvasHeight

        binding.previewCanvas.layoutParams = params

        binding.previewCanvas.requestLayout()

        /*
         * Wait until the new canvas dimensions have
         * actually been applied.
         */
        binding.previewCanvas.post {

            // -------------------------------------------------
            // REFIT BACKGROUND
            // -------------------------------------------------

            if (backgroundBitmap != null) {

                binding.imgBackground.resetTransform()

                fitBackgroundToPreview()
            }

            // -------------------------------------------------
            // REFIT FOREGROUND
            // -------------------------------------------------

            binding.imgForeground.resetTransform()

            fitForegroundToPreview()
        }
    }
    private fun getExportSize(): Pair<Int, Int> {

        return when (selectedCanvasSize) {

            CanvasSizeBottomSheet.CanvasSize.SQUARE ->
                1080 to 1080

            CanvasSizeBottomSheet.CanvasSize.VERTICAL ->
                1080 to 1350

            CanvasSizeBottomSheet.CanvasSize.STORY ->
                1080 to 1920
        }
    }

    private fun selectText(textView: TextView) {

        // Remove previous selection
        selectedTextView?.background = null

        deleteButton?.let {
            binding.previewCanvas.removeView(it)
        }

        deleteButton = null

        resizeButton?.let {
            binding.previewCanvas.removeView(it)
        }

        resizeButton = null

        rotateButton?.let {
            binding.previewCanvas.removeView(it)
        }

        rotateButton = null

        pencilButton?.let {
            binding.previewCanvas.removeView(it)
        }

        pencilButton = null

        // Select current text
        selectedTextView = textView

        // Selection border
        val selectionBackground = GradientDrawable().apply {

            setColor(Color.TRANSPARENT)

            setStroke(
                dpToPx(1.5f),
                Color.rgb(135, 69, 209)
            )

            cornerRadius = dpToPx(6f).toFloat()
        }

        textView.background = selectionBackground

        // Create delete button
        val delete = TextView(requireContext()).apply {

            text = "×"

            setTextColor(Color.WHITE)

            textSize = 18f

            gravity = Gravity.CENTER

            setBackground(
                GradientDrawable().apply {

                    shape = GradientDrawable.OVAL

                    setColor(
                        Color.rgb(232, 82, 102)
                    )
                }
            )

            elevation = dpToPx(6f).toFloat()

            setOnClickListener {

                deleteSelectedText()
            }
        }

        deleteButton = delete

        val size = dpToPx(28f)

        val params =
            FrameLayout.LayoutParams(
                size,
                size
            )

        binding.previewCanvas.addView(
            delete,
            params
        )

        // Make sure it appears above the text
        delete.bringToFront()

        // Position it after layout is completed
        delete.post {

            positionDeleteButton(textView)
        }

        // Create resize button
        val resize = TextView(requireContext()).apply {

            text = "↘"

            setTextColor(Color.WHITE)

            textSize = 16f

            gravity = Gravity.CENTER

            setBackground(
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(238, 164, 35))
                }
            )

            elevation = dpToPx(6f).toFloat()
        }

        resizeButton = resize

        val resizeSize = dpToPx(28f)

        val resizeParams =
            FrameLayout.LayoutParams(
                resizeSize,
                resizeSize
            )

        binding.previewCanvas.addView(
            resize,
            resizeParams
        )

        resize.bringToFront()

        resize.post {
            positionResizeButton(textView)
        }

        resize.setOnTouchListener { _, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    resizeStartX = event.rawX
                    resizeStartY = event.rawY

                    resizeStartTextSize =
                        textView.textSize

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        event.rawX - resizeStartX

                    val dy =
                        event.rawY - resizeStartY

                    // Use the larger movement direction
                    // to calculate the size change.
                    val movement =
                        maxOf(dx, dy)

                    val density =
                        resources.displayMetrics.density

                    val sizeChange =
                        movement / density

                    val newSize =
                        (resizeStartTextSize / density) +
                                sizeChange

                    val finalSize =
                        newSize.coerceIn(
                            12f,
                            120f
                        )

                    textView.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        finalSize
                    )

                    textView.post {

                        positionDeleteButton(
                            textView
                        )

                        positionResizeButton(
                            textView
                        )

                        positionRotateButton(
                            textView
                        )

                        positionPencilButton(
                            textView
                        )
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    saveTextPosition(textView)

                    true
                }

                else -> true
            }
        }

        // Create rotate button
        val rotate = TextView(requireContext()).apply {

            text = "⟳"

            setTextColor(Color.WHITE)

            textSize = 18f

            gravity = Gravity.CENTER

            setBackground(
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(0, 150, 136))
                }
            )

            elevation = dpToPx(6f).toFloat()
        }

        rotateButton = rotate

        val rotateSize = dpToPx(28f)

        val rotateParams =
            FrameLayout.LayoutParams(
                rotateSize,
                rotateSize
            )

        binding.previewCanvas.addView(
            rotate,
            rotateParams
        )

        rotate.bringToFront()

        rotate.post {
            positionRotateButton(textView)
        }

        rotate.setOnTouchListener { _, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    val center =
                        textViewCenterOnScreen(textView)

                    rotateStartAngle =
                        angleBetween(
                            center.first,
                            center.second,
                            event.rawX,
                            event.rawY
                        )

                    rotateStartRotation =
                        textView.rotation

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val center =
                        textViewCenterOnScreen(textView)

                    val angle =
                        angleBetween(
                            center.first,
                            center.second,
                            event.rawX,
                            event.rawY
                        )

                    textView.rotation =
                        rotateStartRotation +
                                (angle - rotateStartAngle)

                    positionDeleteButton(textView)
                    positionResizeButton(textView)
                    positionRotateButton(textView)
                    positionPencilButton(textView)

                    true
                }

                MotionEvent.ACTION_UP -> {

                    saveTextPosition(textView)

                    true
                }

                else -> true
            }
        }

        // Create pencil (format) button
        val pencil = TextView(requireContext()).apply {

            text = "✎"

            setTextColor(Color.WHITE)

            textSize = 16f

            gravity = Gravity.CENTER

            setBackground(
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(41, 128, 185))
                }
            )

            elevation = dpToPx(6f).toFloat()
        }

        pencilButton = pencil

        val pencilSize = dpToPx(28f)

        val pencilParams =
            FrameLayout.LayoutParams(
                pencilSize,
                pencilSize
            )

        binding.previewCanvas.addView(
            pencil,
            pencilParams
        )

        pencil.bringToFront()

        pencil.post {
            positionPencilButton(textView)
        }

        pencil.setOnClickListener {

            if (binding.textFormatCard.visibility == View.VISIBLE) {

                closeTextFormatToolbar()

            } else {

                openTextFormatToolbar(textView)
            }
        }

        // Keep an already-open format toolbar in sync when the
        // selected text changes (e.g. the user dragged a different
        // text layer while formatting was open).
        if (binding.textFormatCard.visibility == View.VISIBLE) {

            syncFormatToolbarToSelection(textView)
        }
    }


    private fun dpToPx(dp: Float): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).roundToInt()
    }


    // Computes where a handle button should sit so its center
    // lands on a corner of the text view's bounding box, taking
    // the text view's current rotation into account. (localDx,
    // localDy) is the corner's offset from the text view's center
    // before rotation is applied, e.g. (-width/2, -height/2) for
    // the top-left corner.
    private fun rotatedHandlePosition(
        textView: TextView,
        localDx: Float,
        localDy: Float,
        buttonSize: Int
    ): Pair<Float, Float> {

        val centerX =
            textView.x + textView.width / 2f

        val centerY =
            textView.y + textView.height / 2f

        val angle =
            Math.toRadians(textView.rotation.toDouble())

        val cos = cos(angle)
        val sin = sin(angle)

        val rotatedDx =
            (localDx * cos - localDy * sin).toFloat()

        val rotatedDy =
            (localDx * sin + localDy * cos).toFloat()

        val canvas = binding.previewCanvas

        var x =
            centerX + rotatedDx - buttonSize / 2f

        var y =
            centerY + rotatedDy - buttonSize / 2f

        // Don't allow the button to go outside the canvas.
        x = x.coerceIn(
            0f,
            (canvas.width - buttonSize).toFloat()
                .coerceAtLeast(0f)
        )

        y = y.coerceIn(
            0f,
            (canvas.height - buttonSize).toFloat()
                .coerceAtLeast(0f)
        )

        return x to y
    }


    // Screen-space center of a text view, used to measure the
    // rotate gesture's drag angle. Rotation doesn't change a
    // view's layout bounds, so this stays valid while dragging.
    private fun textViewCenterOnScreen(
        textView: TextView
    ): Pair<Float, Float> {

        val location = IntArray(2)

        textView.getLocationOnScreen(location)

        val centerX =
            location[0] + textView.width / 2f

        val centerY =
            location[1] + textView.height / 2f

        return centerX to centerY
    }


    private fun angleBetween(
        centerX: Float,
        centerY: Float,
        touchX: Float,
        touchY: Float
    ): Float {

        return Math.toDegrees(
            atan2(
                (touchY - centerY).toDouble(),
                (touchX - centerX).toDouble()
            )
        ).toFloat()
    }


    private fun positionDeleteButton(
        textView: TextView
    ) {

        val delete = deleteButton
            ?: return

        val buttonSize = delete.width

        // Put the center of the cross
        // exactly on the top-left corner
        // of the selected text.
        val (x, y) = rotatedHandlePosition(
            textView,
            -textView.width / 2f,
            -textView.height / 2f,
            buttonSize
        )

        delete.x = x
        delete.y = y

        delete.bringToFront()
    }



    private fun positionResizeButton(
        textView: TextView
    ) {

        val resize = resizeButton
            ?: return

        val buttonSize = resize.width

        val (x, y) = rotatedHandlePosition(
            textView,
            textView.width / 2f,
            textView.height / 2f,
            buttonSize
        )

        resize.x = x
        resize.y = y

        resize.bringToFront()
    }


    private fun positionRotateButton(
        textView: TextView
    ) {

        val rotate = rotateButton
            ?: return

        val buttonSize = rotate.width

        // Top-right corner of the selected text.
        val (x, y) = rotatedHandlePosition(
            textView,
            textView.width / 2f,
            -textView.height / 2f,
            buttonSize
        )

        rotate.x = x
        rotate.y = y

        rotate.bringToFront()
    }


    private fun positionPencilButton(
        textView: TextView
    ) {

        val pencil = pencilButton
            ?: return

        val buttonSize = pencil.width

        // Bottom-left corner of the selected text.
        val (x, y) = rotatedHandlePosition(
            textView,
            -textView.width / 2f,
            textView.height / 2f,
            buttonSize
        )

        pencil.x = x
        pencil.y = y

        pencil.bringToFront()
    }


    private fun clearTextSelection() {

        closeTextFormatToolbar()

        selectedTextView?.background = null

        deleteButton?.let {
            binding.previewCanvas.removeView(it)
        }

        resizeButton?.let {
            binding.previewCanvas.removeView(it)
        }

        rotateButton?.let {
            binding.previewCanvas.removeView(it)
        }

        pencilButton?.let {
            binding.previewCanvas.removeView(it)
        }

        deleteButton = null
        resizeButton = null
        pencilButton = null
        rotateButton = null
        selectedTextView = null
    }


    private fun makeTextMovable(
        textView: TextView
    ) {

        var downX = 0f
        var downY = 0f

        var startX = 0f
        var startY = 0f

        val gestureDetector =
            GestureDetector(
                requireContext(),
                object : GestureDetector.SimpleOnGestureListener() {

                    override fun onDoubleTap(
                        e: MotionEvent
                    ): Boolean {

                        // Keep the text selected
                        selectText(textView)

                        // Existing edit functionality
                        openTextEditor(textView)

                        return true
                    }
                }
            )

        textView.setOnTouchListener { view, event ->

            // Give event to double-tap detector
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    downX = event.rawX
                    downY = event.rawY

                    startX = view.x
                    startY = view.y

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    val dx =
                        event.rawX - downX

                    val dy =
                        event.rawY - downY

                    view.x =
                        startX + dx

                    view.y =
                        startY + dy

                    if (view == selectedTextView) {

                        positionDeleteButton(textView)

                        positionResizeButton(textView)

                        positionRotateButton(textView)

                        positionPencilButton(textView)
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {

                    // Select this text
                    selectText(textView)

                    // Save its new position
                    saveTextPosition(textView)

                    view.performClick()

                    true
                }

                else -> true
            }
        }
    }


    private fun openTextEditor(
        textView: TextView
    ) {

        val textItem =
            textView.tag
                    as? BackgroundEditorViewModel.TextItem
                ?: return

        // Tell the ViewModel which existing
        // text is currently being edited.
        editorViewModel.editingTextItem = textItem

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(
                R.id.fragmentContainer,
                TextEditorFragment()
            )
            .addToBackStack(null)
            .commit()
    }


    private fun createResizeButton(
        textView: TextView
    ) {

        // IMPORTANT:
        // If a resize button already exists,
        // don't create another one.
        if (resizeButton != null) {
            positionResizeButton(textView)
            return
        }

        val resize = TextView(requireContext()).apply {

            text = "↘"

            setTextColor(Color.WHITE)

            textSize = 16f

            gravity = Gravity.CENTER

            setBackground(
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(238, 164, 35))
                }
            )

            elevation = dpToPx(6f).toFloat()

            setOnTouchListener { _, event ->

                when (event.actionMasked) {

                    MotionEvent.ACTION_DOWN -> {

                        resizeStartX = event.rawX
                        resizeStartY = event.rawY

                        resizeStartTextSize =
                            textView.textSize

                        true
                    }

                    MotionEvent.ACTION_MOVE -> {

                        val dx =
                            event.rawX - resizeStartX

                        val dy =
                            event.rawY - resizeStartY

                        val movement =
                            maxOf(dx, dy)

                        val density =
                            resources.displayMetrics.density

                        val sizeChange =
                            movement / density

                        val newSize =
                            (resizeStartTextSize / density) +
                                    sizeChange

                        val finalSize =
                            newSize.coerceIn(
                                12f,
                                120f
                            )

                        textView.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP,
                            finalSize
                        )

                        // ONLY MOVE EXISTING HANDLES
                        positionDeleteButton(textView)
                        positionResizeButton(textView)

                        true
                    }

                    MotionEvent.ACTION_UP -> {

                        saveTextPosition(textView)

                        true
                    }

                    else -> true
                }
            }
        }

        resizeButton = resize

        val size = dpToPx(28f)

        val params =
            FrameLayout.LayoutParams(
                size,
                size
            )

        binding.previewCanvas.addView(
            resize,
            params
        )

        resize.bringToFront()

        resize.post {
            positionResizeButton(textView)
        }
    }


    private fun saveTextPosition(
        textView: TextView
    ) {

        val textItem =
            textView.tag
                    as? BackgroundEditorViewModel.TextItem
                ?: return

        textItem.x = textView.x
        textItem.y = textView.y
        textItem.rotation = textView.rotation
    }


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}