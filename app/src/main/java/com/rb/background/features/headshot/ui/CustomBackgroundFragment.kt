package com.rb.background.features.headshot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rb.background.databinding.FragmentCustomBackgroundBinding

class CustomBackgroundFragment : Fragment() {

    private var _binding: FragmentCustomBackgroundBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCustomBackgroundBinding.inflate(
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

        binding.chipSchool.setOnClickListener {
            binding.etBackground.setText(
                "Modern school classroom"
            )
        }

        binding.chipLaboratory.setOnClickListener {
            binding.etBackground.setText(
                "Professional medical laboratory"
            )
        }

        binding.chipGarden.setOnClickListener {
            binding.etBackground.setText(
                "Beautiful garden with natural lighting"
            )
        }

        binding.btnUseBackground.setOnClickListener {

            val background =
                binding.etBackground.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            if (background.isEmpty()) {
                binding.etBackground.error =
                    "Describe your background"
                return@setOnClickListener
            }

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(
                        RESULT_BACKGROUND,
                        background
                    )
                }
            )

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val REQUEST_KEY =
            "custom_background_request"

        const val RESULT_BACKGROUND =
            "custom_background"
    }
}