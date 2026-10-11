package com.aj.wafatti.feature.chat

import com.aj.wafatti.domain.model.Message
import com.aj.wafatti.domain.model.User

data class ChatUiState(
    val isLoading: Boolean = false,
    val otherUser: User? = null,
    val isOtherUserTyping: Boolean = false,
    val messages: List<Message> = emptyList(),
    val textInput: String = "",
    val error: String? = null
)
