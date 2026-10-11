package com.aj.wafatti.domain.usecase

import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.model.User
import com.aj.wafatti.domain.repository.AuthRepository
import javax.inject.Inject

class AuthenticateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun signIn(email: String, password: String): Result<User> {
        return authRepository.signInWithEmail(email, password)
    }

    suspend fun signUp(displayName: String, email: String, phoneNumber: String, password: String): Result<User> {
        return authRepository.signUpWithEmail(displayName, email, phoneNumber, password)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authRepository.sendPasswordResetEmail(email)
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    suspend fun signOut() {
        authRepository.signOut()
    }
}
