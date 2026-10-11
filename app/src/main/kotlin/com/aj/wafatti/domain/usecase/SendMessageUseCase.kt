package com.aj.wafatti.domain.usecase

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.model.Message
import com.aj.wafatti.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String,
        mediaUrl: String? = null
    ): Result<Message> {
        return chatRepository.sendMessage(conversationId, text, mediaUrl)
    }
}
