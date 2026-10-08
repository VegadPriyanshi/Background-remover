package com.rb.background.features.templates.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rb.background.features.templates.repository.TemplateRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.rb.background.features.templates.model.Photo

class TemplateViewModel : ViewModel() {

    private val repository = TemplateRepository()
    private var currentPage = 1
    private var isLoading = false
    private var hasMorePages = true
    private var currentQuery = "beach"
    private var currentOrientation: String? = null
    private val _photos = MutableLiveData<List<Photo>>()

    val photos: LiveData<List<Photo>>
        get() = _photos

    private val _error = MutableLiveData<String>()

    val error: LiveData<String>
        get() = _error

    fun loadBeachPhotos() {

        viewModelScope.launch {

            try {

                val response =
                    repository.searchPhotos("beach")

                if (response.isSuccessful) {

                    _photos.value =
                        response.body()?.photos ?: emptyList()

                } else {

                    android.util.Log.e(
                        "PEXELS",
                        response.message()
                    )

                    _error.value = "Failed to load templates."

                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "PEXELS",
                    "Network error",
                    e
                )

                _error.value = "No Internet Connection"

            }

        }

    }


    fun loadPhotosForProfession(
        profession: String
    ) {

        // Bias results toward portrait, face-forward
        // photos that actually work as headshot templates.
        currentQuery = "$profession portrait headshot"
        currentOrientation = "portrait"

        currentPage = 1
        hasMorePages = true

        viewModelScope.launch {

            isLoading = true

            try {

                val response =
                    repository.searchPhotos(
                        query = currentQuery,
                        orientation = currentOrientation
                    )

                if (response.isSuccessful) {

                    _photos.value =
                        response.body()?.photos
                            ?: emptyList()

                } else {

                    android.util.Log.e(
                        "PEXELS",
                        response.message()
                    )

                    _error.value =
                        "Failed to load templates."

                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "PEXELS",
                    "Network error",
                    e
                )

                _error.value =
                    "No Internet Connection"

            } finally {

                isLoading = false

            }
        }
    }

    fun searchPhotos(
        query: String
    ) {
        currentQuery = query

        currentPage = 1
        hasMorePages = true
        viewModelScope.launch {

            isLoading = true

            try {

                val response =
                    repository.searchPhotos(
                        query = query,
                        orientation = currentOrientation
                    )

                if (response.isSuccessful) {

                    _photos.value =
                        response.body()?.photos ?: emptyList()

                } else {

                    android.util.Log.e(
                        "PEXELS",
                        response.message()
                    )

                    _error.value = "Failed to load templates."

                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "PEXELS",
                    "Network error",
                    e
                )

                _error.value = "No Internet Connection"

            } finally {

                isLoading = false

            }

        }

    }

    fun loadNextPage() {

        if (isLoading || !hasMorePages) return

        currentPage++

        viewModelScope.launch {

            isLoading = true

            try {

                val response = repository.searchPhotos(

                    query = currentQuery,

                    page = currentPage,

                    orientation = currentOrientation

                )

                if (response.isSuccessful) {

                    val newPhotos =
                        response.body()?.photos ?: emptyList()

                    if (newPhotos.isEmpty()) {

                        hasMorePages = false

                    } else {

                        if (newPhotos.size < 20) {

                            hasMorePages = false

                        }

                        val currentList =
                            _photos.value?.toMutableList()
                                ?: mutableListOf()

                        currentList.addAll(newPhotos)

                        _photos.value = currentList

                        android.util.Log.d(
                            "PAGINATION",
                            "Loaded Page : $currentPage"
                        )

                    }

                } else {

                    android.util.Log.e(
                        "PAGINATION",
                        response.message()
                    )

                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "PAGINATION",
                    "Network error",
                    e
                )

                _error.value = "No Internet Connection"

            } finally {

                isLoading = false

            }

        }

    }

}