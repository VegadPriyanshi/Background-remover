package com.rb.background.features.headshot.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rb.background.R
import com.rb.background.databinding.FragmentGeneratingHeadshotBinding
import com.rb.background.features.headshot.repository.InstantIdHeadshotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class GeneratingHeadshotFragment : Fragment() {

    private var _binding: FragmentGeneratingHeadshotBinding? = null
    private val binding get() = _binding!!

    private lateinit var headshotRepository: InstantIdHeadshotRepository

    private var selectedImageUri: String? = null

    private var selectedProfession: String = ""

    private var selectedTemplateUrl: String = ""

    private var progressTickerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedImageUri =
            arguments?.getString(ARG_IMAGE_URI)

        selectedProfession =
            arguments?.getString(ARG_PROFESSION).orEmpty()

        selectedTemplateUrl =
            arguments?.getString(ARG_TEMPLATE_URL).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentGeneratingHeadshotBinding.inflate(
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

        selectedImageUri?.let { uriString ->

            binding.imgGeneratingPreview.setImageURI(
                Uri.parse(uriString)
            )
        }

        binding.tvSelectedProfession.text =
            "👤 $selectedProfession"

        binding.tvSelectedStyle.text =
            "🎨 Selected Template"

        binding.tvSelectedPose.text =
            "📸 Professional Headshot"

        binding.tvSelectedBackground.text =
            "✨ AI Generated"

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        headshotRepository =
            InstantIdHeadshotRepository(requireContext())

        startGenerationAnimation()
    }

    private fun startGenerationAnimation() {

        binding.progressRing.progress = 0
        binding.progressGeneration.progress = 0
        binding.tvProgressPercent.text = "0%"

        binding.tvProgressTitle.text =
            "Preparing your headshot..."

        startProgressTrickle()
        generateRealHeadshot()
    }

    // Real progress can't be measured from the generation API, so
    // this climbs toward (but never reaches) 90% while the real
    // request is in flight, then jumps to 100% once it actually
    // completes — the status text above the bar still reflects
    // real backend stages via onStatusUpdate.
    private fun startProgressTrickle() {

        progressTickerJob = viewLifecycleOwner.lifecycleScope.launch {

            var progress = 0

            while (progress < 90) {

                delay(150)

                progress += 2

                if (_binding == null) {
                    return@launch
                }

                binding.progressRing.setProgressCompat(progress, true)
                binding.progressGeneration.setProgressCompat(progress, true)
                binding.tvProgressPercent.text = "$progress%"
            }
        }
    }

    private fun generateRealHeadshot() {

        val imageUri = selectedImageUri

        if (imageUri.isNullOrEmpty()) {

            Toast.makeText(
                requireContext(),
                "Photo not available",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (selectedTemplateUrl.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "Template not selected",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val result =
                headshotRepository.generateHeadshot(
                    imageUri = imageUri,
                    profession = selectedProfession,
                    templateUrl = selectedTemplateUrl,
                    onStatusUpdate = { status ->

                        if (isAdded && _binding != null) {

                            binding.tvProgressTitle.text = status
                        }
                    }
                )

            if (!isAdded) {
                return@launch
            }

            result.onSuccess { generatedImageBytes ->

                progressTickerJob?.cancel()

                if (_binding != null) {

                    binding.progressRing.setProgressCompat(100, true)
                    binding.progressGeneration.setProgressCompat(100, true)
                    binding.tvProgressPercent.text = "100%"
                    binding.tvProgressTitle.text = "Photo ready!"
                }

                delay(400)

                openResultScreen(
                    generatedImageBytes
                )

            }.onFailure { error ->

                progressTickerJob?.cancel()

                Toast.makeText(
                    requireContext(),
                    "Unable to generate headshot: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openResultScreen(
        generatedImageBytes: ByteArray
    ) {

        val generatedImageUri =
            saveGeneratedImageTemporarily(
                generatedImageBytes
            )

        if (generatedImageUri == null) {

            Toast.makeText(
                requireContext(),
                "Unable to prepare generated image",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(
                R.id.fragmentContainer,
                HeadshotResultFragment.newInstance(
                    imageUri = generatedImageUri,
                    profession = selectedProfession,
                    templateUrl = selectedTemplateUrl
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun saveGeneratedImageTemporarily(
        imageBytes: ByteArray
    ): String? {

        return try {

            val file =
                File(
                    requireContext().cacheDir,
                    "generated_headshot_${System.currentTimeMillis()}.png"
                )

            FileOutputStream(file).use { outputStream ->
                outputStream.write(imageBytes)
            }

            Uri.fromFile(file).toString()

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_IMAGE_URI = "image_uri"
        private const val ARG_PROFESSION = "profession"
        private const val ARG_TEMPLATE_URL = "template_url"

        fun newInstance(
            imageUri: String,
            profession: String,
            templateUrl: String
        ): GeneratingHeadshotFragment {

            return GeneratingHeadshotFragment().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_IMAGE_URI,
                        imageUri
                    )

                    putString(
                        ARG_PROFESSION,
                        profession
                    )

                    putString(
                        ARG_TEMPLATE_URL,
                        templateUrl
                    )
                }
            }
        }
    }
}