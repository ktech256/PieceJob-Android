package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.remote.TicketDto
import com.piecejob.core.data.repository.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderTicketDetailViewModel @Inject constructor(
    private val repository: SupportRepository
) : ViewModel() {

    private val _ticket = MutableStateFlow<TicketDto?>(null)
    val ticket: StateFlow<TicketDto?> = _ticket

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadTicket(ticketId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val response = repository.getTicketDetails(ticketId)
            if (response.success) {
                _ticket.value = response.data
            }
            _isLoading.value = false
        }
    }

    fun sendMessage(text: String, attachments: List<String> = emptyList()) {
        val ticketId = _ticket.value?.id ?: return
        viewModelScope.launch {
            val response = repository.sendTicketMessage(ticketId, text, attachments)
            if (response.success) {
                _ticket.value = response.data
            }
        }
    }
}
