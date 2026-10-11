package com.aj.wafatti.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aj.wafatti.domain.model.Message
import com.aj.wafatti.domain.model.MessageStatus
import com.aj.wafatti.domain.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String = "",
    val text: String,
    val mediaUrl: String?,
    val messageType: String,
    val status: String,
    val timestamp: Long
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        receiverId = receiverId,
        text = text,
        mediaUrl = mediaUrl,
        messageType = try { MessageType.valueOf(messageType) } catch (_: Exception) { MessageType.TEXT },
        status = try { MessageStatus.valueOf(status) } catch (_: Exception) { MessageStatus.SENT },
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(message: Message): MessageEntity = MessageEntity(
            id = message.id,
            conversationId = message.conversationId,
            senderId = message.senderId,
            receiverId = message.receiverId,
            text = message.text,
            mediaUrl = message.mediaUrl,
            messageType = message.messageType.name,
            status = message.status.name,
            timestamp = message.timestamp
        )
    }
}
