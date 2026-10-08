package com.rb.background.features.headshot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rb.background.R
import com.rb.background.databinding.FragmentCustomizeHeadshotBinding

class CustomizeHeadshotFragment : Fragment() {

    private var _binding: FragmentCustomizeHeadshotBinding? = null
    private val binding get() = _binding!!

    private var selectedProfession: String = "Professional"

    private var selectedTemplateUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedProfession =
            arguments?.getString(ARG_PROFESSION)
                ?: "Professional"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCustomizeHeadshotBinding.inflate(
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

        // --------------------------------
        // Profession
        // --------------------------------

        binding.tvProfession.text =
            selectedProfession

        // --------------------------------
        // Back
        // --------------------------------

        binding.btnBack.setOnClickListener {

            parentFragmentManager.popBackStack()
        }

        // --------------------------------
        // Select Template
        // --------------------------------

        binding.cardTemplate.setOnClickListener {

            val bottomSheet =
                HeadshotTemplateBottomSheet()

            bottomSheet.setOnTemplateSelectedListener { photo, _ ->

                selectedTemplateUrl =
                    photo.src.medium

                binding.tvTemplateDescription.text =
                    "Template selected"

                Toast.makeText(
                    requireContext(),
                    "Template selected",
                    Toast.LENGTH_SHORT
                ).show()
            }

            bottomSheet.show(
                parentFragmentManager,
                "HeadshotTemplateBottomSheet"
            )
        }

        // --------------------------------
        // Generate Headshot
        // --------------------------------

        binding.btnGenerate.setOnClickListener {

            if (selectedTemplateUrl.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Please select a template first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    HeadshotUploadFragment.newInstance(
                        profession = selectedProfession,
                        templateUrl = selectedTemplateUrl,
                        templateGender = ""
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_PROFESSION =
            "profession"

        fun newInstance(
            profession: String
        ): CustomizeHeadshotFragment {

            return CustomizeHeadshotFragment().apply {

                arguments =
                    Bundle().apply {

                        putString(
                            ARG_PROFESSION,
                            profession
                        )
                    }
            }
        }
    }
}