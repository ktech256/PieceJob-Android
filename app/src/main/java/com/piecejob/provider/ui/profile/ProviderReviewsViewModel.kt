package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.ReviewDto
import com.piecejob.core.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderReviewsViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _reviews = MutableStateFlow<List<ReviewDto>>(emptyList())
    val reviews: StateFlow<List<ReviewDto>> = _reviews

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadReviews()
    }

    fun loadReviews() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getMyReviews()
            if (response.success) {
                _reviews.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
