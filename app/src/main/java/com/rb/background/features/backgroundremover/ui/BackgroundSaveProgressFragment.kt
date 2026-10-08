package com.rb.background.features.backgroundremover.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.rb.background.R
import com.rb.background.databinding.FragmentBackgroundSaveProgressBinding

class BackgroundSaveProgressFragment : Fragment() {

    private var _binding: FragmentBackgroundSaveProgressBinding? = null
    private val binding get() = _binding!!

    private val editorViewModel: BackgroundEditorViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBackgroundSaveProgressBinding.inflate(
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

        binding.imgPreview.setImageBitmap(
            editorViewModel.resultBitmap
        )

        binding.progressBar.progress = 0
        binding.progressRing.progress = 0

        binding.tvProgress.text = "Preparing your photo..."
        binding.tvProgressPercent.text = "0%"

        startProgress()
    }

    private fun startProgress() {

        var progress = 0

        val progressRunnable = object : Runnable {

            override fun run() {

                progress += 4

                if (progress > 100) {
                    progress = 100
                }

                binding.progressBar.setProgressCompat(
                    progress,
                    true
                )

                binding.progressRing.setProgressCompat(
                    progress,
                    true
                )

                binding.tvProgressPercent.text = "$progress%"

                updateSteps(progress)

                when {
                    progress < 25 -> {
                        binding.tvProgress.text =
                            "Removing background..."
                    }

                    progress < 60 -> {
                        binding.tvProgress.text =
                            "Applying your background..."
                    }

                    progress < 90 -> {
                        binding.tvProgress.text =
                            "Enhancing quality..."
                    }

                    progress < 100 -> {
                        binding.tvProgress.text =
                            "Finishing touches..."
                    }

                    else -> {

                        binding.tvProgress.text =
                            "Photo ready!"

                        handler.postDelayed({

                            if (isAdded) {

                                parentFragmentManager.beginTransaction()
                                    .setCustomAnimations(
                                        R.anim.slide_in_right,
                                        R.anim.slide_out_left,
                                        R.anim.slide_in_left,
                                        R.anim.slide_out_right
                                    )
                                    .replace(
                                        R.id.fragmentContainer,
                                        BackgroundResultFragment()
                                    )
                                    .addToBackStack(null)
                                    .commit()
                            }

                        }, 400)

                        return
                    }
                }

                handler.postDelayed(this, 80)
            }
        }

        handler.post(progressRunnable)
    }

    private fun updateSteps(progress: Int) {

        markStep(
            binding.dotStep1,
            binding.checkStep1,
            binding.labelStep1,
            done = progress >= 25
        )

        markStep(
            binding.dotStep2,
            binding.checkStep2,
            binding.labelStep2,
            done = progress >= 60
        )

        markStep(
            binding.dotStep3,
            binding.checkStep3,
            binding.labelStep3,
            done = progress >= 90
        )

        markStep(
            binding.dotStep4,
            binding.checkStep4,
            binding.labelStep4,
            done = progress >= 100
        )
    }

    private fun markStep(
        dot: android.widget.TextView,
        check: android.widget.ImageView,
        label: android.widget.TextView,
        done: Boolean
    ) {

        dot.visibility = if (done) View.GONE else View.VISIBLE
        check.visibility = if (done) View.VISIBLE else View.GONE

        label.setTextColor(
            if (done) {
                android.graphics.Color.parseColor("#403A48")
            } else {
                android.graphics.Color.parseColor("#88828E")
            }
        )
    }

    override fun onDestroyView() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroyView()
        _binding = null
    }
}