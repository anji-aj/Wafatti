package com.aj.wafatti.feature.chatlist

import com.aj.wafatti.domain.model.Conversation

data class ChatListUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val error: String? = null
)
