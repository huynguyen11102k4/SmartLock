package com.example.smartlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlock.model.entity.Passcode
import com.example.smartlock.repository.PasscodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeViewModel @Inject constructor(
    private val passcodeRepository: PasscodeRepository
) : ViewModel(){
    private val _uiState = MutableStateFlow<PasscodeUiState>(PasscodeUiState.Idle)
    val uiState: StateFlow<PasscodeUiState> = _uiState.asStateFlow()

    private val _currentDoorId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val passcodes: StateFlow<List<Passcode>> = _currentDoorId
        .filterNotNull()
        .flatMapLatest { doorId ->
            passcodeRepository.getPasscodesFromDb(doorId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrentDoor(doorId: String) {
        _currentDoorId.value = doorId
    }

    fun loadPasscodes(doorId: String){
        _currentDoorId.value = doorId
        viewModelScope.launch {
            _uiState.value = PasscodeUiState.Loading
            passcodeRepository.syncPasscodes(doorId)
                .onSuccess {
                    _uiState.value = PasscodeUiState.Success
                }
                .onFailure {
                    _uiState.value = PasscodeUiState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun addPasscode(doorId: String, code: String, type: Int, validFrom: String?, validTo: String?) {
        viewModelScope.launch {
            _uiState.value = PasscodeUiState.Loading
            passcodeRepository.addPasscode(doorId, code, type, validFrom, validTo)
                .onSuccess {
                    _uiState.value = PasscodeUiState.PasscodeAdded
                }
                .onFailure {
                    _uiState.value = PasscodeUiState.Error(it.message ?: "Thêm mã số thất bại")
                }
        }
    }

    fun updatePasscode(doorId: String, code: String, type: Int, validFrom: String?, validTo: String?) {
        viewModelScope.launch {
            _uiState.value = PasscodeUiState.Loading
            passcodeRepository.updatePasscode(doorId, code, type, validFrom, validTo)
                .onSuccess {
                    _uiState.value = PasscodeUiState.PasscodeUpdated
                }
                .onFailure {
                    _uiState.value = PasscodeUiState.Error(it.message ?: "Cập nhật mã số thất bại")
                }
        }
    }

    fun deletePasscode(doorId: String, code: String) {
        viewModelScope.launch {
            _uiState.value = PasscodeUiState.Loading
            passcodeRepository.deletePasscode(doorId, code)
                .onSuccess {
                    _uiState.value = PasscodeUiState.PasscodeDeleted
                }
                .onFailure {
                    _uiState.value = PasscodeUiState.Error(it.message ?: "Xóa mã số thất bại")
                }
        }
    }

    fun requestSync(doorId: String) {
        viewModelScope.launch {
            _uiState.value = PasscodeUiState.Loading
            passcodeRepository.requestSync(doorId)
                .onSuccess {
                    _uiState.value = PasscodeUiState.SyncRequested("Yêu cầu đồng bộ đã được gửi")
                }
                .onFailure {
                    _uiState.value = PasscodeUiState.Error(it.message ?: "Yêu cầu đồng bộ thất bại")
                }
        }
    }

    fun resetState(){
        _uiState.value = PasscodeUiState.Idle
    }
}

sealed class PasscodeUiState {
    object Idle: PasscodeUiState()
    object Loading: PasscodeUiState()
    object Success: PasscodeUiState()
    object PasscodeAdded: PasscodeUiState()
    object PasscodeUpdated: PasscodeUiState()
    object PasscodeDeleted: PasscodeUiState()
    data class SyncRequested(val message: String): PasscodeUiState()
    data class Error(val message: String): PasscodeUiState()
}