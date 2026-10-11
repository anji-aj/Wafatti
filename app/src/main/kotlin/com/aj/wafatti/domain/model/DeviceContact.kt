package com.aj.wafatti.domain.model

data class DeviceContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val normalizedPhone: String = "",
    val photoUri: String? = null,
    val isWafattiUser: Boolean = false,
    val wafattiUserId: String? = null
)

enum class ContactFilter {
    ALL, WAFATTI, INVITE
}
