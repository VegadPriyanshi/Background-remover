package com.rb.background.features.headshot.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.background.databinding.BottomSheetTemplatesBinding
import com.rb.background.features.templates.adapter.TemplateAdapter
import com.rb.background.features.templates.model.Photo
import com.rb.background.features.templates.viewmodel.TemplateViewModel

class HeadshotTemplateBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTemplatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TemplateViewModel by viewModels()

    private lateinit var adapter: TemplateAdapter

    private var profession: String = ""

    private var onTemplateSelected:
            ((Photo, String) -> Unit)? = null

    fun setProfession(
        profession: String
    ) {
        this.profession = profession
    }

    fun setOnTemplateSelectedListener(
        listener: (Photo, String) -> Unit
    ) {
        onTemplateSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            BottomSheetTemplatesBinding.inflate(
                inflater,
                container,
                false
            )

        setupRecyclerView()
        setupObservers()
        setupSearch()

        binding.progressBar.visibility =
            View.VISIBLE

        if (profession.isNotBlank()) {

            viewModel.loadPhotosForProfession(
                profession
            )

        } else {

            viewModel.searchPhotos(
                "professional headshot"
            )
        }

        return binding.root
    }

    private fun setupRecyclerView() {

        adapter = TemplateAdapter()

        val layoutManager =
            GridLayoutManager(
                requireContext(),
                2
            )

        binding.rvTemplates.layoutManager =
            layoutManager

        binding.rvTemplates.adapter =
            adapter

        binding.rvTemplates.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {

                    super.onScrolled(
                        recyclerView,
                        dx,
                        dy
                    )

                    if (dy <= 0) {
                        return
                    }

                    val visibleItemCount =
                        layoutManager.childCount

                    val totalItemCount =
                        layoutManager.itemCount

                    val firstVisibleItem =
                        layoutManager
                            .findFirstVisibleItemPosition()

                    if (
                        visibleItemCount +
                        firstVisibleItem >=
                        totalItemCount - 2
                    ) {

                        viewModel.loadNextPage()
                    }
                }
            }
        )

        adapter.setOnTemplateClickListener { photo ->

            confirmTemplateGender(photo)
        }
    }

    // We have no automatic way to detect who's shown in a
    // stock template photo, so we ask the user to confirm it
    // once at selection time — that's what later gets checked
    // against their own uploaded photo before generation runs.
    private fun confirmTemplateGender(
        photo: Photo
    ) {

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Template")
            .setMessage("Who is shown in this template photo?")
            .setPositiveButton("Woman") { dialog, _ ->

                dialog.dismiss()

                onTemplateSelected?.invoke(photo, "Woman")

                dismiss()
            }
            .setNegativeButton("Man") { dialog, _ ->

                dialog.dismiss()

                onTemplateSelected?.invoke(photo, "Man")

                dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun setupObservers() {

        viewModel.photos.observe(
            viewLifecycleOwner
        ) { photos ->

            binding.progressBar.visibility =
                View.GONE

            adapter.submitList(photos)
        }

        viewModel.error.observe(
            viewLifecycleOwner
        ) { message ->

            binding.progressBar.visibility =
                View.GONE

            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupSearch() {

        binding.searchView.setOnQueryTextListener(

            object :
                SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {

                    val searchText =
                        query?.trim()

                    if (
                        !searchText.isNullOrEmpty()
                    ) {

                        binding.progressBar.visibility =
                            View.VISIBLE

                        viewModel.searchPhotos(
                            searchText
                        )

                        binding.searchView.clearFocus()
                    }

                    return true
                }

                override fun onQueryTextChange(
                    newText: String?
                ): Boolean {

                    return true
                }
            }
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}