package com.rb.background.features.headshot.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.rb.background.R
import com.rb.background.databinding.FragmentHeadshotResultBinding
import com.rb.background.features.backgroundremover.editor.ImageSaver
import java.io.File

class HeadshotResultFragment : Fragment() {

    private var _binding: FragmentHeadshotResultBinding? = null
    private val binding get() = _binding!!

    private var imageUri: String? = null
    private var selectedProfession: String = ""
    private var selectedTemplateUrl: String = ""

    private var pendingShareClear = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageUri =
            arguments?.getString(ARG_IMAGE_URI)

        selectedProfession =
            arguments?.getString(ARG_PROFESSION).orEmpty()

        selectedTemplateUrl =
            arguments?.getString(ARG_TEMPLATE_URL).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentHeadshotResultBinding.inflate(
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

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {

            goToHome()
        }

        // --------------------------------
        // Show generated image
        // --------------------------------

        imageUri?.let { uriString ->

            binding.imgResult.setImageURI(
                Uri.parse(uriString)
            )
        }

        // --------------------------------
        // Selection summary
        // --------------------------------

        binding.tvSelectionSummary.text =
            "$selectedProfession • Selected Template"

        // --------------------------------
        // Back
        // --------------------------------

        binding.btnBack.setOnClickListener {

            goToHome()
        }

        // --------------------------------
        // Save
        // --------------------------------

        binding.btnSave.setOnClickListener {

            val uriString = imageUri

            if (uriString.isNullOrEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Photo not available",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Glide.with(this)
                .asBitmap()
                .load(Uri.parse(uriString))
                .into(
                    object :
                        com.bumptech.glide.request.target.CustomTarget<Bitmap>() {

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition:
                            com.bumptech.glide.request.transition.Transition<in Bitmap>?
                        ) {

                            val saved =
                                ImageSaver.saveBitmap(
                                    requireContext(),
                                    resource
                                )

                            if (saved) {

                                if (_binding != null) {

                                    binding.imgResult.setImageDrawable(null)
                                }

                                imageUri = null

                                Toast.makeText(
                                    requireContext(),
                                    "Photo saved successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    requireContext(),
                                    "Failed to save photo",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onLoadCleared(
                            placeholder:
                            android.graphics.drawable.Drawable?
                        ) {
                            // Nothing to do
                        }
                    }
                )
        }

        // --------------------------------
        // Share
        // --------------------------------

        binding.btnShare.setOnClickListener {

            val uriString = imageUri

            if (uriString.isNullOrEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Photo not available",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val localFile = Uri.parse(uriString).path?.let(::File)

            if (localFile == null || !localFile.exists()) {

                Toast.makeText(
                    requireContext(),
                    "Photo not available",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                localFile
            )

            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {

                    type = "image/*"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        contentUri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share Headshot"
                )
            )

            // Clear only once the share sheet has actually taken the foreground
            pendingShareClear = true
        }

        // --------------------------------
        // Create Another
        // --------------------------------

        binding.btnCreateAnother.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(
                    R.id.fragmentContainer,
                    AIHeadshotFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun goToHome() {

        if (parentFragmentManager.backStackEntryCount > 0) {

            val firstEntryId =
                parentFragmentManager.getBackStackEntryAt(0).id

            parentFragmentManager.popBackStack(
                firstEntryId,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    override fun onStop() {

        super.onStop()

        if (pendingShareClear) {

            pendingShareClear = false

            binding.imgResult.setImageDrawable(null)
            imageUri = null
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_IMAGE_URI =
            "image_uri"

        private const val ARG_PROFESSION =
            "profession"

        private const val ARG_TEMPLATE_URL =
            "template_url"

        fun newInstance(
            imageUri: String,
            profession: String,
            templateUrl: String
        ): HeadshotResultFragment {

            return HeadshotResultFragment().apply {

                arguments =
                    Bundle().apply {

                        putString(
                            ARG_IMAGE_URI,
                            imageUri
                        )

                        putString(
                            ARG_PROFESSION,
                            profession
                        )

                        putString(
                            ARG_TEMPLATE_URL,
                            templateUrl
                        )
                    }
            }
        }
    }
}