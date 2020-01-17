package com.unibo.androidrobot

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.UnsupportedEncodingException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence


class MyMqttClient(private val context : Context, private val broker : String, private val clientId : String) {

    private val client = MqttClient(broker,clientId, MemoryPersistence())

    fun connect() {
        try {
           client.connect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun isConnected() : Boolean {
        return client.isConnected
    }

    fun publish(topic : String, payload : String) {
        var encodedPayload = ByteArray(0)
        try {
            encodedPayload = payload.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            client.publish(topic, message)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            client.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }

    }
}