package com.aj.wafatti.domain.model

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val otherUser: User? = null,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)
