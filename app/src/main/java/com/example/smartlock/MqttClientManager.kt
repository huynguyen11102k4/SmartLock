package com.example.smartlock

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.util.function.Consumer
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

object MqttClientManager {
    private var client: Mqtt5AsyncClient? = null
    private val listeners = mutableMapOf<String, Consumer<Mqtt5Publish>>()

    @SuppressLint("CheckResult")
    fun connect(
        context: Context,
        clientId: String = UUID.randomUUID().toString(),
        username: String,
        password: String,
        onConnected: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (client?.state?.isConnected == true) {
            onConnected()
            return
        }

        client = Mqtt5Client.builder()
            .identifier(clientId)
            .serverHost("d2684409b6644e89b97ca34b695085ae.s1.eu.hivemq.cloud")
            .serverPort(8883)
            .sslWithDefaultConfig()
            .automaticReconnectWithDefaultConfig()
            .buildAsync()

        val connectBuilder = client!!.toAsync().connectWith()
            .simpleAuth()
            .username(username)
            .password(password.toByteArray())
            .applySimpleAuth()
            .cleanStart(false)
            .keepAlive(60)

        connectBuilder.sessionExpiryInterval(600)

        connectBuilder.send()
            .whenComplete { connAck: Mqtt5ConnAck, throwable: Throwable? ->
                if (throwable != null) {
                    Log.e("MQTT", "Connect failed: ${throwable.message}")
                    onFailure(throwable)
                } else {
                    Log.i("MQTT", "Connected: ${connAck.reasonCode}")
                    onConnected()
                }
            }

    }

    fun subscribe(topic: String, qos: Int = 1, listener: Consumer<Mqtt5Publish>) {
        listeners[topic] = listener

        val qosLevel = when (qos) {
            0 -> MqttQos.AT_MOST_ONCE
            1 -> MqttQos.AT_LEAST_ONCE
            2 -> MqttQos.EXACTLY_ONCE
            else -> MqttQos.AT_LEAST_ONCE
        }

        val subscribe = Mqtt5Subscription.builder()
            .topicFilter(topic)
            .qos(qosLevel)
            .build()

        client?.subscribeWith()
            ?.addSubscription(subscribe)
            ?.callback(listener)
            ?.send()
            ?.whenComplete { subAck: Mqtt5SubAck, throwable: Throwable? ->
                if (throwable != null) {
                    Log.e("MQTT", "Subscribe failed: ${throwable.message}")
                } else {
                    Log.i("MQTT", "Subscribed to $topic")
                }
            }
    }

    @SuppressLint("CheckResult")
    fun publish(
        topic: String,
        payload: String,
        qos: Int = 1,
        retained: Boolean = false,
        expirySeconds: Long? = 600L
    ) {
        val qosLevel = when (qos) {
            0 -> MqttQos.AT_MOST_ONCE
            1 -> MqttQos.AT_LEAST_ONCE
            2 -> MqttQos.EXACTLY_ONCE
            else -> MqttQos.AT_LEAST_ONCE
        }

        val c = client
        if (c?.state?.isConnected != true) {
            Log.w("MQTT", "Client not connected; publish skipped for $topic")
            return
        }

        val pub = c.publishWith()
            .topic(topic)
            .qos(qosLevel)
            .retain(retained)
            .payload(payload.toByteArray(Charsets.UTF_8))
            .payloadFormatIndicator(Mqtt5PayloadFormatIndicator.UTF_8)
            .contentType("application/json")
            .userProperties()
            .add("app", "SmartLock")
            .add("version", "1.0")
            .applyUserProperties()
            .let { builder ->
                if (expirySeconds != null && expirySeconds > 0) {
                    builder.messageExpiryInterval(expirySeconds)
                }
                builder
            }
            .send()

        pub.whenComplete { _, err ->
            if (err != null) {
                Log.e("MQTT", "Publish failed on $topic: ${err.message}", err)
            } else {
                Log.i("MQTT", "Published to $topic (qos=$qosLevel, retained=$retained)")
            }
        }
    }


    fun disconnect() {
        client?.disconnect()
        client = null
    }

    fun isConnected() = client?.state?.isConnected == true
}