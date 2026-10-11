package com.aj.wafatti.domain.usecase

import com.aj.wafatti.domain.model.Message
import com.aj.wafatti.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(conversationId: String): Flow<List<Message>> {
        return chatRepository.getMessagesFlow(conversationId)
    }
}
