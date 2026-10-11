package com.aj.wafatti.data.repository

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.data.local.database.dao.ConversationDao
import com.aj.wafatti.data.local.database.dao.MessageDao
import com.aj.wafatti.data.local.database.dao.UserDao
import com.aj.wafatti.data.local.database.entity.ConversationEntity
import com.aj.wafatti.data.local.database.entity.MessageEntity
import com.aj.wafatti.data.local.database.entity.UserEntity
import com.aj.wafatti.domain.model.Conversation
import com.aj.wafatti.domain.model.Message
import com.aj.wafatti.domain.model.MessageStatus
import com.aj.wafatti.domain.model.MessageType
import com.aj.wafatti.domain.model.User
import com.aj.wafatti.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : ChatRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var activeConversationId: String? = null

    override fun setActiveConversationId(conversationId: String?) {
        activeConversationId = conversationId
    }

    private fun getMyUid(): String {
        return firebaseAuth.currentUser?.uid ?: ""
    }

    private fun getDeterministicConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val myUid = getMyUid()
        if (myUid.isBlank() || conversationId.isBlank()) return
        val ref = database.getReference("typing/$conversationId/$myUid")
        if (isTyping) {
            ref.setValue(true)
        } else {
            ref.removeValue()
        }
    }

    override fun observeTypingStatus(conversationId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        if (conversationId.isBlank() || otherUserId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val ref = database.getReference("typing/$conversationId/$otherUserId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isTyping)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override fun getConversationsFlow(): Flow<List<Conversation>> {
        val myUid = getMyUid()

        if (myUid.isNotBlank()) {
            // Real-time listener for user's conversations in Firestore
            firestore.collection("conversations")
                .whereArrayContains("participantIds", myUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    repositoryScope.launch {
                        val convEntities = mutableListOf<ConversationEntity>()
                        for (doc in snapshot.documents) {
                            val convId = doc.id
                            @Suppress("UNCHECKED_CAST")
                            val participantIds = doc.get("participantIds") as? List<String>
                                ?: convId.split("_").filter { it.isNotBlank() }
                            val otherUid = participantIds.firstOrNull { it != myUid } ?: ""
                            val lastMsg = doc.getString("lastMessage") ?: ""
                            val lastMsgTime = doc.getLong("lastMessageTimestamp")
                                ?: doc.getLong("updatedAt")
                                ?: System.currentTimeMillis()
                            val unread = (doc.getLong("unreadCount_$myUid") ?: 0L).toInt()

                            if (otherUid.isNotBlank()) {
                                try {
                                    val otherDoc = firestore.collection("users").document(otherUid).get().await()
                                    if (otherDoc.exists()) {
                                        val existingSavedName = userDao.getUserByIdSync(otherUid)?.savedName
                                        val existingLocal = userDao.getUserByIdSync(otherUid)
                                        val otherUser = UserEntity(
                                            id = otherUid,
                                            displayName = otherDoc.getString("displayName") ?: "User",
                                            email = otherDoc.getString("email") ?: "",
                                            phoneNumber = otherDoc.getString("phoneNumber") ?: "",
                                            avatarUrl = otherDoc.getString("photoUrl"),
                                            isOnline = existingLocal?.isOnline ?: false,
                                            lastSeen = existingLocal?.lastSeen ?: System.currentTimeMillis(),
                                            bio = otherDoc.getString("bio") ?: "",
                                            savedName = existingSavedName
                                        )
                                        userDao.insertUser(otherUser)
                                    }
                                } catch (_: Exception) {
                                }
                            }

                            val convEntity = ConversationEntity(
                                id = convId,
                                participantIdsCsv = participantIds.joinToString(","),
                                lastMessage = lastMsg,
                                lastMessageTimestamp = lastMsgTime,
                                unreadCount = unread
                            )
                            convEntities.add(convEntity)
                        }
                        if (convEntities.isNotEmpty()) {
                            conversationDao.insertConversations(convEntities)
                        }
                    }
                }
        }

        return combine(
            conversationDao.getConversationsFlow(),
            userDao.getAllUsersFlow()
        ) { entities, users ->
            val userMap = users.associateBy { it.id }
            entities.mapNotNull { entity ->
                val participantIds = entity.participantIdsCsv.split(",").filter { it.isNotBlank() }
                if (myUid.isNotBlank() && participantIds.isNotEmpty() && !participantIds.contains(myUid)) {
                    return@mapNotNull null
                }
                val otherUid = participantIds.firstOrNull { it != myUid } ?: ""
                val otherUser = userMap[otherUid]?.toDomain()
                entity.toDomain(otherUser)
            }
        }
    }

    override fun getMessagesFlow(conversationId: String): Flow<List<Message>> {
        val myUid = getMyUid()
        val participantIds = if (conversationId.contains("_")) conversationId.split("_").filter { it.isNotBlank() } else emptyList()
        val deterministicConvId = if (participantIds.size >= 2) {
            getDeterministicConversationId(participantIds[0], participantIds[1])
        } else conversationId

        // Real-time listener for conversation messages in Firestore
        firestore.collection("conversations")
            .document(deterministicConvId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                repositoryScope.launch {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val senderId = doc.getString("senderId") ?: return@mapNotNull null
                        val text = doc.getString("text") ?: ""
                        val mediaUrl = doc.getString("mediaUrl")
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        var statusStr = doc.getString("status") ?: MessageStatus.SENT.name

                        // If incoming message to current user
                        if (senderId != myUid) {
                            val isCurrentChatActive = (activeConversationId == deterministicConvId)
                            if (isCurrentChatActive && statusStr != "SEEN") {
                                statusStr = "SEEN"
                                try {
                                    doc.reference.set(mapOf("status" to "SEEN"), SetOptions.merge())
                                } catch (_: Exception) {}
                            } else if (!isCurrentChatActive && statusStr == "SENT") {
                                statusStr = "RECEIVED"
                                try {
                                    doc.reference.set(mapOf("status" to "RECEIVED"), SetOptions.merge())
                                } catch (_: Exception) {}
                            }
                        }

                        MessageEntity(
                            id = id,
                            conversationId = deterministicConvId,
                            senderId = if (senderId == myUid) "me" else senderId,
                            receiverId = if (senderId == myUid) "" else myUid,
                            text = text,
                            mediaUrl = mediaUrl,
                            messageType = if (mediaUrl != null) MessageType.IMAGE.name else MessageType.TEXT.name,
                            status = statusStr,
                            timestamp = timestamp
                        )
                    }
                    messageDao.insertMessages(messages)
                }
            }

        return messageDao.getMessagesFlow(deterministicConvId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(conversationId: String, text: String, mediaUrl: String?): Result<Message> {
        val myUid = getMyUid()
        if (myUid.isBlank()) return Result.Error(Exception("User not authenticated"))

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val participantIds = if (conversationId.contains("_")) {
            conversationId.split("_").filter { it.isNotBlank() }
        } else {
            listOf(myUid, conversationId)
        }
        val otherUid = participantIds.firstOrNull { it != myUid } ?: ""
        val deterministicConvId = if (participantIds.size >= 2) {
            getDeterministicConversationId(participantIds[0], participantIds[1])
        } else conversationId

        return try {
            val message = Message(
                id = messageId,
                conversationId = deterministicConvId,
                senderId = "me",
                receiverId = otherUid,
                text = text,
                mediaUrl = mediaUrl,
                messageType = if (mediaUrl != null) MessageType.IMAGE else MessageType.TEXT,
                status = MessageStatus.SENT,
                timestamp = timestamp
            )

            // 1. Save to local Room DB immediately
            messageDao.insertMessage(MessageEntity.fromDomain(message))

            val conversation = conversationDao.getConversationById(deterministicConvId)
            val updatedConv = conversation?.copy(
                lastMessage = text,
                lastMessageTimestamp = timestamp
            ) ?: ConversationEntity(
                id = deterministicConvId,
                participantIdsCsv = participantIds.joinToString(","),
                lastMessage = text,
                lastMessageTimestamp = timestamp,
                unreadCount = 0
            )
            conversationDao.insertConversation(updatedConv)

            // 2. Sync to Cloud Firestore in real-time
            val msgData = mapOf(
                "id" to messageId,
                "conversationId" to deterministicConvId,
                "senderId" to myUid,
                "receiverId" to otherUid,
                "text" to text,
                "mediaUrl" to mediaUrl,
                "timestamp" to timestamp,
                "status" to "SENT"
            )

            firestore.collection("conversations")
                .document(deterministicConvId)
                .collection("messages")
                .document(messageId)
                .set(msgData)
                .await()

            val convData = mutableMapOf<String, Any>(
                "id" to deterministicConvId,
                "participantIds" to participantIds,
                "lastMessage" to text,
                "lastMessageTimestamp" to timestamp,
                "lastMessageSenderId" to myUid,
                "updatedAt" to timestamp
            )
            if (otherUid.isNotBlank()) {
                convData["unreadCount_$otherUid"] = FieldValue.increment(1)
            }

            firestore.collection("conversations")
                .document(deterministicConvId)
                .set(convData, SetOptions.merge())
                .await()

            Result.Success(message)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createOrGetConversation(otherUserId: String): Result<Conversation> {
        val myUid = getMyUid()
        if (myUid.isBlank()) return Result.Error(Exception("User not authenticated"))

        val conversationId = getDeterministicConversationId(myUid, otherUserId)
        val timestamp = System.currentTimeMillis()

        return try {
            // 1. Fetch other user profile from Firestore or Room
            var otherUser = userDao.getUserByIdSync(otherUserId)?.toDomain()
            if (otherUser == null && otherUserId.isNotBlank()) {
                try {
                    val userDoc = firestore.collection("users").document(otherUserId).get().await()
                    if (userDoc.exists()) {
                        otherUser = User(
                            id = otherUserId,
                            displayName = userDoc.getString("displayName") ?: "User",
                            email = userDoc.getString("email") ?: "",
                            phoneNumber = userDoc.getString("phoneNumber") ?: "",
                            avatarUrl = userDoc.getString("photoUrl"),
                            isOnline = userDoc.getBoolean("isOnline") ?: false,
                            lastSeen = userDoc.getLong("lastSeen") ?: System.currentTimeMillis(),
                            bio = userDoc.getString("bio") ?: ""
                        )
                        userDao.insertUser(UserEntity.fromDomain(otherUser))
                    }
                } catch (_: Exception) {
                }
            }

            // 2. Create/Merge conversation document in Firestore
            val convData = mapOf(
                "id" to conversationId,
                "participantIds" to listOf(myUid, otherUserId),
                "updatedAt" to timestamp
            )
            firestore.collection("conversations")
                .document(conversationId)
                .set(convData, SetOptions.merge())
                .await()

            // 3. Save to local Room DB
            val existingConv = conversationDao.getConversationById(conversationId)
            val conversation = if (existingConv != null) {
                existingConv.toDomain(otherUser)
            } else {
                val newConv = Conversation(
                    id = conversationId,
                    participantIds = listOf(myUid, otherUserId),
                    lastMessage = "",
                    lastMessageTimestamp = timestamp,
                    unreadCount = 0,
                    otherUser = otherUser ?: User(id = otherUserId, displayName = "Chat")
                )
                conversationDao.insertConversation(ConversationEntity.fromDomain(newConv))
                newConv
            }

            Result.Success(conversation)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markMessagesAsRead(conversationId: String): Result<Unit> {
        val myUid = getMyUid()
        if (myUid.isBlank()) return Result.Success(Unit)

        val participantIds = if (conversationId.contains("_")) conversationId.split("_").filter { it.isNotBlank() } else emptyList()
        val deterministicConvId = if (participantIds.size >= 2) {
            getDeterministicConversationId(participantIds[0], participantIds[1])
        } else conversationId

        try {
            // Reset unread count in Firestore for current user
            firestore.collection("conversations")
                .document(deterministicConvId)
                .set(mapOf("unreadCount_$myUid" to 0), SetOptions.merge())

            // Reset local Room conversation unread count
            val localConv = conversationDao.getConversationById(deterministicConvId)
            if (localConv != null && localConv.unreadCount != 0) {
                conversationDao.insertConversation(localConv.copy(unreadCount = 0))
            }

            // Mark incoming messages as SEEN in Firestore
            repositoryScope.launch {
                try {
                    val unreadSnapshot = firestore.collection("conversations")
                        .document(deterministicConvId)
                        .collection("messages")
                        .get()
                        .await()

                    for (doc in unreadSnapshot.documents) {
                        val senderId = doc.getString("senderId") ?: ""
                        if (senderId != myUid && doc.getString("status") != "SEEN") {
                            doc.reference.set(mapOf("status" to "SEEN"), SetOptions.merge())
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }

        return Result.Success(Unit)
    }
}
