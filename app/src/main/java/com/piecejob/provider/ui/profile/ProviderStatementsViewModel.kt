package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.WalletRepository
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderStatementsViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _statements = MutableStateFlow<List<StatementDto>>(emptyList())
    val statements: StateFlow<List<StatementDto>> = _statements

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadStatements()
    }

    fun loadStatements() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = walletRepository.getStatements()
            if (response.success) {
                _statements.value = response.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
