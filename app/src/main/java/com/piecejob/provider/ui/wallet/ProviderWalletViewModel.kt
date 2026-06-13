package com.piecejob.provider.ui.wallet

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
class ProviderWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _wallet = MutableStateFlow<WalletDto?>(null)
    val wallet: StateFlow<WalletDto?> = _wallet

    private val _transactions = MutableStateFlow<List<WalletTransactionDto>>(emptyList())
    val transactions: StateFlow<List<WalletTransactionDto>> = _transactions

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
            try {
                // Parallel load
                launch {
                    val balanceRes = walletRepository.getWalletBalance()
                    if (balanceRes.success) _wallet.value = balanceRes.data
                }
                launch {
                    val historyRes = walletRepository.getWalletHistory()
                    if (historyRes.success) _transactions.value = historyRes.data ?: emptyList()
                }
                launch {
                    val invRes = walletRepository.getInvoices()
                    if (invRes.success) _invoices.value = invRes.data ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Support legacy UI access
    val history: StateFlow<List<WalletTransactionDto>> get() = transactions
}
