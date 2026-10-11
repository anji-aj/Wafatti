package com.aj.wafatti.feature.contacts

import android.Manifest
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aj.wafatti.core.common.Result
import com.aj.wafatti.core.utils.PermissionUtils
import com.aj.wafatti.data.repository.DeviceContactsReader
import com.aj.wafatti.domain.model.ContactFilter
import com.aj.wafatti.domain.model.DeviceContact
import com.aj.wafatti.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val deviceContactsReader: DeviceContactsReader,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    fun checkPermissionStatus(context: Context, isPermanentlyDenied: Boolean = false) {
        val granted = PermissionUtils.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)
        if (granted) {
            _uiState.update { it.copy(permissionStatus = PermissionStatus.GRANTED) }
            loadDeviceContacts()
        } else if (isPermanentlyDenied) {
            _uiState.update { it.copy(permissionStatus = PermissionStatus.PERMANENTLY_DENIED) }
        } else {
            // Rationale explanation step before showing system dialog
            _uiState.update { it.copy(permissionStatus = PermissionStatus.SHOW_RATIONALE) }
        }
    }

    fun onAllowRationaleClicked() {
        // User agreed to in-app explanation, proceed to system dialog
        _uiState.update { it.copy(permissionStatus = PermissionStatus.NOT_REQUESTED) }
    }

    fun onNotNowClicked() {
        _uiState.update { it.copy(permissionStatus = PermissionStatus.DENIED) }
    }

    fun onPermissionResult(granted: Boolean, isPermanentlyDenied: Boolean = false) {
        if (granted) {
            _uiState.update { it.copy(permissionStatus = PermissionStatus.GRANTED, error = null) }
            loadDeviceContacts()
        } else {
            val status = if (isPermanentlyDenied) PermissionStatus.PERMANENTLY_DENIED else PermissionStatus.DENIED
            _uiState.update { it.copy(permissionStatus = status, isLoading = false) }
        }
    }

    fun loadDeviceContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isCheckingMatching = true, error = null) }

            val rawContacts = deviceContactsReader.readContacts()
            if (rawContacts.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, isCheckingMatching = false, contacts = emptyList()) }
                return@launch
            }

            // Perform Wafatti Contact Matching without exposing entire DB
            val phoneNumbers = rawContacts.map { it.phoneNumber }
            when (val matchingResult = userRepository.checkWafattiUsersByPhones(phoneNumbers)) {
                is Result.Success -> {
                    val wafattiUsersMap = matchingResult.data
                    val updatedContacts = rawContacts.map { contact ->
                        val matchedUser = wafattiUsersMap[contact.phoneNumber] ?: wafattiUsersMap[contact.normalizedPhone]
                        if (matchedUser != null) {
                            if (matchedUser.savedName.isNullOrBlank() && contact.name.isNotBlank()) {
                                userRepository.saveContactName(matchedUser.id, contact.name)
                            }
                            contact.copy(
                                isWafattiUser = true,
                                wafattiUserId = matchedUser.id
                            )
                        } else {
                            contact.copy(
                                isWafattiUser = false,
                                wafattiUserId = null
                            )
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCheckingMatching = false,
                            contacts = updatedContacts,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    // Show error state with Retry option
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCheckingMatching = false,
                            contacts = rawContacts,
                            error = "Unable to check Wafatti contacts."
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChanged(filter: ContactFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun retryMatching() {
        loadDeviceContacts()
    }
}
