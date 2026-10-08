package com.rb.background.features.headshot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rb.background.R
import com.rb.background.databinding.FragmentProfessionSelectionBinding
import com.rb.background.features.templates.model.Photo

class ProfessionSelectionFragment : Fragment() {

    private var _binding: FragmentProfessionSelectionBinding? = null
    private val binding get() = _binding!!

    private var presetImageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presetImageUri =
            arguments?.getString(ARG_PRESET_IMAGE_URI)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentProfessionSelectionBinding.inflate(
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

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cardDoctor.setOnClickListener {
            openTemplateSelection("Doctor")
        }

        binding.cardLawyer.setOnClickListener {
            openTemplateSelection("Lawyer")
        }

        binding.cardEngineer.setOnClickListener {
            openTemplateSelection("Engineer")
        }

        binding.cardBusiness.setOnClickListener {
            openTemplateSelection("Business")
        }

        binding.cardTeacher.setOnClickListener {
            openTemplateSelection("Teacher")
        }

        binding.cardDesigner.setOnClickListener {
            openTemplateSelection("Designer")
        }

        binding.cardDeveloper.setOnClickListener {
            openTemplateSelection("Developer")
        }

        binding.cardPhotographer.setOnClickListener {
            openTemplateSelection("Photographer")
        }
    }

    private fun openTemplateSelection(
        profession: String
    ) {

        val templateSheet =
            HeadshotTemplateBottomSheet()

        templateSheet.setProfession(
            profession
        )

        templateSheet.setOnTemplateSelectedListener { photo, templateGender ->

            openUploadScreen(
                profession = profession,
                photo = photo,
                templateGender = templateGender
            )
        }

        templateSheet.show(
            childFragmentManager,
            "HeadshotTemplateBottomSheet"
        )
    }

    private fun openUploadScreen(
        profession: String,
        photo: Photo,
        templateGender: String
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
                HeadshotUploadFragment.newInstance(
                    profession = profession,
                    templateUrl = photo.src.large2x,
                    templateGender = templateGender,
                    presetImageUri = presetImageUri
                )
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_PRESET_IMAGE_URI = "preset_image_uri"

        fun newInstance(
            presetImageUri: String? = null
        ): ProfessionSelectionFragment {

            return ProfessionSelectionFragment().apply {

                arguments = Bundle().apply {

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
}