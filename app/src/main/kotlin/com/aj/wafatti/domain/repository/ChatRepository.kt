package com.aj.wafatti.domain.repository

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.model.Conversation
import com.aj.wafatti.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversationsFlow(): Flow<List<Conversation>>
    fun getMessagesFlow(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String, mediaUrl: String? = null): Result<Message>
    suspend fun createOrGetConversation(otherUserId: String): Result<Conversation>
    suspend fun markMessagesAsRead(conversationId: String): Result<Unit>

    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean)
    fun observeTypingStatus(conversationId: String, otherUserId: String): Flow<Boolean>
    fun setActiveConversationId(conversationId: String?)
}
