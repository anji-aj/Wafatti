package com.aj.wafatti.domain.repository

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserFlow(userId: String): Flow<User?>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun checkWafattiUsersByPhones(phoneNumbers: List<String>): Result<Map<String, User>>
    suspend fun updateUserProfile(displayName: String, avatarUrl: String?, bio: String, phoneNumber: String = ""): Result<User>
    suspend fun updateUserPresence(isOnline: Boolean)
    suspend fun saveContactName(userId: String, customName: String?): Result<Unit>
}
