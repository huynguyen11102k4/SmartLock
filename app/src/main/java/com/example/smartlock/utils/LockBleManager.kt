package com.example.smartlock.utils

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import java.nio.charset.Charset
import java.util.UUID

class LockBleManager(ctx: Context) : BleManager(ctx) {
    private val SERVICE_LOCK = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHAR_CONFIG  = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    private val CHAR_NOTIFY  = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")

    private var cfg: BluetoothGattCharacteristic? = null
    private var evt: BluetoothGattCharacteristic? = null

    private var ackDeferred: CompletableDeferred<String?>? = null
    private var notificationsReady = CompletableDeferred<Boolean>()

    override fun getGattCallback() = object : BleManagerGattCallback(){
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            Log.d("LockBleManager", "Checking services...")
            val service = gatt.getService(SERVICE_LOCK)
            if (service == null) {
                Log.e("LockBleManager", "Service not found!")
                return false
            }

            cfg = service.getCharacteristic(CHAR_CONFIG)
            evt = service.getCharacteristic(CHAR_NOTIFY)

            if (cfg == null || evt == null) {
                Log.e("LockBleManager", "Characteristics not found! cfg=$cfg, evt=$evt")
                return false
            }

            val props = cfg?.properties ?: 0
            val canWrite = props and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

            Log.d("LockBleManager", "Service supported: canWrite=$canWrite")
            return canWrite
        }

        override fun initialize() {
            Log.d("LockBleManager", "Initializing...")
            notificationsReady = CompletableDeferred()

            requestMtu(247).enqueue()

            evt?.let { notifyChar ->
                setNotificationCallback(notifyChar).with(onNotify)
                enableNotifications(notifyChar)
                    .done {
                        Log.d("LockBleManager", "Notifications ENABLED")
                        notificationsReady.complete(true)
                    }
                    .fail { _, status ->
                        Log.e("LockBleManager", "Failed to enable notifications: $status")
                        notificationsReady.complete(false)
                    }
                    .enqueue()
            }
        }


        override fun onServicesInvalidated() {
            Log.d("LockBleManager", "Services invalidated")
            cfg = null
            evt = null
            notificationsReady = CompletableDeferred()
        }
    }

    private val onNotify = DataReceivedCallback { _, data ->
        val str = data.value?.toString(Charset.forName("UTF-8"))
        Log.d("LockBleManager", "✅ Notification received: $str")
        ackDeferred?.complete(str)
    }

    fun connectTo(d: BluetoothDevice) = connect(d)
        .useAutoConnect(false)
        .timeout(15_000)
        .retry(2, 100)

    suspend fun waitForNotifications(timeoutMs: Long = 5_000): Boolean {
        return try {
            withTimeout(timeoutMs) {
                notificationsReady.await()
            }
        } catch (e: Exception) {
            Log.e("LockBleManager", "Timeout waiting for notifications: ${e.message}")
            false
        }
    }

    suspend fun sendConfigAwaitAck(
        json: String,
        expect: String? = "OK",
        timeoutMs: Long = 15_000
    ): Boolean {
        val characteristic = cfg
        if (characteristic == null) {
            Log.e("LockBleManager", "Config characteristic not found!")
            return false
        }
        delay(800)
        ackDeferred = CompletableDeferred()

        writeCharacteristic(
            characteristic,
            json.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
            .done {
                Log.d("LockBleManager", "Write completed successfully")
            }
            .fail { _, status ->
                Log.e("LockBleManager", "Write failed with status: $status")
                ackDeferred?.complete(null)
            }
            .enqueue()

        return try {
            Log.d("LockBleManager", "⏳ Optionally waiting for ACK (timeout: ${timeoutMs}ms)...")

            val ack = withTimeout(timeoutMs) {
                ackDeferred?.await()
            }
            ackDeferred = null
            if (expect != null && ack != null) {
                val ok = ack == expect
                Log.d("LockBleManager", "Result using ACK: $ok (expected: '$expect', got: '$ack')")
                ok
            } else {
                Log.d("LockBleManager", "No ACK or no expectation, but write was OK -> treat as success")
                true
            }
        } catch (e: Exception) {
            ackDeferred = null
            true
        }
    }
}