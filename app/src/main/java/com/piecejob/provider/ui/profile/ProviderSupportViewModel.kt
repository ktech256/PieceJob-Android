package com.piecejob.provider.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.TicketRepository
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.data.remote.TicketDto
import com.piecejob.core.data.remote.SubmitTicketRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderSupportViewModel @Inject constructor(
    private val repository: TicketRepository
) : ViewModel() {

    private val _tickets = MutableStateFlow<List<TicketDto>>(emptyList())
    val tickets: StateFlow<List<TicketDto>> = _tickets

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadTickets()
    }

    fun loadTickets() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getMyTickets()
            if (res.success) {
                _tickets.value = res.data ?: emptyList()
            }
            _isLoading.value = false
        }
    }

    fun submitTicket(type: String, subject: String, desc: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitTicket(SubmitTicketRequest(null, type, subject, desc))
            if (res.success) {
                loadTickets()
            }
            _isLoading.value = false
        }
    }
}
