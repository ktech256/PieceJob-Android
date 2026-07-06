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
    private val walletRepository: WalletRepository,
    private val configRepository: com.piecejob.core.data.repository.ConfigRepository
) : ViewModel() {

    private val _wallet = MutableStateFlow<WalletDto?>(null)
    val wallet: StateFlow<WalletDto?> = _wallet

    val currencySymbol = MutableStateFlow(configRepository.getCurrencySymbol())

    private val _transactions = MutableStateFlow<List<WalletTransactionDto>>(emptyList())
    val transactions: StateFlow<List<WalletTransactionDto>> = _transactions

    private val _statements = MutableStateFlow<List<StatementDto>>(emptyList())
    val statements: StateFlow<List<StatementDto>> = _statements

    private val _invoices = MutableStateFlow<List<InvoiceDto>>(emptyList())
    val invoices: StateFlow<List<InvoiceDto>> = _invoices

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _withdrawSuccess = MutableStateFlow(false)
    val withdrawSuccess: StateFlow<Boolean> = _withdrawSuccess

    private val _navigationEvent = MutableStateFlow<com.piecejob.core.ui.navigation.Screen?>(null)
    val navigationEvent: StateFlow<com.piecejob.core.ui.navigation.Screen?> = _navigationEvent

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadWalletData()
    }

    fun loadWalletData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Refresh config
                launch {
                    configRepository.refreshWorkspaceConfig()
                    currencySymbol.value = configRepository.getCurrencySymbol()
                }

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

    fun requestWithdrawal(amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = walletRepository.requestWithdrawal(amount)
            if (response.success) {
                _withdrawSuccess.value = true
                loadWalletData() // Refresh balance
            } else {
                _error.value = response.message ?: "Withdrawal failed"
            }
            _isLoading.value = false
        }
    }

    fun payServiceFee(vendor: String, voucherNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = walletRepository.payServiceFee(vendor, voucherNumber)
            if (response.success) {
                loadWalletData() // Refresh balance and service fee balance
            } else {
                _error.value = response.message ?: "Service fee payment failed"
            }
            _isLoading.value = false
        }
    }

    fun resetWithdrawState() {
        _withdrawSuccess.value = false
        _error.value = null
    }

    fun onMenuClick(screen: com.piecejob.core.ui.navigation.Screen) {
        _navigationEvent.value = screen
    }

    fun resetNavigationEvent() {
        _navigationEvent.value = null
    }

    // Support legacy UI access
    val history: StateFlow<List<WalletTransactionDto>> get() = transactions
}
