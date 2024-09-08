package com.example.mypp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.RequiresApi


var Device_list = mutableListOf<BluetoothDevice>();
lateinit var bluetoothAdapter: BluetoothAdapter
var buttons_list: LinearLayout? = null
var String_Devices = ""
var acti: MainActivity? = null
@RequiresApi(Build.VERSION_CODES.S)
fun start_search(activity: MainActivity)
{
        acti = activity
        buttons_list = activity.findViewById<LinearLayout>(R.id.button_list_id)
        String_Devices = ""
        Device_list.clear()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        activity.registerReceiver(bluetoothReceiver, filter)
        startBluetoothDiscovery()


}
    val bluetoothReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice object and its info
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                val deviceName = device?.name ?: "Unknown device"
                val deviceAddress = device?.address ?: "Unknown address"

                if(!Device_list.contains(device))
                {
                    val button = Button(context)
                    button.text = deviceName
                    button.setOnClickListener{
                        if (device != null) {

                            device.createBond()
                            acti?.let { Paired_Devices(it) }

                        }
                    }
                    buttons_list?.addView(button)

                    if (device != null) {
                        Device_list.add(device)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothDiscovery() {
        // Check if Bluetooth is already discovering, if so, cancel discovery
        if (bluetoothAdapter.isDiscovering) {
            Device_list.clear()
            buttons_list?.removeAllViews()
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery();
    }