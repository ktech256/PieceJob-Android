package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ProviderRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderEquipmentViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    private val _equipment = MutableStateFlow<List<EquipmentDto>>(emptyList())
    val equipment: StateFlow<List<EquipmentDto>> = _equipment

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadEquipment()
    }

    fun loadEquipment() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getMyEquipment()
            if (response.success) {
                _equipment.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun addTool(name: String, category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.addEquipment(EquipmentDto(name, category, null, false))
            if (response.success) {
                _equipment.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
