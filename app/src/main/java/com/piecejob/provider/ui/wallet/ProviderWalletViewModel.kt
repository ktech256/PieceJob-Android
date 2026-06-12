package com.piecejob.provider.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.ProviderWalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderWalletViewModel @Inject constructor(
    private val repository: ProviderWalletRepository
) : ViewModel() {

    private val _wallet = MutableStateFlow<WalletDto?>(null)
    val wallet: StateFlow<WalletDto?> = _wallet

    private val _history = MutableStateFlow<List<WalletTransactionDto>>(emptyList())
    val history: StateFlow<List<WalletTransactionDto>> = _history

    private val _statements = MutableStateFlow<List<StatementDto>>(emptyList())
    val statements: StateFlow<List<StatementDto>> = _statements

    private val _invoices = MutableStateFlow<List<InvoiceDto>>(emptyList())
    val invoices: StateFlow<List<InvoiceDto>> = _invoices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadWalletData()
    }

    fun loadWalletData() {
        viewModelScope.launch {
            _isLoading.value = true
            val balanceResponse = repository.getWalletBalance()
            if (balanceResponse.success) {
                _wallet.value = balanceResponse.data
            }
            
            val historyResponse = repository.getWalletHistory()
            if (historyResponse.success) {
                _history.value = historyResponse.data ?: emptyList()
            }

            val statementResponse = repository.getStatements()
            if (statementResponse.success) {
                _statements.value = statementResponse.data ?: emptyList()
            }

            val invoiceResponse = repository.getInvoices()
            if (invoiceResponse.success) {
                _invoices.value = invoiceResponse.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
