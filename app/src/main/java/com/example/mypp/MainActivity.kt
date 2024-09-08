package com.example.mypp
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
//import androidx.loader.content.CursorLoader
import android.content.CursorLoader
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.Stack
import java.util.UUID


//var paired_buttons: ScrollView? = null
class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Begin_App(this)
    }
    private fun getRealPathFromURI(uri: Uri): String {
        var filePath = ""
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    filePath = it.getString(columnIndex)
                }
            }
        }
        return filePath
    }
    fun getFileExtension(contentResolver: ContentResolver, uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?: ""
    }
    private fun findFilePath(fileName: String): String? {
        val root = Environment.getExternalStorageDirectory()
        val stack = Stack<File>()
        stack.push(root)
        while (stack.isNotEmpty()) {
            val dir = stack.pop()
            val listFiles = dir.listFiles() ?: continue
            for (file in listFiles) {

                Log.d("tag",file.name);
                if (file.isDirectory) {
                    stack.push(file)

                } else if (file.name + "." + file.extension == fileName) {
                    return file.absolutePath
                }
            }
        }
        return null
    }
    fun copyImageToInternalStorage(
        context: Context,
        imageUri: Uri
    ): String? {
        // Generate a unique file name
        val fileName = "image_${System.currentTimeMillis()}"

        // Get the file extension from the URI
        val fileExtension = getFileExtension(context.contentResolver, imageUri)

        // Create a destination file in the internal storage
        val internalStorageDir = context.filesDir
        val destinationFile = File(internalStorageDir, "$fileName.$fileExtension")

        // Open input stream from the image URI
        val contentResolver: ContentResolver = context.contentResolver
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(imageUri)

            // Decode the input stream into a Bitmap
            val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream)

            // Create a file output stream
            val fileOutputStream = FileOutputStream(destinationFile)

            // Determine the image format based on file extension
            val compressFormat = when (fileExtension.lowercase(Locale.ROOT)) {
                "jpg", "jpeg" -> Bitmap.CompressFormat.JPEG
                "png" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG // Default to JPEG format
            }

            // Compress the bitmap to the file output stream with desired format and quality
            bitmap.compress(compressFormat, 100, fileOutputStream)

            // Close the file output stream
            fileOutputStream.close()

            // Return the absolute path of the saved image file
            return destinationFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            // Close the input stream
            inputStream?.close()
        }

        return null
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val copiedImagePath = copyImageToInternalStorage(this, uri ?: "".toUri())


                if (copiedImagePath != null) {

                    val fileToSend = File(copiedImagePath)
                    Log.d("tag", "Selected file: $copiedImagePath")
                    sendFiles(fileToSend, this)
                }
                else
                {
                    Log.d("tag","FUCKKK")
                }

            }
        }
    }

}

@RequiresApi(Build.VERSION_CODES.S)
fun Begin_App(activity: MainActivity)
{
    val search_button = activity.findViewById<Button>(R.id.button);
    search_button.setOnClickListener { Bluetooth_Setup(activity) }
    val build_server = activity.findViewById<Button>(R.id.create_server)
    build_server.setOnClickListener {
        wait_for_connection(activity) }
}
val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
//val MY_UUID: UUID = UUID.randomUUID()
@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
fun wait_for_connection(activity: MainActivity)
{
    if(hasBluetoothPermission(activity))
    {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
        if(isBluetoothEnabled() && isLocationEnabled)
        {
            val display_error = activity.findViewById<TextView>(R.id.sex)
            display_error.text = "WELCOME ABOARD.."
            start_search(activity)
            allow_connection(activity);
        }
        else
        {
            val display_error = activity.findViewById<TextView>(R.id.sex)
            display_error.text = "PLEASE ENABLE BLUETOOTH AND LOCATION!!!!"
            Begin_App(activity)
        }
    }
    else
    {
        Request_Bluetooth_Permissions(activity)
        Begin_App(activity)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
fun allow_connection(activity: MainActivity)
{
    activity.setContentView(R.layout.connecting_screen)

    val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("NAME", MY_UUID)
    }

    val exit_server = activity.findViewById<Button>(R.id.stop_waiting)
    exit_server.setOnClickListener {
        activity.setContentView(R.layout.activity_main)
        Begin_App(activity)
    }
    Thread {

        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                shouldLoop = false
                null
            }
            socket?.also {
                activity.runOnUiThread {
                    start_chat(activity, it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }.start()
}




@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
private lateinit var socket: BluetoothSocket
@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
fun Paired_Devices(activity: MainActivity)
{
    val paired_buttons = activity.findViewById<LinearLayout>(R.id.paired_list)
    paired_buttons.removeAllViews()

    var paired_Devices_List = getPairedDevices();
    if (paired_Devices_List != null) {
        for (device in paired_Devices_List) {
            if (device != null) {

                val button = Button(activity)
                button.text = device.name
                button.setOnClickListener {
                    if (device != null) {
                        bluetoothAdapter.cancelDiscovery()

                        var socket = device.createRfcommSocketToServiceRecord(MY_UUID)

                        try{
                            socket.connect()
                        }
                        catch (e: IOException)
                        {

                        }

                        if(socket.isConnected())
                        {
                            start_chat(activity,socket);
                        }
                        else{
                            socket.close()
                        }


                    }
                }
                paired_buttons?.addView(button)


            }

        }
    }

}