package com.aj.wafatti.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.repository.AuthRepository
import com.aj.wafatti.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: "me"
            userRepository.getUserFlow(uid).collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            displayNameInput = user.displayName,
                            phoneNumberInput = user.phoneNumber,
                            bioInput = user.bio
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayNameInput = name) }
    }

    fun onPhoneNumberChanged(phone: String) {
        _uiState.update { it.copy(phoneNumberInput = phone) }
    }

    fun onBioChanged(bio: String) {
        _uiState.update { it.copy(bioInput = bio) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isSuccess = false) }
            val current = _uiState.value
            val result = userRepository.updateUserProfile(
                displayName = current.displayNameInput.ifBlank { current.user?.displayName ?: "User" },
                avatarUrl = current.user?.avatarUrl,
                bio = current.bioInput,
                phoneNumber = current.phoneNumberInput
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, user = result.data, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }
}
