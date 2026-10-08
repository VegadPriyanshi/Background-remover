package com.rb.background.features.backgroundremover.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.background.databinding.BottomSheetBackgroundOptionsBinding
import com.rb.background.features.backgroundremover.bottomsheet.BackgroundOptionsBottomSheet

class BackgroundOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBackgroundOptionsBinding? = null
    private val binding get() = _binding!!
    private var onSolidColorClick: (() -> Unit)? = null

    private var onTemplatesClick: (() -> Unit)? = null

    fun setOnTemplatesClickListener(
        listener: () -> Unit
    ) {
        onTemplatesClick = listener
    }

    fun setOnSolidColorClickListener(listener: () -> Unit) {
        onSolidColorClick = listener
    }



    private var onGalleryClick: (() -> Unit)? = null

    fun setOnGalleryClickListener(listener: () -> Unit) {
        onGalleryClick = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            BottomSheetBackgroundOptionsBinding.inflate(
                inflater,
                container,
                false
            )


        binding.btnSolidColor.setOnClickListener {

            dismiss()

            onSolidColorClick?.invoke()

        }

        binding.btnGallery.setOnClickListener {

            dismiss()

            onGalleryClick?.invoke()

        }
        binding.btnTemplates.setOnClickListener {

            dismiss()

            onTemplatesClick?.invoke()

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

