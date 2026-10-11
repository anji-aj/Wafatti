package com.aj.wafatti.feature.contacts

import com.aj.wafatti.domain.model.ContactFilter
import com.aj.wafatti.domain.model.DeviceContact

enum class PermissionStatus {
    NOT_REQUESTED,
    SHOW_RATIONALE,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

data class ContactsUiState(
    val isLoading: Boolean = false,
    val permissionStatus: PermissionStatus = PermissionStatus.NOT_REQUESTED,
    val searchQuery: String = "",
    val filter: ContactFilter = ContactFilter.ALL,
    val contacts: List<DeviceContact> = emptyList(),
    val isCheckingMatching: Boolean = false,
    val error: String? = null
)
