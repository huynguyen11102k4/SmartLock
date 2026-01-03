package com.example.smartlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlock.model.entity.User
import com.example.smartlock.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel(){
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun getUserProfile(){
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            userRepository.getUserProfile()
                .onSuccess { user ->
                    _currentUser.value = user
                    _uiState.value = UserUiState.Success
                    if (user.avatarUrl.isNullOrEmpty()) {
                        autoGenerateRandomAvatar()
                    }
                }
                .onFailure {
                    _uiState.value = UserUiState.Error(it.message ?: "Failed to load profile")
                }
        }
    }

    private fun autoGenerateRandomAvatar() {
        viewModelScope.launch {
            userRepository.updateAvatar(null, isRandom = true)
                .onSuccess {
                    getUserProfile()
                }
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            userRepository.updateName(name)
                .onSuccess {
                    _uiState.value = UserUiState.ProfileUpdated
                    getUserProfile()
                }
                .onFailure {
                    _uiState.value = UserUiState.Error(it.message ?: "Failed to update name")
                }
        }
    }

    fun updateAvatar(filePart: MultipartBody.Part?, isRandom: Boolean) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            userRepository.updateAvatar(filePart, isRandom)
                .onSuccess {
                    _uiState.value = UserUiState.ProfileUpdated
                    getUserProfile()
                }
                .onFailure {
                    _uiState.value = UserUiState.Error(it.message ?: "Lỗi kết nối")
                }
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            userRepository.updatePhoneNumber(phoneNumber)
                .onSuccess {
                    _uiState.value = UserUiState.ProfileUpdated
                    getUserProfile()
                }
                .onFailure {
                    _uiState.value = UserUiState.Error(it.message ?: "Failed to update phone number")
                }
        }
    }

    fun updateDateOfBirth(dateOfBirth: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            userRepository.updateDateOfBirth(dateOfBirth)
                .onSuccess {
                    _uiState.value = UserUiState.ProfileUpdated
                    getUserProfile()
                }
                .onFailure {
                    _uiState.value = UserUiState.Error(it.message ?: "Failed to update date of birth")
                }
        }
    }

    fun resetState() {
        _uiState.value = UserUiState.Idle
    }
}

sealed class UserUiState {
    object Idle : UserUiState()
    object Loading : UserUiState()
    object Success : UserUiState()
    object ProfileUpdated : UserUiState()
    data class Error(val message: String) : UserUiState()
}