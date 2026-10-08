package com.rb.background.features.headshot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rb.background.R
import com.rb.background.databinding.FragmentStyleSelectionBinding

class StyleSelectionFragment : Fragment() {

    private var _binding: FragmentStyleSelectionBinding? = null
    private val binding get() = _binding!!

    private var selectedStyle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedStyle =
            arguments?.getString(ARG_SELECTED_STYLE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentStyleSelectionBinding.inflate(
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

        binding.cardProfessional.setOnClickListener {
            selectStyle("Professional")
        }

        binding.cardExecutive.setOnClickListener {
            selectStyle("Executive")
        }

        binding.cardModern.setOnClickListener {
            selectStyle("Modern")
        }

        binding.cardClassic.setOnClickListener {
            selectStyle("Classic")
        }
        updateSelectedStyle()
    }

    private fun selectStyle(style: String) {

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(
                    RESULT_STYLE,
                    style
                )
            }
        )

        parentFragmentManager.popBackStack()
    }

    private fun updateSelectedStyle() {

        binding.cardProfessional.isChecked =
            selectedStyle == "Professional"

        binding.cardExecutive.isChecked =
            selectedStyle == "Executive"

        binding.cardModern.isChecked =
            selectedStyle == "Modern"

        binding.cardClassic.isChecked =
            selectedStyle == "Classic"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val REQUEST_KEY = "style_selection_request"
        const val RESULT_STYLE = "selected_style"

        private const val ARG_SELECTED_STYLE = "selected_style"

        fun newInstance(
            currentStyle: String = ""
        ): StyleSelectionFragment {

            return StyleSelectionFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_SELECTED_STYLE,
                        currentStyle
                    )
                }
            }
        }
    }
}