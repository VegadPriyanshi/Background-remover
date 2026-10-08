package com.rb.background.features.home.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.background.databinding.BottomSheetQuickToolPickerBinding

class QuickToolPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetQuickToolPickerBinding? = null
    private val binding get() = _binding!!

    private var onRemoveBackgroundClick: (() -> Unit)? = null
    private var onAiHeadshotClick: (() -> Unit)? = null

    fun setOnRemoveBackgroundClickListener(
        listener: () -> Unit
    ) {
        onRemoveBackgroundClick = listener
    }

    fun setOnAiHeadshotClickListener(
        listener: () -> Unit
    ) {
        onAiHeadshotClick = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            BottomSheetQuickToolPickerBinding.inflate(
                inflater,
                container,
                false
            )

        binding.btnRemoveBackground.setOnClickListener {

            dismiss()

            onRemoveBackgroundClick?.invoke()
        }

        binding.btnAiHeadshot.setOnClickListener {

            dismiss()

            onAiHeadshotClick?.invoke()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
