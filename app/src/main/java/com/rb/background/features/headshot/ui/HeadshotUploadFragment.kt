package com.rb.background.features.headshot.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.rb.background.R
import com.rb.background.databinding.FragmentHeadshotUploadBinding
import com.rb.background.features.backgroundremover.utils.ImageUtils
import java.io.File

class HeadshotUploadFragment : Fragment() {

    private var _binding: FragmentHeadshotUploadBinding? = null
    private val binding get() = _binding!!

    private var selectedProfession: String = ""
    private var selectedTemplateUrl: String = ""
    private var selectedTemplateGender: String = ""
    private var presetImageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedProfession =
            arguments?.getString(ARG_PROFESSION).orEmpty()

        selectedTemplateUrl =
            arguments?.getString(ARG_TEMPLATE_URL).orEmpty()

        selectedTemplateGender =
            arguments?.getString(ARG_TEMPLATE_GENDER).orEmpty()

        presetImageUri =
            arguments?.getString(ARG_PRESET_IMAGE_URI)
    }

    // ---------------------------------------------------------
    // Face Detector
    // ---------------------------------------------------------

    private val faceDetector by lazy {

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(
                FaceDetectorOptions.PERFORMANCE_MODE_FAST
            )
            .setLandmarkMode(
                FaceDetectorOptions.LANDMARK_MODE_NONE
            )
            .setClassificationMode(
                FaceDetectorOptions.CLASSIFICATION_MODE_NONE
            )
            .build()

        FaceDetection.getClient(options)
    }

    // ---------------------------------------------------------
    // Selected Image
    // ---------------------------------------------------------

    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var cameraImageUri: Uri? = null

    // ---------------------------------------------------------
    // Gallery
    // ---------------------------------------------------------

    private val galleryLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri ?: return@registerForActivityResult

            applySelectedPhoto(uri)
        }

    // ---------------------------------------------------------
    // Camera
    // ---------------------------------------------------------

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                cameraImageUri?.let { uri ->

                    applySelectedPhoto(uri)
                }
            }
        }

    private fun applySelectedPhoto(uri: Uri) {

        selectedImageUri = uri

        binding.imgPreview.setImageURI(uri)

        binding.imgPreview.visibility = View.VISIBLE
        binding.layoutUpload.visibility = View.GONE

        val bitmap = ImageUtils.uriToBitmap(
            requireContext(),
            uri
        )

        if (bitmap != null) {
            selectedBitmap = bitmap
            detectFace(bitmap)
        }
    }

    // ---------------------------------------------------------
    // Face Detection
    // ---------------------------------------------------------

    private fun detectFace(bitmap: Bitmap) {

        val image = InputImage.fromBitmap(
            bitmap,
            0
        )

        // Disable until detection is successfully completed.
        binding.btnGenerate.isEnabled = false

        faceDetector.process(image)
            .addOnSuccessListener { faces ->

                when {

                    // -----------------------------------------
                    // No face
                    // -----------------------------------------

                    faces.isEmpty() -> {

                        Toast.makeText(
                            requireContext(),
                            "No face detected. Please choose a clear face photo.",
                            Toast.LENGTH_LONG
                        ).show()

                        binding.btnGenerate.isEnabled = false
                    }

                    // -----------------------------------------
                    // Multiple faces
                    // -----------------------------------------

                    faces.size > 1 -> {

                        Toast.makeText(
                            requireContext(),
                            "Please upload another photo that contains only one person.",
                            Toast.LENGTH_LONG
                        ).show()

                        binding.btnGenerate.isEnabled = false
                    }

                    // -----------------------------------------
                    // Exactly one face
                    // -----------------------------------------

                    else -> {

                        Toast.makeText(
                            requireContext(),
                            "Face detected successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        confirmPhotoMatchesTemplate()
                    }
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    requireContext(),
                    "Face detection failed. Please try another photo.",
                    Toast.LENGTH_SHORT
                ).show()

                binding.btnGenerate.isEnabled = false
            }
    }

    // There's no automatic way to detect gender from the photo,
    // so we ask the user to confirm it and check it against the
    // gender they confirmed for the template — mismatches are
    // blocked here instead of wasting a generation attempt.
    private fun confirmPhotoMatchesTemplate() {

        if (selectedTemplateGender.isBlank()) {

            binding.btnGenerate.isEnabled = true

            return
        }

        binding.btnGenerate.isEnabled = false

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Your Photo")
            .setMessage(
                "This template features a " +
                        "$selectedTemplateGender. " +
                        "Who is shown in your uploaded photo?"
            )
            .setPositiveButton("Woman") { dialog, _ ->

                dialog.dismiss()

                onPhotoGenderConfirmed("Woman")
            }
            .setNegativeButton("Man") { dialog, _ ->

                dialog.dismiss()

                onPhotoGenderConfirmed("Man")
            }
            .setCancelable(false)
            .show()
    }

    private fun onPhotoGenderConfirmed(
        photoGender: String
    ) {

        if (photoGender == selectedTemplateGender) {

            binding.btnGenerate.isEnabled = true

        } else {

            binding.btnGenerate.isEnabled = false

            AlertDialog.Builder(requireContext())
                .setTitle("Photo Doesn't Match Template")
                .setMessage(
                    "This template features a " +
                            "$selectedTemplateGender. " +
                            "Please upload a photo of a " +
                            "$selectedTemplateGender, or go back " +
                            "and choose a different template."
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // ---------------------------------------------------------
    // Create View
    // ---------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHeadshotUploadBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    // ---------------------------------------------------------
    // View Created
    // ---------------------------------------------------------

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        // Back
        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        // Gallery
        binding.btnGallery.setOnClickListener {

            galleryLauncher.launch("image/*")
        }

        // Camera
        binding.btnCamera.setOnClickListener {

            val photoFile = File.createTempFile(
                "headshot_",
                ".jpg",
                requireContext().cacheDir
            )

            cameraImageUri =
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )

            cameraLauncher.launch(
                cameraImageUri
            )
        }

        // Pre-fill the photo picked from the Home screen's
        // "Upload Photo" card, if any, instead of making the
        // user pick it again here.
        presetImageUri?.let { uriString ->

            applySelectedPhoto(Uri.parse(uriString))
        }

        // Generate
        binding.btnGenerate.setOnClickListener {

            val imageUri = selectedImageUri

            if (imageUri == null) {

                Toast.makeText(
                    requireContext(),
                    "Please select a photo first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (selectedTemplateUrl.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Please select a headshot template first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(
                    R.id.fragmentContainer,
                    GeneratingHeadshotFragment.newInstance(
                        imageUri = imageUri.toString(),
                        profession = selectedProfession,
                        templateUrl = selectedTemplateUrl
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    // ---------------------------------------------------------
    // Arguments
    // ---------------------------------------------------------

    companion object {

        private const val ARG_PROFESSION =
            "profession"

        private const val ARG_TEMPLATE_URL =
            "template_url"

        private const val ARG_TEMPLATE_GENDER =
            "template_gender"

        private const val ARG_PRESET_IMAGE_URI =
            "preset_image_uri"

        fun newInstance(
            profession: String,
            templateUrl: String,
            templateGender: String,
            presetImageUri: String? = null
        ): HeadshotUploadFragment {

            return HeadshotUploadFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_PROFESSION,
                        profession
                    )

                    putString(
                        ARG_TEMPLATE_URL,
                        templateUrl
                    )

                    putString(
                        ARG_TEMPLATE_GENDER,
                        templateGender
                    )

                    presetImageUri?.let {

                        putString(
                            ARG_PRESET_IMAGE_URI,
                            it
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Destroy
    // ---------------------------------------------------------

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}