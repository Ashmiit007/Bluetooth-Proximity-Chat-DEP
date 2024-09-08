package com.example.mypp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@RequiresApi(Build.VERSION_CODES.S)
fun hasBluetoothPermission(activity: MainActivity): Boolean {
    var permissionState = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
    var res = (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED)
    permissionState = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH)
    res = res && (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED)
    permissionState = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN)
    res = res && (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED)
    permissionState = ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN)
    res = res && (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED)
    permissionState = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
    res = res && (permissionState == android.content.pm.PackageManager.PERMISSION_GRANTED)
    return res
}

fun isBluetoothEnabled(): Boolean {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    return bluetoothAdapter != null && bluetoothAdapter.isEnabled
}
@SuppressLint("MissingPermission")
fun getPairedDevices(): Set<BluetoothDevice>? {
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    return bluetoothAdapter?.bondedDevices
}


val BLUETOOTHCODE = 123
@RequiresApi(Build.VERSION_CODES.S)
fun Request_Bluetooth_Permissions(activity: MainActivity)
{
    ActivityCompat.requestPermissions(
        activity as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.BLUETOOTH_CONNECT),
                BLUETOOTHCODE
            );
}