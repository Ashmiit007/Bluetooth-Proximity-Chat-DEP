package com.example.mypp
import android.provider.MediaStore
import android.annotation.SuppressLint
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream

@Volatile
var socketList: MutableList<BluetoothSocket> = mutableListOf()
var connection_avail = 1
var shouldLoop = true
var dev_name = "Me: "
fun sendMessage(message: String,outputStream: OutputStream) {
    try {
        outputStream.write(message.toByteArray())
    } catch (e: Exception) {
        Log.e("sendMessage", "Error sending message: ${e.message}", e)
    }
}

fun saveImageToGallery(context: Context, file: File, displayName: String): Boolean {
    val mimeType = getMimeType(file)
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/JohnnyDEP")
    }

    var stream: OutputStream? = null
    var success = false
    try {
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            stream = resolver.openOutputStream(uri)
            if (stream != null) {
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(stream)
                }
                success = true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        stream?.close()
    }

    return success
}

fun getMimeType(file: File): String {
    val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}

@SuppressLint("UseCompatLoadingForDrawables")
fun receiveMessages(activity: MainActivity) {
    val message_list = activity.findViewById<LinearLayout>(R.id.msg_here_left)
    val message_list_ryt = activity.findViewById<LinearLayout>(R.id.msg_here_right)
    var buffer = ByteArray(1024)
    var bytes: Int
    while (connection_avail == 1) {

        for(socket in socketList)
        {
            val inputStream: InputStream = socket.inputStream
            if(inputStream.available()!=0){
                var bytesRead: Int
                bytesRead = inputStream.read(buffer)
                val message = String(buffer, 0, bytesRead)
                if (message.startsWith("FILE:")) {
                    Log.d("IMAGE DETE", "DETECTED SUCCESS")
                    val fileName = "image_${System.currentTimeMillis()}.jpeg"
                    val fileOutputStream = ByteArrayOutputStream()
                    val file = File(activity.filesDir, fileName)
                    Log.d("SEXTER",file.extension)
                    var totalBytesRead = bytesRead - 10
                    var xx = 0
                    fileOutputStream.write(buffer, 0, bytesRead)
                    for(relay in socketList)
                    {
                        if(relay != socket)
                        {
                            val relay_outputStream: OutputStream = relay.outputStream
                            relay_outputStream.write(buffer,0,bytesRead)
                        }
                    }
                    while (true) {
                        bytesRead = inputStream.read(buffer)
                        val messagei = String(buffer, 0, bytesRead)
                        for(relay in socketList)
                        {
                            if(relay != socket)
                            {
                                val relay_outputStream: OutputStream = relay.outputStream
                                relay_outputStream.write(buffer,0,bytesRead)
                            }
                        }
                        if (messagei.endsWith("STOPP")) {
                            fileOutputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            Log.d("BluetoothImageTransfer", "End of stream reached. Total bytes read: $totalBytesRead")
                            break
                        }
                        fileOutputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        xx += 1
                        Log.d("FUCKER", "$xx")
                        Log.d("BluetoothImageTransfer", "Read $bytesRead bytes. Total: $totalBytesRead")
                    }
                    Log.d("IMAGE DETE", "DECODED STARTED")
                    var imageBytes = fileOutputStream.toByteArray()
                    imageBytes = imageBytes.copyOfRange(5,imageBytes.size-5)
                    Log.d("IMAGE BYTES",imageBytes.size.toString())
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    var img_file = FileOutputStream(File(activity.filesDir, fileName))

                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, img_file)

                    val imageView = ImageView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            600, 600 // Width and height in pixels
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    activity.runOnUiThread {
                        var i = 0
                        while( i < 6)
                        {
                            i += 1
                            var textView = TextView(activity)
                            textView.text = "\n"
                            message_list_ryt.addView(textView)
                        }

                        imageView.setImageBitmap(bitmap)
                        message_list.addView(imageView)
                    }
                    fileOutputStream.close()



                    saveImageToGallery(activity,file,fileName)

                } else {
                    // This is a text message
                    var to_display = ""
                    to_display = to_display + message
                    activity.runOnUiThread {
                        var textView = TextView(activity)
                        textView.text = to_display
                        textView.textSize = 16f
                        textView.setPadding(16, 8, 16, 8)
                        textView.setTextColor(Color.BLACK)
                        val bubbleDrawable = activity.getDrawable(R.drawable.bubble_background)
                        textView.background = bubbleDrawable
                        message_list.addView(textView)

                        textView = TextView(activity)
                        textView.text = "\n\n"
                        message_list_ryt.addView(textView)

                    }
                    for (relay in socketList) {
                        if (relay != socket) {
                            sendMessage(message, relay.outputStream)
                        }
                    }
                }
            }
        }

    }
}


fun sendFiles(file: File, activity: MainActivity)
{
    val buffer = ByteArray(1024)
    val inputStream: InputStream = FileInputStream(file)
    var keyword = "FILE:".toByteArray()

    val bitmap = BitmapFactory.decodeFile(file.path)
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    var byte_array = stream.toByteArray()
    for(socket in socketList)
    {
        keyword = "FILE:".toByteArray()
        val outputStream: OutputStream = socket.outputStream
        outputStream.write(keyword);

        outputStream.write(byte_array)
        keyword = "STOPP".toByteArray()
        outputStream.write(keyword)
    }
    Log.d("IMAGE DETE", "SENT SUCCESS")
    Log.d("SENT SIZE", byte_array.size.toString())


    val message_list = activity.findViewById<LinearLayout>(R.id.msg_here_right)
    var textView = TextView(activity)

    var message = "SENT an IMAGE"
    var to_display = "Me: "
    to_display = to_display + message
    textView.text = to_display
    textView.textSize = 16f
    textView.text = to_display
    textView.textSize = 16f
    textView.setPadding(16, 8, 16, 8)
    textView.setTextColor(Color.BLACK)
    val bubbleDrawable = activity.getDrawable(R.drawable.bulle_right)
    textView.background = bubbleDrawable
    val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )
    layoutParams.gravity = Gravity.END
    textView.layoutParams = layoutParams

    message_list.addView(textView)

    val message_list_ryt = activity.findViewById<LinearLayout>(R.id.msg_here_left)
    textView = TextView(activity)
    textView.text = "\n"
    message_list_ryt.addView(textView)

}

private const val REQUEST_PICK_FILE = 1001
fun openFilePicker(activity: MainActivity) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/jpeg" // This specifies that all file types are allowed
    }
    activity.startActivityForResult(intent, REQUEST_PICK_FILE)
}


fun check_connections(activity: MainActivity)
{
    while(true)
    {
        for(socket in socketList)
        {
            if(socket.isConnected == false)
            {
                socketList.remove(socket)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.S)
fun start_chat(activity: MainActivity, socket: BluetoothSocket)
{
    socketList.add(socket)
    shouldLoop = true
    connection_avail = 1
    activity.setContentView(R.layout.messageui)
    allow_more(activity)
    dev_name = bluetoothAdapter.name + ":\n"

    val receiveThread = Thread { receiveMessages(activity) }
    receiveThread.start()

//    Thread{check_connections(activity)}.start()

    val send_button = activity.findViewById<Button>(R.id.send_message)
    send_button.setOnClickListener { extract_message(activity) }



    val exit_button = activity.findViewById<Button>(R.id.exit_button)
    exit_button.setOnClickListener {
        connection_avail = 0
        shouldLoop = false
        for (socket in socketList) {
            socket.close()
        }
        socketList.clear()
        activity.setContentView(R.layout.activity_main)
        Begin_App(activity)
    }

    val sendfile = activity.findViewById<Button>(R.id.sendfile)
    sendfile.setOnClickListener { openFilePicker(activity) }
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
fun allow_more(activity: MainActivity)
{
    val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("NAME", MY_UUID)
    }

    val text_disp = activity.findViewById<TextView>(R.id.connection_availability)
    activity.runOnUiThread{
        text_disp.text = "Not Available for Connections"
    }
    Thread {
        while (shouldLoop) {
            activity.runOnUiThread{
                text_disp.text = "Available for Connections"
            }
            if(socketList.size >= 2)
            {
                shouldLoop = false
                break;
            }
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Socket's accept() method failed", e)
                null
            }
            socket?.also {
                mmServerSocket?.close()

                if(shouldLoop == false)
                {
                    activity.runOnUiThread {
                        start_chat(activity, it)
                    }
                }
                else
                {
                    socketList.add(it)
                    if(socketList.size >= 2)
                    {
                        shouldLoop = false
                    }
                }
            }
        }
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not close the connect socket", e)
            }
        }
        activity.runOnUiThread{
            text_disp.text = "Not Available for Connections"
        }
    }.start()

}


@SuppressLint("UseCompatLoadingForDrawables")
fun extract_message(activity: MainActivity)
{
    val text_message = activity.findViewById<EditText>(R.id.textinput_1)
    var message = text_message.text.toString()
    var sev_message = dev_name + message

    for(socket in socketList)
    {
        sendMessage(sev_message,socket.outputStream);
    }
    val message_list = activity.findViewById<LinearLayout>(R.id.msg_here_right)
    var textView = TextView(activity)
    var to_display = "Me:\n"
    to_display = to_display + message
    textView.text = to_display
    textView.textSize = 16f
    textView.text = to_display
    textView.textSize = 16f
    textView.setPadding(16, 8, 16, 8)
    textView.setTextColor(Color.BLACK)
    val bubbleDrawable = activity.getDrawable(R.drawable.bulle_right)
    textView.background = bubbleDrawable
    val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
    )
    layoutParams.gravity = Gravity.END
    textView.layoutParams = layoutParams
    if(message != "") {
        message_list.addView(textView)
    }
    val message_list_ryt = activity.findViewById<LinearLayout>(R.id.msg_here_left)
    textView = TextView(activity)
    textView.text = "\n\n"
    message_list_ryt.addView(textView)


    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    text_message.text = null
    imm.hideSoftInputFromWindow(text_message.windowToken, 0)
}
