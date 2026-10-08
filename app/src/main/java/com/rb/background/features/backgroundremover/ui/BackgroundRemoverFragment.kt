package com.rb.background.features.backgroundremover.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rb.background.databinding.FragmentBackgroundRemoverBinding
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.Color
import com.rb.background.features.backgroundremover.ml.SubjectSegmentationManager
import com.rb.background.features.backgroundremover.utils.ImageUtils
import com.rb.background.features.backgroundremover.editor.BitmapEditor
import com.rb.background.features.backgroundremover.editor.CheckerboardGenerator
import com.rb.background.features.backgroundremover.editor.ImageSaver
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.rb.background.R
import com.rb.background.features.backgroundremover.bottomsheet.BackgroundOptionsBottomSheet
import com.rb.background.features.backgroundremover.editor.BackgroundReplacer
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
import com.skydoves.colorpickerview.ColorPickerDialog
import com.rb.background.features.backgroundremover.bottomsheet.ColorPickerBottomSheet
import com.rb.background.features.backgroundremover.utils.ImageDownloader
import com.rb.background.features.templates.ui.TemplateBottomSheet

class BackgroundRemoverFragment : Fragment() {


    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        // Marks the flow's first back stack entry so the result screen can pop straight to Home
        const val BACKSTACK_REMOVE_BACKGROUND_FLOW = "remove_background_flow"

        fun newInstance(imageUri: String): BackgroundRemoverFragment {
            return BackgroundRemoverFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URI, imageUri)
                }
            }
        }
    }

    private var _binding: FragmentBackgroundRemoverBinding? = null
    private val binding get() = _binding!!

    private val editorViewModel: BackgroundEditorViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null
    private var editableBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    private var selectedBackgroundColor = Color.WHITE

    private val segmentationManager = SubjectSegmentationManager()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBackgroundRemoverBinding.inflate(inflater, container, false)

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        binding.imgPreview.post {

            binding.imgPreview.background =
                CheckerboardGenerator.create(
                    resources = resources,
                    width = binding.imgPreview.width,
                    height = binding.imgPreview.height
                )

        }

        arguments?.getString(ARG_IMAGE_URI)?.let { uriString ->

            selectedImageUri = Uri.parse(uriString)

            selectedBitmap = ImageUtils.uriToBitmap(
                requireContext(),
                selectedImageUri!!
            )

            binding.imgPreview.setImageBitmap(selectedBitmap)
        }

        binding.btnDone.setOnClickListener {

            val bitmap = selectedBitmap

            if (bitmap == null) {
                Toast.makeText(
                    requireContext(),
                    "Please select an image first.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            removeBackground(bitmap)
        }



        return binding.root
    }

    private fun removeBackground(bitmap: Bitmap) {

        binding.btnDone.isEnabled = false
        binding.btnDone.text = "✂️ Removing Background..."

        // This is the start of a brand new editing session, so drop
        // anything left over from a previous photo (text layers,
        // background choice, etc.) before loading the new one in.
        editorViewModel.clear()

        editorViewModel.selectedBitmap = bitmap

        segmentationManager
            .segmentImage(bitmap)
            .addOnSuccessListener { result ->

                android.util.Log.d(
                    "BackgroundRemover",
                    "Subject detected successfully!"
                )

                val foregroundBitmap = result.foregroundBitmap

                if (foregroundBitmap != null) {

                    // Reuse your existing BitmapEditor functionality
                    val editableBitmap =
                        BitmapEditor.createEditableBitmap(
                            foregroundBitmap
                        )

                    // Store the background-removed image
                    editorViewModel.editableBitmap = editableBitmap

                    // Also keep it as the current result
                    editorViewModel.resultBitmap = editableBitmap

                    binding.btnDone.isEnabled = true
                    binding.btnDone.text = "✂️ Remove Background"

                    // Open the next screen
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                        )
                        .replace(
                            R.id.fragmentContainer,
                            BackgroundPreviewFragment()
                        )
                        .addToBackStack(null)
                        .commit()

                } else {

                    binding.btnDone.isEnabled = true
                    binding.btnDone.text = "✂️ Remove Background"

                    Toast.makeText(
                        requireContext(),
                        "Could not remove background.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { exception ->

                binding.btnDone.isEnabled = true
                binding.btnDone.text = "✂️ Remove Background"

                android.util.Log.e(
                    "BackgroundRemover",
                    exception.message ?: "Unknown Error"
                )

                Toast.makeText(
                    requireContext(),
                    "Failed to remove background.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            uri?.let {

                selectedImageUri = it

                selectedBitmap = ImageUtils.uriToBitmap(
                    requireContext(),
                    it
                )

                binding.imgPreview.setImageBitmap(selectedBitmap)

                selectedBitmap?.let { bitmap ->

                    segmentationManager
                        .segmentImage(bitmap)
                        .addOnSuccessListener { result ->

                            android.util.Log.d(
                                "BackgroundRemover",
                                "Subject detected successfully!"
                            )


                            val originalBitmap = selectedBitmap ?: return@addOnSuccessListener

                            val foregroundBitmap = result.foregroundBitmap

                            if (foregroundBitmap != null) {

                                editableBitmap =
                                    BitmapEditor.createEditableBitmap(
                                        foregroundBitmap
                                    )

                                val finalBitmap =
                                    BackgroundReplacer.replaceWithColor(
                                        foregroundBitmap = editableBitmap!!,
                                        backgroundColor = selectedBackgroundColor
                                    )

                                resultBitmap = finalBitmap

                                binding.imgPreview.setImageBitmap(finalBitmap)
                            } else {

                                android.util.Log.e(
                                    "BackgroundRemover",
                                    "Foreground bitmap is null"
                                )

                            }
                        }
                        .addOnFailureListener { exception ->

                            android.util.Log.e(
                                "BackgroundRemover",
                                exception.message ?: "Unknown Error"
                            )

                        }

                }

            }
        }
    private val backgroundPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            uri ?: return@registerForActivityResult

            backgroundBitmap = ImageUtils.uriToBitmap(
                requireContext(),
                uri
            )

            // Make sure we already have the segmented foreground
            val foreground = editableBitmap

            if (foreground == null || backgroundBitmap == null) {

                Toast.makeText(
                    requireContext(),
                    "Please remove the background first.",
                    Toast.LENGTH_SHORT
                ).show()

                return@registerForActivityResult
            }

            resultBitmap =
                BackgroundReplacer.replaceWithBitmap(
                    foregroundBitmap = foreground,
                    backgroundBitmap = backgroundBitmap!!
                )

            binding.imgPreview.setImageBitmap(resultBitmap)

            Toast.makeText(
                requireContext(),
                "Background applied successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }

    private fun applyBackgroundColor(color: Int) {

        val bitmap = editableBitmap ?: return

        selectedBackgroundColor = color

        resultBitmap =
            BackgroundReplacer.replaceWithColor(
                foregroundBitmap = bitmap,
                backgroundColor = color
            )

        binding.imgPreview.setImageBitmap(resultBitmap)
    }

    private fun resetEditor() {

        // Clear all image data
        selectedImageUri = null
        selectedBitmap = null
        editableBitmap = null
        backgroundBitmap = null
        resultBitmap = null

        selectedBackgroundColor = Color.WHITE

        // Remove image
        binding.imgPreview.setImageBitmap(null)

        // Restore checkerboard background
        binding.imgPreview.background =
            CheckerboardGenerator.create(
                resources = resources,
                binding.imgPreview.width,
                binding.imgPreview.height
            )

    }

    private fun showColorPicker() {

        ColorPickerDialog.Builder(requireContext())
            .setTitle("Choose Background Color")

            .setPositiveButton(
                "Select",
                ColorEnvelopeListener { envelope: ColorEnvelope, _ ->

                    applyBackgroundColor(envelope.color)

                }
            )

            .setNegativeButton(
                "Cancel"
            ) { dialogInterface, _ ->

                dialogInterface.dismiss()

            }

            .attachBrightnessSlideBar(true)

            .show()

    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}