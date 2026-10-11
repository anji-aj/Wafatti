package com.aj.wafatti.domain.repository

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<User?>
    fun getCurrentUserId(): String?
    fun isUserLoggedIn(): Boolean
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(displayName: String, email: String, phoneNumber: String, password: String): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut()
}
