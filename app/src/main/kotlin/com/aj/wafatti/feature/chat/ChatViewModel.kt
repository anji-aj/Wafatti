package com.aj.wafatti.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aj.wafatti.domain.repository.AuthRepository
import com.aj.wafatti.domain.repository.ChatRepository
import com.aj.wafatti.domain.repository.UserRepository
import com.aj.wafatti.domain.usecase.GetMessagesUseCase
import com.aj.wafatti.domain.usecase.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentConversationId: String = ""
    private var typingJob: Job? = null
    private var isCurrentlyTypingInDb = false

    fun initialize(conversationId: String) {
        val myUid = authRepository.getCurrentUserId() ?: ""
        val participantIds = if (conversationId.contains("_")) conversationId.split("_").filter { it.isNotBlank() } else emptyList()
        val otherUserId = participantIds.firstOrNull { it != myUid } ?: if (!conversationId.contains("_")) conversationId else ""

        val deterministicConvId = if (myUid.isNotBlank() && otherUserId.isNotBlank()) {
            if (myUid < otherUserId) "${myUid}_${otherUserId}" else "${otherUserId}_${myUid}"
        } else {
            conversationId
        }

        currentConversationId = deterministicConvId
        chatRepository.setActiveConversationId(deterministicConvId)

        viewModelScope.launch {
            // Ensure conversation record exists for both users
            if (otherUserId.isNotBlank() && otherUserId != "me") {
                chatRepository.createOrGetConversation(otherUserId)
            }

            // Stream messages
            launch {
                getMessagesUseCase(deterministicConvId).collect { messages ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages
                        )
                    }
                    // When new messages arrive while viewing this screen, mark them SEEN
                    chatRepository.markMessagesAsRead(deterministicConvId)
                }
            }

            // Stream target user presence
            if (otherUserId.isNotBlank() && otherUserId != "me") {
                launch {
                    userRepository.getUserFlow(otherUserId).collect { otherUser ->
                        _uiState.update { it.copy(otherUser = otherUser) }
                    }
                }

                // Stream target user typing status
                launch {
                    chatRepository.observeTypingStatus(deterministicConvId, otherUserId).collect { isTyping ->
                        _uiState.update { it.copy(isOtherUserTyping = isTyping) }
                    }
                }
            }

            // Mark initial messages as SEEN
            chatRepository.markMessagesAsRead(deterministicConvId)
        }
    }

    fun onTextInputChanged(text: String) {
        _uiState.update { it.copy(textInput = text) }

        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            updateTypingStatus(false)
        } else {
            if (!isCurrentlyTypingInDb) {
                updateTypingStatus(true)
            }
            typingJob?.cancel()
            typingJob = viewModelScope.launch {
                delay(1200) // 1.2s debounce
                updateTypingStatus(false)
            }
        }
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        isCurrentlyTypingInDb = isTyping
        val convId = currentConversationId
        if (convId.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.setTypingStatus(convId, isTyping)
            }
        }
    }

    fun sendMessage() {
        val text = _uiState.value.textInput.trim()
        if (text.isBlank() || currentConversationId.isBlank()) return

        typingJob?.cancel()
        updateTypingStatus(false)

        _uiState.update { it.copy(textInput = "") }
        viewModelScope.launch {
            sendMessageUseCase(currentConversationId, text)
        }
    }

    fun onScreenVisible() {
        if (currentConversationId.isNotBlank()) {
            chatRepository.setActiveConversationId(currentConversationId)
            viewModelScope.launch {
                chatRepository.markMessagesAsRead(currentConversationId)
            }
        }
    }

    fun onScreenHidden() {
        chatRepository.setActiveConversationId(null)
        typingJob?.cancel()
        updateTypingStatus(false)
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.setActiveConversationId(null)
        typingJob?.cancel()
        updateTypingStatus(false)
    }

    fun saveContactName(customName: String?) {
        val targetId = _uiState.value.otherUser?.id
        if (!targetId.isNullOrBlank()) {
            viewModelScope.launch {
                userRepository.saveContactName(targetId, customName)
            }
        }
    }
}
