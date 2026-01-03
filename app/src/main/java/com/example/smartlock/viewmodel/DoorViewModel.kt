package com.example.smartlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlock.model.entity.Door
import com.example.smartlock.model.entity.DoorRecord
import com.example.smartlock.model.entity.DoorShare
import com.example.smartlock.repository.DoorRepository
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
class DoorViewModel @Inject constructor(
    private val doorRepository: DoorRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<DoorUiState>(DoorUiState.Idle)
    val uiState: StateFlow<DoorUiState> = _uiState

    val doors: StateFlow<List<Door>> = doorRepository.getDoorsFromDb()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDoorId = MutableStateFlow<String?>(null)

    private val _currentRecord = MutableStateFlow<DoorRecord?>(null)
    val currentRecord: StateFlow<DoorRecord?> = _currentRecord.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDoor: StateFlow<Door?> = _currentDoorId
        .filterNotNull()
        .flatMapLatest { doorId ->
            doorRepository.getDoorById(doorId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val doorRecords: StateFlow<List<DoorRecord>> = _currentDoorId
        .filterNotNull()
        .flatMapLatest { doorId ->
            doorRepository.getRecordsFromDb()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val doorShares: StateFlow<List<DoorShare>> = _currentDoorId
        .filterNotNull()
        .flatMapLatest { doorId ->
            doorRepository.getSharesFromDb(doorId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadDoor() {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.syncDoors()
                .onSuccess { doors ->
                    _uiState.value = DoorUiState.Success
                    if (doors.isEmpty()) {
                        createDoor(
                            doorCode = "DEMO_001",
                            name = "My First Door",
                            mqttTopicPrefix = "door/123",
                            macAddress = "00:00:00:00:00:00"
                        )
                    }
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to load doors")
                }
        }
    }

    fun setCurrentDoor(doorId: String){
        _currentDoorId.value = doorId
    }

    fun createDoor(doorCode: String, name: String, mqttTopicPrefix: String, macAddress: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.createDoor(doorCode, name, mqttTopicPrefix, macAddress)
                .onSuccess {
                    _uiState.value = DoorUiState.DoorCreated
                    loadDoor()  // Refresh danh sách
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to create door")
                }
        }
    }

    fun refreshDoorDetails(doorId: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.getDoor(doorId)
                .onSuccess {
                    _uiState.value = DoorUiState.Success
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to refresh door details")
                }
        }
    }

    fun updateDoor(doorId: String, name: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.updateDoor(doorId, name)
                .onSuccess {
                    _uiState.value = DoorUiState.DoorUpdated
                    loadDoor()
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to update door")
                }
        }
    }

    fun deleteDoor(doorId: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.deleteDoor(doorId)
                .onSuccess {
                    _uiState.value = DoorUiState.DoorDeleted
                    loadDoor()
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to delete door")
                }
        }
    }

    fun updateDoorCode(doorId: String, newCode: String) {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.updateDoorCode(doorId, newCode)
                .onSuccess {
                    _uiState.value = DoorUiState.DoorCodeUpdated
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Lỗi hệ thống")
                }
        }
    }

    fun lockDoor(doorId: String) {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.lockDoor(doorId)
                .onSuccess {
                    // Vì Repository trả về Result<Unit>, vào đây là đã thành công
                    _uiState.value = DoorUiState.CommandSuccess("Đã gửi lệnh khóa cửa thành công")
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Không thể khóa cửa")
                }
        }
    }

    fun unlockDoor(doorId: String) {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.unlockDoor(doorId)
                .onSuccess {
                    _uiState.value = DoorUiState.CommandSuccess("Đã gửi lệnh mở khóa thành công")
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Không thể mở khóa cửa")
                }
        }
    }

    fun syncDoorStatus(doorId: String) {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.syncDoorStatus(doorId)
                .onSuccess {
                    _uiState.value = DoorUiState.CommandSuccess("Đã cập nhật trạng thái cửa")
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Lỗi đồng bộ")
                }
        }
    }

    fun loadRecords(doorId: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.syncRecords(doorId)
                .onSuccess {
                    _uiState.value = DoorUiState.Success
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to load records")
                }
        }
    }


    fun getDoorRecord(doorId: String, recordId: String) {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.getDoorRecord(doorId, recordId)
                .onSuccess { record ->
                    _currentRecord.value = record
                    _uiState.value = DoorUiState.Success
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to load record")
                }
        }
    }

    fun loadShares(doorId: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.syncShares(doorId)
                .onSuccess {
                    _uiState.value = DoorUiState.Success
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to load shares")
                }
        }
    }

    fun shareDoor(doorId: String, userId: String, permission: Int, validFrom: String? = null, validTo: String? = null){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.shareDoor(doorId, userId, permission, validFrom, validTo)
                .onSuccess {
                    _uiState.value = DoorUiState.Success
                    loadShares(doorId)
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to share door")
                }
        }
    }

    fun updateShare(
        doorId: String,
        userId: String,
        permission: Int,
        validFrom: String? = null,
        validTo: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.updateShare(doorId, userId, permission, validFrom, validTo)
                .onSuccess {
                    _uiState.value = DoorUiState.Success
                    loadShares(doorId)
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to update share")
                }
        }
    }

    fun revokeShare(doorId: String, userId: String){
        viewModelScope.launch {
            _uiState.value = DoorUiState.Loading
            doorRepository.revokeShare(doorId, userId)
                .onSuccess {
                    _uiState.value = DoorUiState.ShareRevoked
                    loadShares(doorId)
                }
                .onFailure {
                    _uiState.value = DoorUiState.Error(it.message ?: "Failed to revoke share")
                }
        }
    }

    fun resetState(){
        _uiState.value = DoorUiState.Idle
    }
}

sealed class DoorUiState{
    object Idle: DoorUiState()
    object Loading: DoorUiState()
    object Success: DoorUiState()
    object DoorCreated: DoorUiState()
    object DoorUpdated: DoorUiState()
    object DoorDeleted: DoorUiState()
    object ShareRevoked: DoorUiState()
    object DoorCodeUpdated: DoorUiState()
    data class CommandSuccess(val message: String): DoorUiState()
    data class Error(val message: String): DoorUiState()
}