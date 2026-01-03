package com.example.smartlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlock.model.entity.ICCard
import com.example.smartlock.repository.ICCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ICCardViewModel @Inject constructor(
    private val icCardRepository: ICCardRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<ICCardUiState>(ICCardUiState.Idle)
    val uiState: StateFlow<ICCardUiState> = _uiState

    private val _currentDoorId = MutableStateFlow<String?>(null)

    val icCards: StateFlow<List<ICCard>> = _currentDoorId
        .filterNotNull()
        .flatMapLatest { doorId ->
            icCardRepository.getICCardsFromDb(doorId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCurrentDoor(doorId: String) {
        _currentDoorId.value = doorId
    }

    fun loadICCards(doorId: String) {
        _currentDoorId.value = doorId
        viewModelScope.launch {
            _uiState.value = ICCardUiState.Loading
            icCardRepository.syncICCards(doorId)
                .onSuccess {
                    _uiState.value = ICCardUiState.Success
                }
                .onFailure {
                    _uiState.value = ICCardUiState.Error(it.message ?: "Failed to load")
                }
        }
    }

    fun addICCard(doorId: String, cardUid: String, name: String) {
        viewModelScope.launch {
            _uiState.value = ICCardUiState.Loading
            icCardRepository.addICCard(doorId, cardUid, name)
                .onSuccess { success ->
                    _uiState.value = if (success) {
                        ICCardUiState.CardAdded
                    } else {
                        ICCardUiState.Error("Add card failed")
                    }
                    loadICCards(doorId)
                }
                .onFailure {
                    _uiState.value = ICCardUiState.Error(it.message ?: "Failed to add")
                }
        }
    }

    fun deleteICCard(doorId: String, cardUid: String) {
        viewModelScope.launch {
            _uiState.value = ICCardUiState.Loading
            icCardRepository.deleteICCard(doorId, cardUid)
                .onSuccess {
                    _uiState.value = ICCardUiState.CardDeleted
                    loadICCards(doorId)
                }
                .onFailure {
                    _uiState.value = ICCardUiState.Error(it.message ?: "Failed to delete")
                }
        }
    }

    fun startSwipeAdd(doorId: String) {
        viewModelScope.launch {
            _uiState.value = ICCardUiState.Loading
            icCardRepository.startSwipeAdd(doorId)
                .onSuccess { success ->
                    _uiState.value = if (success) {
                        ICCardUiState.SwipeAddMode("Swipe Add Mode started. Please swipe the IC card on the reader.")
                    } else {
                        ICCardUiState.Error("Start swipe mode failed")
                    }
                }
                .onFailure {
                    _uiState.value = ICCardUiState.Error(it.message ?: "Failed to start")
                }
        }
    }

    fun requestSync(doorId: String) {
        viewModelScope.launch {
            _uiState.value = ICCardUiState.Loading
            icCardRepository.requestSync(doorId)
                .onSuccess {
                    _uiState.value = ICCardUiState.SyncRequested("Yêu cầu đồng bộ đã được gửi tới thiết bị")
                }
                .onFailure {
                    _uiState.value = ICCardUiState.Error(it.message ?: "Đồng bộ thất bại")
                }
        }
    }

    fun resetState() {
        _uiState.value = ICCardUiState.Idle
    }
}

sealed class ICCardUiState {
    object Idle : ICCardUiState()
    object Loading : ICCardUiState()
    object Success : ICCardUiState()
    object CardAdded : ICCardUiState()
    object CardDeleted : ICCardUiState()
    data class SwipeAddMode(val message: String) : ICCardUiState()
    data class SyncRequested(val message: String) : ICCardUiState()
    data class Error(val message: String) : ICCardUiState()
}