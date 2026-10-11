package com.aj.wafatti.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aj.wafatti.domain.model.Conversation
import com.aj.wafatti.domain.model.User

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val participantIdsCsv: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
) {
    fun toDomain(otherUser: User? = null): Conversation = Conversation(
        id = id,
        participantIds = participantIdsCsv.split(",").filter { it.isNotBlank() },
        otherUser = otherUser,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp,
        unreadCount = unreadCount
    )

    companion object {
        fun fromDomain(conversation: Conversation): ConversationEntity = ConversationEntity(
            id = conversation.id,
            participantIdsCsv = conversation.participantIds.joinToString(","),
            lastMessage = conversation.lastMessage,
            lastMessageTimestamp = conversation.lastMessageTimestamp,
            unreadCount = conversation.unreadCount
        )
    }
}
