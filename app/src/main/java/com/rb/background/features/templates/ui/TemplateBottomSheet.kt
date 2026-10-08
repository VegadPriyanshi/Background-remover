package com.rb.background.features.templates.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rb.background.databinding.BottomSheetTemplatesBinding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.rb.background.features.templates.adapter.TemplateAdapter
import com.rb.background.features.templates.viewmodel.TemplateViewModel
import androidx.lifecycle.Observer
import com.rb.background.features.templates.model.Photo
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class TemplateBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTemplatesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TemplateViewModel by viewModels()

    private lateinit var adapter: TemplateAdapter
    private var onTemplateSelected: ((Photo) -> Unit)? = null

    fun setOnTemplateSelectedListener(
        listener: (Photo) -> Unit
    ) {
        onTemplateSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetTemplatesBinding.inflate(
            inflater,
            container,
            false
        )
        adapter = TemplateAdapter()

        val layoutManager = GridLayoutManager(
            requireContext(),
            2
        )

        binding.rvTemplates.layoutManager = layoutManager

        binding.rvTemplates.adapter = adapter
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

                    if (dy <= 0) return

                    val visibleItemCount =
                        layoutManager.childCount

                    val totalItemCount =
                        layoutManager.itemCount

                    val firstVisibleItem =
                        layoutManager.findFirstVisibleItemPosition()

                    if (

                        visibleItemCount + firstVisibleItem
                        >= totalItemCount - 2

                    ) {

                        viewModel.loadNextPage()

                    }

                }

            }

        )
        adapter.setOnTemplateClickListener { photo ->

            onTemplateSelected?.invoke(photo)

            dismiss()

        }

        viewModel.photos.observe(viewLifecycleOwner) { photos ->

            binding.progressBar.visibility = View.GONE

            adapter.submitList(photos)

        }
        viewModel.error.observe(viewLifecycleOwner) { message ->

            binding.progressBar.visibility = View.GONE

            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()

        }

        binding.progressBar.visibility = View.VISIBLE

        if (isInternetAvailable()) {

            android.util.Log.d(
                "NETWORK",
                "Internet Available"
            )

        } else {

            android.util.Log.d(
                "NETWORK",
                "No Internet"
            )

        }

        viewModel.loadBeachPhotos()

        binding.searchView.setOnQueryTextListener(

            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {

                    val searchText = query?.trim()

                    if (!searchText.isNullOrEmpty()) {

                        binding.progressBar.visibility = View.VISIBLE

                        binding.progressBar.visibility = View.VISIBLE

                        viewModel.searchPhotos(searchText)

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

        return binding.root
    }

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            requireContext().getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}