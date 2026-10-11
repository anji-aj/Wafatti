package com.aj.wafatti.domain.model

data class User(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val bio: String = "",
    val savedName: String? = null
) {
    val effectiveDisplayName: String
        get() = savedName?.trim()?.takeIf { it.isNotBlank() }
            ?: displayName.trim().takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
}
