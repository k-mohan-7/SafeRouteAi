package com.saferoute.ai.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saferoute.ai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.login(email, password)
                .onSuccess { _uiState.value = AuthUiState(success = true) }
                .onFailure { _uiState.value = AuthUiState(error = it.message) }
        }
    }

    fun register(
        fullname: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.register(fullname, email, phone, password, confirmPassword)
                .onSuccess { _uiState.value = AuthUiState(success = true) }
                .onFailure { _uiState.value = AuthUiState(error = it.message) }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
