package com.rb.background.features.home.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.rb.background.R
import com.rb.background.databinding.FragmentHomeBinding
import com.rb.background.features.headshot.ui.ProfessionSelectionFragment
import com.rb.background.features.home.adapter.QuickToolsAdapter
import com.rb.background.features.home.bottomsheet.QuickToolPickerBottomSheet
import com.rb.background.features.home.models.QuickToolAction
import com.rb.background.features.home.repository.QuickToolsRepository
import android.widget.Toast
import com.rb.background.features.backgroundremover.ui.BackgroundRemoverFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /**
     * Gallery launcher for Remove Background.
     *
     * User selects one image from the gallery.
     */
    private val removeBackgroundImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {
                // Step 1 only:
                // Pass the selected image to the next Background Remover screen.
                openBackgroundRemover(it)
            }
        }

    /**
     * Gallery launcher for the hero card's "Upload Photo" button.
     *
     * User selects a photo first, then picks which tool to use it with.
     */
    private val uploadPhotoLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {
                showQuickToolPicker(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUploadPhoto.setOnClickListener {

            uploadPhotoLauncher.launch("image/*")
        }

        setupRecyclerView()
    }

    private fun showQuickToolPicker(
        imageUri: Uri
    ) {

        val bottomSheet = QuickToolPickerBottomSheet()

        bottomSheet.setOnRemoveBackgroundClickListener {

            openBackgroundRemover(imageUri)
        }

        bottomSheet.setOnAiHeadshotClickListener {

            openAiHeadshot(imageUri)
        }

        bottomSheet.show(
            childFragmentManager,
            "QuickToolPickerBottomSheet"
        )
    }

    private fun openBackgroundRemover(
        imageUri: Uri
    ) {

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(
                R.id.fragmentContainer,
                BackgroundRemoverFragment.newInstance(imageUri.toString())
            )
            .addToBackStack(BackgroundRemoverFragment.BACKSTACK_REMOVE_BACKGROUND_FLOW)
            .commit()
    }

    private fun openAiHeadshot(
        imageUri: Uri? = null
    ) {

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(
                R.id.fragmentContainer,
                ProfessionSelectionFragment.newInstance(
                    presetImageUri = imageUri?.toString()
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun setupRecyclerView() {

        val quickTools = QuickToolsRepository.getQuickTools()

        val adapter = QuickToolsAdapter(quickTools) { quickTool ->

            when (quickTool.action) {

                QuickToolAction.REMOVE_BACKGROUND -> {

                    // Open gallery directly.
                    removeBackgroundImageLauncher.launch("image/*")
                }

                QuickToolAction.AI_HEADSHOT -> {

                    openAiHeadshot()
                }

                QuickToolAction.PASSPORT_PHOTO,
                QuickToolAction.TEMPLATES -> {

                    Toast.makeText(
                        requireContext(),
                        "Coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.rvQuickTools.layoutManager =
            GridLayoutManager(requireContext(), 2)

        binding.rvQuickTools.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}