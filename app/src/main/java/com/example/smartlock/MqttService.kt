package com.example.smartlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import java.util.function.Consumer
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish

class MqttService : Service() {
    companion object{
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "mqtt_service_channel"
        private const val WAKE_LOCK_TAG = "SmartLock::MqttWakeLock"

        var isRunning = false

        private val subscribedTopics = mutableMapOf<String, Consumer<Mqtt5Publish>>()

        private var instance: MqttService? = null
        fun getInstance(): MqttService? {
            return instance
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("MqttService", "═══════════════════════════════════")
        Log.d("MqttService", "onCreate() called - Service STARTING")
        Log.d("MqttService", "Android Version: ${Build.VERSION.SDK_INT}")
        Log.d("MqttService", "═══════════════════════════════════")

        try {
            createNotificationChannel()
            Log.d("MqttService", "✓ Notification channel created")

            val notification = createNotification()
            Log.d("MqttService", "✓ Notification created")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Log.d("MqttService", "Starting foreground service with type (Android 14+)")
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d("MqttService", "Starting foreground service (Android 10+)")
                startForeground(NOTIFICATION_ID, notification)
            } else {
                Log.d("MqttService", "Starting foreground service (Android 9-)")
                startForeground(NOTIFICATION_ID, notification)
            }

            Log.d("MqttService", "✓ startForeground() called successfully")

            acquireWakeLock()
            Log.d("MqttService", " WakeLock acquired")

            isRunning = true
            Log.d("MqttService", "isRunning set to true")

            Log.d("MqttService", "About to connect MQTT...")
            connectMqtt()

        } catch (e: Exception) {
            Log.e("MqttService", "✗✗✗ ERROR in onCreate() ✗✗✗")
            Log.e("MqttService", "Error message: ${e.message}")
            Log.e("MqttService", "Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(){
        Log.d("MqttService", "createNotificationChannel() called")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MQTT Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for MQTT foreground service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            Log.d("MqttService", "Notification channel created for Android O+")
        }
    }

    private fun connectMqtt(){
        Log.d("MqttService", "connectMqtt() called - Starting connection process")

        try {
            MqttClientManager.connect(
                context = this,
                username = "toikhonghai",
                password = "Huy123456",
                onConnected = {
                    Log.i("MqttService", "KẾT NỐI HIVEMQ THÀNH CÔNG!")
                    reSubscribeAll()
                },
                onFailure = { err ->
                    Log.e("MqttService", "LỖI KẾT NỐI MQTT")
                    Log.e("MqttService", "Error: ${err.message}", err)
                }
            )
            Log.d("MqttService", "MqttClientManager.connect() called successfully")
        } catch (e: Exception) {
            Log.e("MqttService", "Exception in connectMqtt(): ${e.message}", e)
        }
    }

    fun subscribe(topic: String, listener: Consumer<Mqtt5Publish>){
        Log.d("MqttService", "subscribe() called for topic: $topic")
        subscribedTopics[topic] = listener
        MqttClientManager.subscribe(topic, listener = listener)
    }

    private fun reSubscribeAll(){
        Log.d("MqttService", "reSubscribeAll() called with ${subscribedTopics.size} topics")
        subscribedTopics.forEach { (topic, listener) ->
            MqttClientManager.subscribe(topic, listener = listener)
        }
    }

    private fun acquireWakeLock() {
        Log.d("MqttService", "acquireWakeLock() called")
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire(10 * 60 * 1000L)
            }
            Log.d("MqttService", "WakeLock acquired successfully")
        } catch (e: Exception) {
            Log.e("MqttService", "Failed to acquire WakeLock: ${e.message}", e)
        }
    }

    private fun createNotification(): Notification {
        Log.d("MqttService", "createNotification() called")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartLock Service")
            .setContentText("Đang duy trì kết nối MQTT...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MqttService", "onStartCommand() called")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MqttService", "onDestroy() called - Service STOPPING")
        isRunning = false
        wakeLock?.release()
        MqttClientManager.disconnect()
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("MqttService", "onBind() called")
        return null
    }
}