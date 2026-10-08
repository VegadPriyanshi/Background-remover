package com.rb.background.features.backgroundremover.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.background.databinding.BottomSheetCanvasSizeBinding

class CanvasSizeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCanvasSizeBinding? = null
    private val binding get() = _binding!!

    private var onCanvasSizeSelected: ((CanvasSize) -> Unit)? = null

    fun setOnCanvasSizeSelectedListener(
        listener: (CanvasSize) -> Unit
    ) {
        onCanvasSizeSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetCanvasSizeBinding.inflate(
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

        binding.optionSquare.setOnClickListener {
            onCanvasSizeSelected?.invoke(CanvasSize.SQUARE)
            dismiss()
        }

        binding.optionVertical.setOnClickListener {
            onCanvasSizeSelected?.invoke(CanvasSize.VERTICAL)
            dismiss()
        }

        binding.optionStory.setOnClickListener {
            onCanvasSizeSelected?.invoke(CanvasSize.STORY)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class CanvasSize {
        SQUARE,
        VERTICAL,
        STORY
    }
}