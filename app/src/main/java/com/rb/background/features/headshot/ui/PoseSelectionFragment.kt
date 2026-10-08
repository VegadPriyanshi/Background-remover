package com.rb.background.features.headshot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rb.background.databinding.FragmentPoseSelectionBinding

class PoseSelectionFragment : Fragment() {

    private var _binding: FragmentPoseSelectionBinding? = null
    private val binding get() = _binding!!

    private var selectedPose: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedPose =
            arguments?.getString(ARG_SELECTED_POSE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPoseSelectionBinding.inflate(
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

        binding.cardConfident.setOnClickListener {
            selectPose("Confident Standing")
        }

        binding.cardArmsCrossed.setOnClickListener {
            selectPose("Arms Crossed")
        }

        binding.cardRelaxed.setOnClickListener {
            selectPose("Relaxed Portrait")
        }

        binding.cardSeated.setOnClickListener {
            selectPose("Seated Professional")
        }

        updateSelectedPose()
    }

    private fun selectPose(pose: String) {

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(
                    RESULT_POSE,
                    pose
                )
            }
        )

        parentFragmentManager.popBackStack()
    }

    private fun updateSelectedPose() {

        binding.cardConfident.isChecked =
            selectedPose == "Confident Standing"

        binding.cardArmsCrossed.isChecked =
            selectedPose == "Arms Crossed"

        binding.cardRelaxed.isChecked =
            selectedPose == "Relaxed Portrait"

        binding.cardSeated.isChecked =
            selectedPose == "Seated Professional"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val REQUEST_KEY = "pose_selection_request"
        const val RESULT_POSE = "selected_pose"

        private const val ARG_SELECTED_POSE = "selected_pose"

        fun newInstance(
            currentPose: String = ""
        ): PoseSelectionFragment {

            return PoseSelectionFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_SELECTED_POSE,
                        currentPose
                    )
                }
            }
        }
    }
}