package com.rb.background.features.backgroundremover.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.rb.background.R
import com.rb.background.databinding.FragmentBackgroundResultBinding
import com.rb.background.features.backgroundremover.editor.ImageSaver
import java.io.File

class BackgroundResultFragment : Fragment() {

    private var _binding: FragmentBackgroundResultBinding? = null
    private val binding get() = _binding!!

    private val editorViewModel: BackgroundEditorViewModel by activityViewModels()

    private var pendingShareClear = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBackgroundResultBinding.inflate(
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

        binding.btnBack.setOnClickListener {

            goToHome()
        }

        val bitmap = editorViewModel.resultBitmap

        if (bitmap == null) {

            Toast.makeText(
                requireContext(),
                "Image is not available.",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()

            return
        }

        binding.imgPreview.setImageBitmap(bitmap)

        binding.btnSave.setOnClickListener {

            saveImage()
        }

        binding.btnShare.setOnClickListener {

            shareImage()
        }
    }

    private fun saveImage() {

        val bitmap = editorViewModel.resultBitmap
            ?: return

        binding.btnSave.isEnabled = false

        val saved = ImageSaver.saveBitmap(
            requireContext(),
            bitmap
        )

        binding.btnSave.isEnabled = true

        if (saved) {

            // Clear the final preview after successful save
            binding.imgPreview.setImageDrawable(null)

            // Clear the stored result as well
            editorViewModel.resultBitmap = null

            Toast.makeText(
                requireContext(),
                "Image saved successfully!",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            Toast.makeText(
                requireContext(),
                "Failed to save image.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareImage() {

        val bitmap = editorViewModel.resultBitmap
            ?: return

        try {

            val file = File(
                requireContext().cacheDir,
                "shared_background_result.png"
            )

            file.outputStream().use { outputStream ->

                bitmap.compress(
                    android.graphics.Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(
                Intent.ACTION_SEND
            ).apply {

                type = "image/png"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share customized photo"
                )
            )

            // Clear only once the share sheet has actually taken the foreground
            pendingShareClear = true

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Unable to share image.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun goToHome() {

        parentFragmentManager.popBackStack(
            BackgroundRemoverFragment.BACKSTACK_REMOVE_BACKGROUND_FLOW,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    override fun onStop() {

        super.onStop()

        if (pendingShareClear) {

            pendingShareClear = false

            binding.imgPreview.setImageDrawable(null)
            editorViewModel.resultBitmap = null
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()
        _binding = null
    }
}