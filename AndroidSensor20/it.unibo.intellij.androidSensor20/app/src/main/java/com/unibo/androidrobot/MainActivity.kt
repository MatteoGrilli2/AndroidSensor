package com.unibo.androidrobot

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat

const val APP_TAG  = "AndroidSensor"                    // Tag to filter logcat output
private const val clientId = "AndroidSensors"           // Id to filter mosquitto -v output
private const val defaultBrokerIP = "192.168.1.54"      // local ip of my machine
private const val defaultBrokerPort = "1883"            // default port for mosquitto
private const val defaultBrokerAddress = "tcp://$defaultBrokerIP:$defaultBrokerPort"

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var sensors : List<Sensor>
    private lateinit var sensorActive : HashMap<String,Boolean>
    private lateinit var mqttClient : MyMqttClient
    private var address : String = defaultBrokerAddress
    private var messageCount : Int = 0
    private var connected : Boolean = false

    companion object {
        // Persistence tags
        private const val STATE_MSG_COUNT = "messageCount"
        private const val STATE_BROKER_ADD = "brokerAddress"
        private const val STATE_SENSOR_ACTIVE_PREFIX = "sensorActive"
        private const val STATE_CHECKALL = "checkAllActive"
        private const val STATE_CONNECTED = "connected"
        // Utility
        private const val CHECKBOX_ID_OFFSET = 1000
        private const val DOC_URL_PREFIX = "https://developer.android.com/reference/android/hardware/SensorEvent.html#sensor.type_"
        private const val DOC_URL_POSTFIX = "-:"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get sensor references
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL)

        // Get UI elements references
        val button = findViewById<Button>(R.id.mqttButton)
        val checkAll = findViewById<CheckBox>(R.id.checkAll)
        val addressBar = findViewById<EditText>(R.id.mqttAddress)

        if (savedInstanceState == null) {
            // First instantiation
            sensorActive = HashMap()
            sensors.forEach {
                sensorActive[Utils().fix(it.name)] = false
            }
            messageCount = 0
            connected = false
            mqttClient = MyMqttClient(applicationContext, "tcp://$defaultBrokerIP:$defaultBrokerPort", clientId)
        } else {
            // Restore state
            with (savedInstanceState) {
                sensorActive = HashMap()
                sensors.forEach{
                    sensorActive[Utils().fix(it.name)] = getBoolean(STATE_SENSOR_ACTIVE_PREFIX + Utils().fix(it.name))
                }
                messageCount = getInt(STATE_MSG_COUNT)
                connected = getBoolean(STATE_CONNECTED)
                val a = getString(STATE_BROKER_ADD)
                if (a != null) {
                    address = a
                    mqttClient = MyMqttClient(applicationContext, address, clientId)
                } else {
                    Log.e(APP_TAG, "Failed to restore STATE_BROKER_ADD")
                }
                // If client was connected before, restore connection
                if (connected)
                    mqttClient.connect()
            }
        }

        // Populate sensor table
        val table = findViewById<TableLayout>(R.id.tableLayout)
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        var rowID = 0
        sensors.forEach {
            // Add sensor to layout
            val row = inflater.inflate(R.layout.table_row, table, false) as TableRow
            val checkbox = row.findViewById<CheckBox>(R.id.checkbox)
            val img = row.findViewById<ImageView>(R.id.doc_icon)
            img.setOnClickListener {_ ->
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                val sensorType = it.stringType.toLowerCase().split(".").last()
                val uri = DOC_URL_PREFIX + sensorType + DOC_URL_POSTFIX
                Log.v(APP_TAG, "uri: $uri")
                intent.data = Uri.parse(uri)
                startActivity(intent)
            }
            // Setup checkbox
            val name = Utils().fix(it.name)
            checkbox.id = rowID + CHECKBOX_ID_OFFSET
            checkbox.text = name
            checkbox.isChecked = sensorActive[name] ?: false

            checkbox.setOnClickListener {it1 ->
                val b = (it1 as CheckBox).isChecked
                sensorActive[it1.text.toString()] = b
                if(!b) checkAll.isChecked = false
            }
            row.id = rowID
            row.setBackgroundColor(
                ContextCompat.getColor(
                    applicationContext,
                    when (rowID % 2 == 0) {
                        true -> R.color.rowEven
                        false -> R.color.rowOdd
                    }
                )
            )
            rowID++
            table.addView(row)
        }

        // Update UI
        checkAll.isChecked = savedInstanceState?.getBoolean(STATE_CHECKALL) ?: false
        button.text = if (!connected) resources.getText(R.string.connect) else resources.getText(R.string.disconnect)
        with(addressBar) {
            isEnabled = !connected
            isFocusableInTouchMode = !connected
            isCursorVisible = !connected
            inputType = if (!connected) InputType.TYPE_CLASS_TEXT else InputType.TYPE_NULL
        }

        // Set listeners for UI elements
        checkAll.setOnClickListener{
            val b = (it as CheckBox).isChecked
            for (i in 0 until sensorActive.size) {
                val checkbox = findViewById<CheckBox>(CHECKBOX_ID_OFFSET+i)
                checkbox.isChecked = b
            }
        }
        button.setOnClickListener {
            if (!mqttClient.isConnected()) {
                Log.v(APP_TAG, "Attempting connection to $address")
                val input = addressBar.text.toString()
                address = when {
                    ":" in input -> "tcp://$input"                              // Use provided address
                    input.isNotEmpty() -> "tcp://$input:$defaultBrokerPort"     // Use provided ip and default port
                    else -> defaultBrokerAddress                                // Use default address
                }
                mqttClient = MyMqttClient(applicationContext,address,clientId)
                mqttClient.connect()
                if (mqttClient.isConnected()) {
                    connected = true
                    button.text = resources.getText(R.string.disconnect)
                    with(addressBar) {
                        isEnabled = false
                        isFocusableInTouchMode  = false
                        isCursorVisible = false
                        inputType = InputType.TYPE_NULL
                        invalidate()
                    }
                } else {
                    Log.i(APP_TAG,"Connection failed!")
                }
            } else {
                Log.v(APP_TAG, "Attempting disconnection from mqtt broker...")
                mqttClient.disconnect()
                button.text = resources.getText(R.string.connect)
                with(addressBar) {
                    isEnabled = true
                    isFocusableInTouchMode  = true
                    isCursorVisible = true
                    inputType = InputType.TYPE_CLASS_TEXT
                    invalidate()
                }
                connected = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save state
        outState.run{
            sensors.forEach{
                val b = sensorActive[Utils().fix(it.name)]
                if (b != null) {
                    putBoolean(STATE_SENSOR_ACTIVE_PREFIX + Utils().fix(it.name),b)
                } else {
                    Log.e(APP_TAG, "Failed to retrieve state of " + Utils().fix(it.name))
                }
            }
            putInt(STATE_MSG_COUNT, messageCount)
            putBoolean(STATE_CONNECTED, connected)
            putString(STATE_BROKER_ADD, address)
            putBoolean(STATE_CHECKALL, findViewById<CheckBox>(R.id.checkAll).isChecked)
        }
        // Terminate mqtt client temporarily and update UI
        if (connected) {
            mqttClient.disconnect()
            val button = findViewById<Button>(R.id.mqttButton)
            val addressBar = findViewById<EditText>(R.id.mqttAddress)
            button.text = getText(R.string.connect)
            with(addressBar) {
                isEnabled = true
                isFocusableInTouchMode  = true
                isCursorVisible = true
                inputType = InputType.TYPE_CLASS_TEXT
                invalidate()
            }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (sensorActive[Utils().fix(event.sensor.name)] != null) {
            if (mqttClient.isConnected() && sensorActive[Utils().fix(event.sensor.name)] == true) {
                var topic = findViewById<EditText>(R.id.sendToEditText).text.toString()
                if (topic == "") topic = "events"
                val bundle = PublishTaskBundle(mqttClient, event, topic, messageCount)
                PublishTask().execute(bundle)
                messageCount++
            }
        } else {
            Log.e(APP_TAG, "Unregistered sensor")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Log.v(APP_TAG, "onAccuracyChanged")
    }

    override fun onResume() {
        super.onResume()
        sensors.forEach {
            mSensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }
}
