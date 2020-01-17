package com.unibo.androidrobot

import android.hardware.SensorEvent
import android.os.AsyncTask

data class PublishTaskBundle(val mqttClient : MyMqttClient,
                                    val event : SensorEvent,
                                    val topic : String,
                                    val messageCount : Int)

class PublishTask : AsyncTask<PublishTaskBundle,Int,Unit>() {

    override fun doInBackground(vararg params: PublishTaskBundle) {
        val bundle = params[0]
        with (bundle) {
            if (mqttClient.isConnected()) {
                val val0: Float? = event.values?.get(0)
                var val1: Float? = null
                var val2: Float? = null
                if (event.values != null && event.values.size >= 2) {
                    val1 = event.values?.get(1)
                }
                if (event.values != null && event.values.size >= 3) {
                    val2 = event.values?.get(2)
                }
                val sensorName = Utils().fix(event.sensor.name)
                var msg = "msg(androidsensor,event,android,none,androidsensor($sensorName($val0"
                if (val1 != null) msg += ",$val1"
                if (val2 != null) msg += ",$val2"
                msg += ")),$messageCount)"
                mqttClient.publish("unibo/qak/$topic", msg)
            }
        }
    }
}