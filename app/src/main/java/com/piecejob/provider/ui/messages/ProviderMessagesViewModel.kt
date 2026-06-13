package com.piecejob.provider.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ChatRepository
import com.piecejob.core.data.repository.TicketRepository
import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderMessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _tickets = MutableStateFlow<List<TicketDto>>(emptyList())
    val tickets: StateFlow<List<TicketDto>> = _tickets

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ticketsRes = ticketRepository.getMyTickets()
                if (ticketsRes.success) {
                    _tickets.value = ticketsRes.data ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}
