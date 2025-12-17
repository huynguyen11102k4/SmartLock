package com.example.smartlock.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlock.MqttClientManager
import com.example.smartlock.MqttService
import com.example.smartlock.model.Door
import com.example.smartlock.model.ICCard
import com.example.smartlock.model.Passcode
import com.example.smartlock.model.Record
import com.example.smartlock.repository.DoorRepository
import com.example.smartlock.repository.ICCardRepository
import com.example.smartlock.repository.PasscodeRepository
import com.example.smartlock.repository.RecordRepository
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
import java.util.Date
import java.util.UUID

class DoorViewModel(context: Context) : ViewModel() {

    private val doorRepo = DoorRepository(context)
    private val passcodeRepo = PasscodeRepository(context)
    private val icCardRepo = ICCardRepository(context)
    private val recordRepo = RecordRepository(context)

    val doors: StateFlow<List<Door>> = doorRepo.allDoors.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _masterPasscode = MutableStateFlow<String?>(null)
    val masterPasscode: StateFlow<String?> = _masterPasscode
    val icCards: MutableStateFlow<List<ICCard>> = MutableStateFlow(emptyList())
    val records: MutableStateFlow<List<Record>> = MutableStateFlow(emptyList())

    private val mqttService: MqttService? = MqttService.getInstance()

    private val _currentStates = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentStates: StateFlow<Map<String, String>> = _currentStates

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

        if (mqttService != null && MqttService.isRunning) {
            mqttService.subscribe("${door.mqttTopicPrefix}/passcodes/list") { publish ->
                try {
                    val payload = String(publish.payloadAsBytes)
                    Log.d("MQTT_PASSCODE", "Passcode list received from ${door.id}: $payload")

                    val jsonArray = JSONArray(payload)
                    val tempPasscodes = mutableListOf<Passcode>()

                    Log.d("MQTT_PASSCODE", "Attempting to parse total ${jsonArray.length()} items.")

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val code = obj.getString("code")
                        val type = obj.getString("type")
                        val validity = obj.optString("validity", "")

                        if (type == "permanent") {
                            Log.d("MQTT_PASSCODE", "Found permanent passcode (Master): $code")
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
                            Log.d("MQTT_PASSCODE", "Found EKey: $code (Type: $type). Total temp found so far: ${tempPasscodes.size}") // Log chi tiết
                        }
                    }

                    Log.d("MQTT_PASSCODE", "Finished parsing. Found ${tempPasscodes.size} EKeys to insert.")

                    viewModelScope.launch {
                        Log.d("MQTT_PASSCODE", "Starting DB sync for door $doorId.")
                        passcodeRepo.deleteAllForDoor(doorId)
                        Log.d("MQTT_PASSCODE", "Deleted all existing EKeys for $doorId.")

                        tempPasscodes.forEach { passcodeRepo.insert(it) }
                        Log.d("MQTT_PASSCODE", "Inserted ${tempPasscodes.size} EKeys from MQTT.")
                    }

                } catch (e: Exception) {
                    Log.e("MQTT_PASSCODE", "Critical JSON parsing error in subscribeToPasscodeList: ${e.message}", e)
                }
            }
        } else {
            Log.w("MQTT", "MqttService not running, cannot subscribe to passcodes/list")
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
//        requestPasscodeSync(doorId)
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

    fun requestPasscodeSync(doorId: String){
        val door = doors.value.find { it.id == doorId } ?: return
        MqttClientManager.publish("${door.mqttTopicPrefix}/passcodes/request", "{}")
    }

    fun subscribeToICCardList(doorId: String) {
        val door = doors.value.find { it.id == doorId } ?: return

        if (mqttService != null && MqttService.isRunning) {
            mqttService.subscribe("${door.mqttTopicPrefix}/iccards/list") { publish ->
                try {
                    val jsonArray = JSONArray(String(publish.payloadAsBytes))
                    val cards = mutableListOf<ICCard>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.getString("id")
                        val name = obj.optString("name", "Card #$id")

                        cards.add(ICCard(id = id, doorId = doorId, name = name, status = "Active"))
                    }

                    viewModelScope.launch {
                        icCardRepo.deleteAllForDoor(doorId)
                        cards.forEach { icCardRepo.insert(it) }
                    }
                } catch (e: Exception) {
                    Log.e("MQTT", "Parse ICCard list error", e)
                }
            }
        } else {
            Log.w("MQTT", "MqttService not running, cannot subscribe to iccards/list")
        }
    }

    fun getICCardsForDoor(doorId: String): Flow<List<ICCard>> =
        icCardRepo.getCardsForDoor(doorId)

    fun addICCard(doorId: String, card: ICCard) {
        val door = doors.value.find { it.id == doorId } ?: return
        val payload = JSONObject().apply {
            put("action", "add")
            put("id", card.id)
            put("name", card.name)
        }.toString()

        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards", payload)

        viewModelScope.launch {
            icCardRepo.insert(card.copy(doorId = doorId))
        }
    }

    fun deleteICCard(doorId: String, cardId: String) {
        val door = doors.value.find { it.id == doorId } ?: return
        val payload = JSONObject().apply {
            put("action", "delete")
            put("id", cardId)
        }.toString()

        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards", payload)

        viewModelScope.launch {
            icCardRepo.delete(cardId, doorId)
        }
    }

    fun requestICCardSync(doorId: String){
        val door = doors.value.find { it.id == doorId } ?: return
        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards/request", "{}")
    }

    fun getRecordsForDoor(doorId: String): Flow<List<Record>> =
        recordRepo.getRecordsForDoor(doorId)

    fun subscribeToRecords(doorId: String) {
        val door = doors.value.find { it.id == doorId } ?: return

        if (mqttService != null && MqttService.isRunning) {
            mqttService.subscribe("${door.mqttTopicPrefix}/log") { message ->
                try {
                    val payload = String(message.payloadAsBytes)
                    Log.d("MQTT_RECORDS", "Log message received: $payload")
                    val json = JSONObject(payload)
                    val event = json.optString("event", "unknown_event")
                    val method = json.optString("method", "unknown")
                    val detail = json.optString("detail", "")

                    val inferredState = when {
                        event == "unlock" || method.contains("unlock") -> "unlocked"
                        event == "lock" || event == "wrong_pin" || event == "locked" -> "locked"
                        else -> "locked"
                    }

                    val record = Record(
                        id = UUID.randomUUID().toString(),
                        doorId = doorId,
                        timestamp = Date(),
                        event = event,
                        method = method,
                        detail = detail,
                        state = inferredState,
                        sourceMqttMessage = payload
                    )
                    viewModelScope.launch {
                        recordRepo.insert(record)
                    }
                    Log.d("MQTT", "Record log received: $payload" )
                } catch (e: Exception) {
                    Log.e("MQTT", "Parse record log error", e)
                }
            }
        } else {
            Log.w("MQTT", "MqttService not running, cannot subscribe to log")
        }
    }

    fun subscribeToState(doorId: String) {
        val door = doors.value.find { it.id == doorId } ?: return

        if (mqttService != null && MqttService.isRunning) {
            mqttService.subscribe("${door.mqttTopicPrefix}/state") { message ->
                try {
                    val payload = String(message.payloadAsBytes)
                    Log.d("MQTT_RECORDS", "Log message received: $payload")
                    val json = JSONObject(payload)
                    val state = json.optString("state", "locked")
                    val reason = json.optString("reason", "")

                    _currentStates.update { it + (doorId to state) }

                    viewModelScope.launch {
                        val currentDoor = doorRepo.getDoorById(doorId)
                        if (currentDoor != null) {
                            doorRepo.update(currentDoor.copy(currentState = state))
                        }

                        val record = Record(
                            id = UUID.randomUUID().toString(),
                            doorId = doorId,
                            timestamp = Date(),
                            event = "state_change",
                            method = "system",
                            detail = "State: $state (reason: $reason)",
                            state = state,
                            sourceMqttMessage = payload
                        )
                        recordRepo.insert(record)
                    }
                } catch (e: Exception) {
                    Log.e("MQTT", "Parse state error", e)
                }
            }
        } else {
            Log.w("MQTT", "MqttService not running, cannot subscribe to state")
        }
    }

    fun requestSync(doorId: String) = viewModelScope.launch {
        val door = doors.value.find { it.id == doorId } ?: return@launch
        MqttClientManager.publish("${door.mqttTopicPrefix}/passcodes/request", "{}")
        MqttClientManager.publish("${door.mqttTopicPrefix}/iccards/request", "{}")
    }

    fun reSubscribeAll() {
        viewModelScope.launch {
            doorRepo.allDoors.collect { doors ->
                doors.forEach { door ->
                    subscribeToPasscodeList(door.id)
                    subscribeToICCardList(door.id)
                    subscribeToRecords(door.id)
                    subscribeToState(door.id)
//                    requestSync(door.id)
                }
            }
        }
    }

    fun updateMasterPasscodeFromMqtt(code: String) {
        _masterPasscode.value = code
    }
}
