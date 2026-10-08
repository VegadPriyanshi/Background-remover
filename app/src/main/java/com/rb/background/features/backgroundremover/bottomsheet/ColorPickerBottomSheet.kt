package com.rb.background.features.backgroundremover.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.background.databinding.BottomSheetColorPickerBinding
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class ColorPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetColorPickerBinding? = null
    private val binding get() = _binding!!

    private var selectedColor: Int = android.graphics.Color.WHITE

    private var initialColor: Int = android.graphics.Color.WHITE

    private var onColorSelected: ((Int) -> Unit)? = null

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        onColorSelected = listener
    }

    fun setInitialColor(color: Int) {
        initialColor = color
        selectedColor = color
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetColorPickerBinding.inflate(
            inflater,
            container,
            false
        )

        binding.colorPickerView.attachBrightnessSlider(binding.brightnessSlider)
        binding.colorPickerView.setColorListener(
            ColorEnvelopeListener { envelope, fromUser ->

                if (!fromUser) return@ColorEnvelopeListener

                selectedColor = envelope.color

                binding.viewSelectedColor.setBackgroundColor(envelope.color)

                binding.tvHex.text = envelope.hexCode

                onColorSelected?.invoke(envelope.color)

            }
        )

        binding.btnApply.setOnClickListener {

            dismiss()

        }
        binding.btnCancel.setOnClickListener {

            onColorSelected?.invoke(initialColor)

            dismiss()

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}