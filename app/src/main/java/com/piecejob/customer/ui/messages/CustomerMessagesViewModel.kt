package com.piecejob.customer.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piecejob.core.data.repository.ChatRepository
import com.piecejob.core.data.repository.JobRepository
import com.piecejob.core.data.remote.dto.*
import com.piecejob.core.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerMessagesViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationDto>>(emptyList())
    val conversations: StateFlow<List<ConversationDto>> = _conversations

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
                val response = chatRepository.getConversations()
                if (response.success) {
                    _conversations.value = response.data ?: emptyList()
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
