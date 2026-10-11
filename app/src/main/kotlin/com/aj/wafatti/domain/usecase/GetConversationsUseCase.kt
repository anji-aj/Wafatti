package com.aj.wafatti.domain.usecase

import com.aj.wafatti.domain.model.Conversation
import com.aj.wafatti.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(): Flow<List<Conversation>> {
        return chatRepository.getConversationsFlow()
    }
}
