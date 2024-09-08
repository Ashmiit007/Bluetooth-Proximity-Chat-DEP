package com.example.mypp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import com.example.mypp.MainActivity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
//import androidx.core.content.ContextCompat.startActivity

var x = 0;
fun add1(list_buttons: TextView)
{
    x+=1;
    var auto = x.toString();
    list_buttons.text = auto;

}
@RequiresApi(Build.VERSION_CODES.S)
fun Bluetooth_Setup(activity: MainActivity){
    Bluetooth_Permission_Check(activity)
}
@RequiresApi(Build.VERSION_CODES.S)
fun Bluetooth_Permission_Check(activity: MainActivity)
{
    if(hasBluetoothPermission(activity))
    {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if(isBluetoothEnabled() && isLocationEnabled)
        {
            if(x==0)
            {
                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
                activity.startActivity(discoverableIntent)
                x += 1
            }
            Paired_Devices(activity)
            val display_error = activity.findViewById<TextView>(R.id.sex)
            display_error.text = "WELCOME ABOARD.."
            start_search(activity)
        }
        else
        {
            val display_error = activity.findViewById<TextView>(R.id.sex)
            display_error.text = "PLEASE ENABLE BLUETOOTH AND LOCATION!!!!"
        }
    }
    else
    {
        Request_Bluetooth_Permissions(activity)
        Begin_App(activity)
    }
}