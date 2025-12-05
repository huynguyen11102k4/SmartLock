package com.example.smartlock.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlock.MqttClientManager
import com.example.smartlock.model.Door
import com.example.smartlock.model.ICCard
import com.example.smartlock.model.Passcode
import com.example.smartlock.repository.DoorRepository
import com.example.smartlock.repository.ICCardRepository
import com.example.smartlock.repository.PasscodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DoorViewModel(context: Context) : ViewModel() {

    private val doorRepo = DoorRepository(context)
    private val passcodeRepo = PasscodeRepository(context)
    private val icCardRepo = ICCardRepository(context)

    val doors: StateFlow<List<com.example.smartlock.model.Door>> = doorRepo.allDoors.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _masterPasscode = MutableStateFlow<String?>(null)
    val masterPasscode: StateFlow<String?> = _masterPasscode
    val icCards: MutableStateFlow<List<ICCard>> = MutableStateFlow(emptyList())

    fun getDoorById(doorId: String): Flow<Door?> =
        doorRepo.allDoors
            .map { doors -> doors.find { d -> d.id == doorId } }

    fun insertDoor(door: Door) = viewModelScope.launch {
        doorRepo.insert(door)
    }

    fun updateDoor(door: Door) = viewModelScope.launch {
        doorRepo.update(door)
    }

    fun deleteDoor(door: Door) = viewModelScope.launch {
        doorRepo.delete(door)
    }

    fun deleteByIdDoor(doorId: String) = viewModelScope.launch {
        doorRepo.deleteById(doorId)
    }

    fun getMasterPasscode(doorId: String): String? {
        return doors.value.find { it.id == doorId }?.masterPasscode
    }

    fun updateMasterPasscode(doorId: String, newCode: String) = viewModelScope.launch {
        doorRepo.updateMasterPasscode(doorId, newCode)
    }

    fun subscribeToPasscodeList(doorId: String) {
        val door = doors.value.find { it.id == doorId } ?: return

        MqttClientManager.subscribe("${door.mqttTopicPrefix}/passcodes/list") { publish ->
            try {
                val jsonArray = JSONArray(String(publish.payloadAsBytes))
                val tempPasscodes = mutableListOf<Passcode>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val code = obj.getString("code")
                    val type = obj.getString("type")
                    val validity = obj.optString("validity", "")

                    if (type == "permanent") {
                        viewModelScope.launch {
                            doorRepo.updateMasterPasscode(doorId, code)
                        }
                    } else {
                        val passcode = Passcode(
                            code = code,
                            doorId = doorId,
                            type = type,
                            validity = validity,
                            status = "Active"
                        )
                        tempPasscodes.add(passcode)
                    }
                }

                viewModelScope.launch {
                    passcodeRepo.deleteAllForDoor(doorId)
                    tempPasscodes.forEach { passcodeRepo.insert(it) }
                }

            } catch (e: Exception) {
                Log.e("MQTT", "Parse passcode list error", e)
            }
        }
    }

    fun addEKey(doorId: String, passcode: Passcode) {
        val door = doors.value.find { it.id == doorId } ?: return
        val payload = JSONObject().apply {
            put("action", "add")
            put("code", passcode.code)
            put("type", passcode.type)
            if (passcode.type == "timed") put("validity", passcode.validity)
        }.toString()
        MqttClientManager.publish("${door.mqttTopicPrefix}/passcodes", payload)
        viewModelScope.launch {
            passcodeRepo.insert(passcode)
        }
    }

    fun deleteEKey(code: String, doorId: String) {
        val door = doors.value.find { it.id == doorId } ?: return
        val payload = JSONObject().apply {
            put("action", "delete")
            put("code", code)
        }.toString()
        MqttClientManager.publish("${door.mqttTopicPrefix}/passcodes", payload)
        viewModelScope.launch {
            passcodeRepo.delete(code, doorId)
        }
    }

    fun getEKeysForDoor(doorId: String): Flow<List<Passcode>> =
        passcodeRepo.getPasscodesForDoor(doorId)

    fun syncICCards(doorId: String) {
        val door = doors.value.find { it.id == doorId } ?: return
        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards/request", "{}")
    }

    fun icCardsForDoor(doorId: String): StateFlow<List<ICCard>> =
        icCardRepo.getCardsForDoor(doorId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addICCard(doorId: String, card: ICCard) {
        val door = doors.value.find { it.id == doorId } ?: return
        val json = JSONObject().apply {
            put("action", "add")
            put("id", card.id)
            put("name", card.name)
        }.toString()
        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards", json)
        icCards.update { it + card }
    }

    fun deleteICCard(doorId: String, id: String) {
        val door = doors.value.find { it.id == doorId } ?: return
        val json = JSONObject().put("action", "delete").put("id", id).toString()
        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards", json)
        icCards.update { it.filter { c -> c.id != id } }
    }

    fun requestSync(doorId: String) = viewModelScope.launch {
        val door = doors.value.find { it.id == doorId } ?: return@launch
        MqttClientManager.publish("${door.mqttTopicPrefix}/passcodes/request", "{}")
        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards/request", "{}")
    }

    fun updateMasterPasscodeFromMqtt(code: String) {
        _masterPasscode.value = code
    }
}
