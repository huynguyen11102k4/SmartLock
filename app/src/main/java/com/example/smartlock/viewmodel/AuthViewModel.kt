package com.example.smartlock.viewmodel

import com.example.smartlock.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.register(email, password, name)
                .onSuccess {
                    _uiState.value = AuthUiState.OtpSent("OTP đã được gửi đến email")
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Registration failed")
                }
        }
    }

    fun verifyRegisterOtp(email: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.verifyRegisterOtp(email, otp)
                .onSuccess {
                    _uiState.value = AuthUiState.RegisterSuccess
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "OTP verification failed")
                }
        }
    }

    fun resendRegisterOtp(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.resendRegisterOtp(email)
                .onSuccess {
                    _uiState.value = AuthUiState.OtpResent
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to resend OTP")
                }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState.LoginSuccess
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Login failed")
                }
        }
    }

    fun loginWithSocial(type: String, token: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginWithSocial(type, token)
                .onSuccess {
                    _uiState.value = AuthUiState.LoginSuccess
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Login failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.logout()
                .onSuccess {
                    _uiState.value = AuthUiState.LoggedOut
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Logout failed")
                }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.changePassword(oldPassword, newPassword)
                .onSuccess {
                    _uiState.value = AuthUiState.PasswordChanged
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Password change failed")
                }
        }
    }

    fun sendForgotPasswordOtp(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendForgotPasswordOtp(email)
                .onSuccess {
                    _uiState.value = AuthUiState.OtpSent("OTP đã được gửi đến email")
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to send OTP")
                }
        }
    }

    fun verifyForgotPasswordOtp(email: String, otp: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.verifyForgotPasswordOtp(email, otp, newPassword)
                .onSuccess {
                    _uiState.value = AuthUiState.ForgotPasswordVerified
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "OTP verification failed")
                }
        }
    }

    fun linkOAuth(provider: String, token: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.linkOAuth(provider, token)
                .onSuccess {
                    _uiState.value = AuthUiState.OAuthLinked
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Link OAuth failed")
                }
        }
    }

    fun unlinkOAuth(provider: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.unlinkOAuth(provider)
                .onSuccess {
                    _uiState.value = AuthUiState.OAuthUnlinked
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Unlink OAuth failed")
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object RegisterSuccess : AuthUiState()
    object LoginSuccess : AuthUiState()
    object LoggedOut : AuthUiState()
    object PasswordChanged : AuthUiState()
    object OtpResent : AuthUiState()
    object ForgotPasswordVerified : AuthUiState()
    object OAuthLinked : AuthUiState()
    object OAuthUnlinked : AuthUiState()
    data class OtpSent(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}