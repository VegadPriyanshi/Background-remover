package com.rb.background.features.backgroundremover.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rb.background.databinding.FragmentTextEditorBinding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TextEditorFragment : Fragment() {

    private var _binding: FragmentTextEditorBinding? = null
    private val binding get() = _binding!!
    private val editorViewModel: BackgroundEditorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTextEditorBinding.inflate(
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



        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->

            val imeInsets =
                insets.getInsets(WindowInsetsCompat.Type.ime())

            // Push the bottom of the layout up by the keyboard height so
            // bottomActions (Cancel/Done) stays laid out right above the
            // keyboard instead of being drawn underneath it.
            root.setPadding(
                root.paddingLeft,
                root.paddingTop,
                root.paddingRight,
                imeInsets.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(binding.root)


        val editingItem =
            editorViewModel.editingTextItem


        if (editingItem != null) {

            binding.etText.setText(
                editingItem.text
            )

            binding.etText.setSelection(
                binding.etText.text.length
            )
        }

        // ---------------------------------------------
        // CANCEL
        // ---------------------------------------------

        binding.btnCancel.setOnClickListener {

            parentFragmentManager.popBackStack()
        }


        // ---------------------------------------------
        // DONE
        // ---------------------------------------------

        binding.btnDone.setOnClickListener {

            val text = binding.etText.text
                ?.toString()
                ?.trim()
                .orEmpty()

            if (text.isEmpty()) {

                binding.etText.error = "Enter text"

                return@setOnClickListener
            }

            val editingItem =
                editorViewModel.editingTextItem

            if (editingItem != null) {

                // -----------------------------------------
                // EDIT EXISTING TEXT
                // -----------------------------------------

                editingItem.text = text

                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {

                        putString(
                            RESULT_TEXT,
                            text
                        )

                        putBoolean(
                            RESULT_IS_EDIT,
                            true
                        )
                    }
                )

            } else {

                // -----------------------------------------
                // ADD NEW TEXT
                // -----------------------------------------

                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {

                        putString(
                            RESULT_TEXT,
                            text
                        )

                        putBoolean(
                            RESULT_IS_EDIT,
                            false
                        )
                    }
                )
            }

            // IMPORTANT:
            // Do not clear editingTextItem here.
            // BackgroundCustomizeFragment still needs it
            // to identify the TextView being edited.

            parentFragmentManager.popBackStack()
        }



    }

    override fun onDestroyView() {



        super.onDestroyView()

        _binding = null
    }

    companion object {

        const val REQUEST_KEY =
            "text_editor_request"

        const val RESULT_TEXT =
            "text_editor_result"

        const val RESULT_IS_EDIT =
            "result_is_edit"
    }
}