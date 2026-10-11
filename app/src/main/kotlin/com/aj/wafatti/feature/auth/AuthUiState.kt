package com.aj.wafatti.feature.auth

import com.aj.wafatti.domain.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isPasswordResetSent: Boolean = false
)
