package com.piecejob.provider.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ChatRepository
import com.piecejob.core.data.repository.TicketRepository
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.*
import com.piecejob.core.data.remote.dto.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderMessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val ticketRepository: TicketRepository,
    private val jobRepository: JobRepository,
    private val socketManager: com.piecejob.core.socket.SocketManager
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationDto>>(emptyList())
    val conversations: StateFlow<List<ConversationDto>> = _conversations

    private val _tickets = MutableStateFlow<List<TicketDto>>(emptyList())
    val tickets: StateFlow<List<TicketDto>> = _tickets

    private val _disputes = MutableStateFlow<List<DisputeDto>>(emptyList())
    val disputes: StateFlow<List<DisputeDto>> = _disputes

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _navigationEvent = MutableSharedFlow<Pair<String, String>>()
    val navigationEvent: SharedFlow<Pair<String, String>> = _navigationEvent

    init {
        loadMessages()
        observeSocket()
    }

    private fun observeSocket() {
        viewModelScope.launch {
            socketManager.messageEventFlow.collect {
                loadMessages()
            }
        }
        viewModelScope.launch {
            socketManager.statusEventFlow.collect {
                loadMessages()
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val convRes = chatRepository.getConversations()
                if (convRes.success) {
                    _conversations.value = convRes.data ?: emptyList()
                }

                val ticketsRes = ticketRepository.getMyTickets()
                if (ticketsRes.success) {
                    _tickets.value = ticketsRes.data ?: emptyList()
                }

                val disputeRes = jobRepository.getMyDisputes()
                if (disputeRes.success) {
                    _disputes.value = disputeRes.data ?: emptyList()
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openChat(jobId: String, otherUserId: String) {
        viewModelScope.launch {
            _navigationEvent.emit(jobId to otherUserId)
        }
    }
}
