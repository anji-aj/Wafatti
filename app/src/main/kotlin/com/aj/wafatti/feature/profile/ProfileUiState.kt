package com.aj.wafatti.feature.profile

import com.aj.wafatti.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val user: User? = null,
    val displayNameInput: String = "",
    val phoneNumberInput: String = "",
    val bioInput: String = "",
    val isSuccess: Boolean = false,
    val error: String? = null
)
