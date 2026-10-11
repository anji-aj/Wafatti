package com.aj.wafatti.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aj.wafatti.core.common.Result
import com.aj.wafatti.domain.usecase.AuthenticateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authenticateUserUseCase: AuthenticateUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authenticateUserUseCase.signIn(email, password)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, user = result.data) }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun signUp(displayName: String, email: String, phoneNumber: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authenticateUserUseCase.signUp(displayName, email, phoneNumber, password)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, user = result.data) }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isPasswordResetSent = false) }
            when (val result = authenticateUserUseCase.sendPasswordResetEmail(email)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isPasswordResetSent = true, error = null) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, isPasswordResetSent = false, error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun resetPasswordResetState() {
        _uiState.update { it.copy(isPasswordResetSent = false, error = null) }
    }
}
