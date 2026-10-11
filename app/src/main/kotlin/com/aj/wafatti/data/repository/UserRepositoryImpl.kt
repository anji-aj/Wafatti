package com.aj.wafatti.data.repository

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.core.utils.PhoneNumberNormalizer
import com.aj.wafatti.data.local.database.dao.UserDao
import com.aj.wafatti.data.local.database.entity.UserEntity
import com.aj.wafatti.domain.model.User
import com.aj.wafatti.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getUserFlow(userId: String): Flow<User?> {
        if (userId.isNotBlank() && userId != "me") {
            // Attach real-time snapshot listener for target user profile in Firestore
            firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    repositoryScope.launch {
                        val uid = snapshot.getString("uid") ?: snapshot.id
                        val displayName = snapshot.getString("displayName") ?: "User"
                        val email = snapshot.getString("email") ?: ""
                        val phone = snapshot.getString("phoneNumber") ?: ""
                        val photoUrl = snapshot.getString("photoUrl")
                        val bio = snapshot.getString("bio") ?: ""
                        val existingLocal = userDao.getUserByIdSync(uid)

                        val userEntity = UserEntity(
                            id = uid,
                            displayName = displayName,
                            email = email,
                            phoneNumber = phone,
                            avatarUrl = photoUrl,
                            isOnline = existingLocal?.isOnline ?: false,
                            lastSeen = existingLocal?.lastSeen ?: System.currentTimeMillis(),
                            bio = bio,
                            savedName = existingLocal?.savedName
                        )
                        userDao.insertUser(userEntity)
                    }
                }

            // Attach Realtime Database listener for target user presence
            database.getReference("presence/$userId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val connections = snapshot.child("connections")
                        val isOnline = connections.exists() && connections.childrenCount > 0
                        val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: System.currentTimeMillis()

                        repositoryScope.launch {
                            val existing = userDao.getUserByIdSync(userId)
                            if (existing != null) {
                                userDao.insertUser(existing.copy(isOnline = isOnline, lastSeen = lastSeen))
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        return userDao.getUserById(userId).map { it?.toDomain() }
    }

    override suspend fun saveContactName(userId: String, customName: String?): Result<Unit> {
        return try {
            val trimmed = customName?.trim()?.ifBlank { null }
            userDao.updateSavedName(userId, trimmed)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUserPresence(isOnline: Boolean) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val presenceMap = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            firestore.collection("users").document(uid).set(presenceMap, SetOptions.merge()).await()
            val localUser = userDao.getUserByIdSync(uid)
            if (localUser != null) {
                userDao.insertUser(localUser.copy(isOnline = isOnline, lastSeen = System.currentTimeMillis()))
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val entities = userDao.searchUsers(query)
            Result.Success(entities.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun checkWafattiUsersByPhones(phoneNumbers: List<String>): Result<Map<String, User>> {
        return try {
            val resultMap = mutableMapOf<String, User>()

            // Normalize all requested phone numbers
            val normalizedList = phoneNumbers
                .map { PhoneNumberNormalizer.normalize(it) }
                .filter { it.isNotBlank() }
                .distinct()

            if (normalizedList.isNotEmpty()) {
                // Batch query Firestore users collection in chunks of 30 (Firestore limit for whereIn)
                val chunks = normalizedList.chunked(30)
                for (chunk in chunks) {
                    val querySnapshot = firestore.collection("users")
                        .whereIn("normalizedPhoneNumber", chunk)
                        .get()
                        .await()

                    for (doc in querySnapshot.documents) {
                        val uid = doc.getString("uid") ?: doc.id
                        val displayName = doc.getString("displayName") ?: "User"
                        val email = doc.getString("email") ?: ""
                        val phone = doc.getString("phoneNumber") ?: ""
                        val normPhone = doc.getString("normalizedPhoneNumber") ?: PhoneNumberNormalizer.normalize(phone)
                        val photoUrl = doc.getString("photoUrl")
                        val bio = doc.getString("bio") ?: ""
                        val existingLocal = userDao.getUserByIdSync(uid)

                        val matchedUser = User(
                            id = uid,
                            displayName = displayName,
                            email = email,
                            phoneNumber = normPhone.ifBlank { phone },
                            avatarUrl = photoUrl,
                            isOnline = existingLocal?.isOnline ?: false,
                            lastSeen = existingLocal?.lastSeen ?: System.currentTimeMillis(),
                            bio = bio,
                            savedName = existingLocal?.savedName
                        )

                        // Insert/cache matched user in local Room database
                        userDao.insertUser(UserEntity.fromDomain(matchedUser))

                        // Associate with requested phone numbers
                        for (rawPhone in phoneNumbers) {
                            val reqNorm = PhoneNumberNormalizer.normalize(rawPhone)
                            if (PhoneNumberNormalizer.isSameNumber(reqNorm, normPhone)) {
                                resultMap[rawPhone] = matchedUser
                                resultMap[reqNorm] = matchedUser
                                resultMap[normPhone] = matchedUser
                            }
                        }
                    }
                }
            }

            // Also check local Room database as fallback
            val allLocal = userDao.getAllUsers()
            for (localEntity in allLocal) {
                val localNorm = PhoneNumberNormalizer.normalize(localEntity.phoneNumber)
                if (localNorm.isNotBlank()) {
                    for (rawPhone in phoneNumbers) {
                        val reqNorm = PhoneNumberNormalizer.normalize(rawPhone)
                        if (PhoneNumberNormalizer.isSameNumber(reqNorm, localNorm)) {
                            if (!resultMap.containsKey(rawPhone)) {
                                resultMap[rawPhone] = localEntity.toDomain()
                            }
                        }
                    }
                }
            }

            Result.Success(resultMap)
        } catch (e: Exception) {
            Result.Error(Exception("Unable to check Wafatti contacts. ${e.localizedMessage ?: ""}"))
        }
    }

    override suspend fun updateUserProfile(
        displayName: String,
        avatarUrl: String?,
        bio: String,
        phoneNumber: String
    ): Result<User> {
        val uid = firebaseAuth.currentUser?.uid ?: "me"
        val email = firebaseAuth.currentUser?.email ?: ""
        val normPhone = PhoneNumberNormalizer.normalize(phoneNumber)

        return try {
            val profileMap = mutableMapOf<String, Any?>(
                "uid" to uid,
                "displayName" to displayName,
                "email" to email,
                "bio" to bio,
                "isOnline" to true,
                "lastSeen" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            if (avatarUrl != null) {
                profileMap["photoUrl"] = avatarUrl
            }
            if (phoneNumber.isNotBlank()) {
                profileMap["phoneNumber"] = phoneNumber.trim()
                profileMap["normalizedPhoneNumber"] = normPhone
            }

            firestore.collection("users").document(uid).set(profileMap, SetOptions.merge()).await()

            val existingSavedName = userDao.getUserByIdSync(uid)?.savedName

            val user = User(
                id = uid,
                displayName = displayName,
                email = email,
                phoneNumber = normPhone.ifBlank { phoneNumber },
                avatarUrl = avatarUrl,
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                bio = bio,
                savedName = existingSavedName
            )
            userDao.insertUser(UserEntity.fromDomain(user))
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
