package com.piecejob.customer.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val configRepository: com.piecejob.core.data.repository.ConfigRepository
) : ViewModel() {

    private val _wallet = MutableStateFlow<WalletDto?>(null)
    val wallet: StateFlow<WalletDto?> = _wallet

    val currencySymbol = MutableStateFlow(configRepository.getCurrencySymbol())

    private val _history = MutableStateFlow<List<WalletTransactionDto>>(emptyList())
    val history: StateFlow<List<WalletTransactionDto>> = _history

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
            val balanceResponse = walletRepository.getWalletBalance()
            if (balanceResponse.success) {
                _wallet.value = balanceResponse.data
            }
            
            val historyResponse = walletRepository.getWalletHistory()
            if (historyResponse.success) {
                _history.value = historyResponse.data ?: emptyList()
            }

            val invoiceResponse = walletRepository.getInvoices()
            if (invoiceResponse.success) {
                _invoices.value = invoiceResponse.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }
}
