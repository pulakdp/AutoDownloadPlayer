package io.github.pulakdp.autodownloadplayer.data

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Created by pulak on 24/11/18.
 */
class StreamingServer(
    private val videoFile: File,
    private val localIp: String?,
    private val localServerCallback: LocalServerCallback
) {

    private val LOG_TAG = "StreamingServer"

    lateinit var executor: ExecutorService

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    private var port: Int = 0
    private var isRunning: Boolean = false

    interface LocalServerCallback {
        fun onServerStart(streamUrl: String)
    }

    fun setUpLocalServer() {

        executor = Executors.newSingleThreadExecutor()
        lateinit var url: String
        try {
            val iNetAddress = InetAddress.getByName(localIp)
            val bytes = iNetAddress.address

            //No need of any backlog because only I'll be accessing it
            serverSocket = ServerSocket(port, 0, InetAddress.getByAddress(bytes))
            serverSocket?.soTimeout = 10000
            port = serverSocket?.localPort!!
            url = ("http://" + serverSocket?.inetAddress?.hostAddress + ":" + port)
            Log.d(LOG_TAG, "Server started at $url")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Exception occurred at server: $e")
        }
    }

    fun startServer() {
        isRunning = true
        executor.execute(serverRunnable)
        localServerCallback.onServerStart(getFileUrl())
    }

    fun stopService() {
        isRunning = false
        executor.shutdown()
    }

    private fun getFileUrl(): String {
        return "http://${serverSocket?.inetAddress?.hostAddress}:$port/${videoFile.name}"
    }

    private fun getInputStreamFromFile(): FileInputStream? {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(videoFile)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Problem occurred while getting stream from file: $e")
        }
        return fileInputStream
    }

    private val serverRunnable = Runnable {
        while (isRunning) {
            try {
                Log.d(LOG_TAG, "server socket value: $serverSocket")
                clientSocket = serverSocket?.accept()
                Log.d(LOG_TAG, "Client connected!")
                Thread.sleep(1000)
                putDataOnLocalServer(clientSocket)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Oops! $e.message")
            }
        }
        Log.e(LOG_TAG, "Server stopped")
    }

    @Throws(Exception::class)
    private fun putDataOnLocalServer(client: Socket?) {
        //Constructing header so that exoplayer recognizes that the source is a video
        val lineBreak = "\r\n"
        val headerBuilder = StringBuilder()
        headerBuilder.append("HTTP/1.1 200 OK$lineBreak")
        headerBuilder.append("Content-Type: video/${videoFile.name.substring(videoFile.name.lastIndexOf(".") + 1)}")
        headerBuilder.append(lineBreak)
        headerBuilder.append("Accept-Ranges: bytes")
        headerBuilder.append(lineBreak)
        headerBuilder.append("Content-Length: ${videoFile.length()}")
        headerBuilder.append(lineBreak)
        headerBuilder.append(lineBreak)

        var inputStreamFromFile: InputStream? = null
        try {
            inputStreamFromFile = getInputStreamFromFile()
            val buffer = headerBuilder.toString().toByteArray()

            //Writing header to client
            client?.getOutputStream()?.write(buffer, 0, buffer.size)

            // Start sending content.

            val buff = ByteArray(1024 * 50)
            var cbSentThisBatch = 0

            while (isRunning) {
                while (!DataSync.isDataReady()) {
                    if (DataSync.dataStatus == DataSync.DATA_READY) {
                        Log.d(LOG_TAG, "Data ready, start putting it up on server")
                        break
                    } else if (DataSync.dataStatus == DataSync.DATA_DOWNLOADED) {
                        Log.d(LOG_TAG, "All Data Downloaded")
                        break
                    } else if (DataSync.dataStatus == DataSync.DATA_DOWNLOADING) {
                        Log.d(LOG_TAG, "Waiting for data")
                    } else if (DataSync.dataStatus == DataSync.DATA_NOT_AVAILABLE) {
                        Log.d(LOG_TAG, "Data not available")
                    }
                    synchronized(this) {
                        Thread.sleep(1000)
                    }
                }
                val fileReader = inputStreamFromFile!!.read(buff)

                if (fileReader == -1) {
                    inputStreamFromFile.close()
                    inputStreamFromFile = getInputStreamFromFile()
                    continue
                }
                client?.getOutputStream()?.write(buff, 0, fileReader)
                client?.getOutputStream()?.flush()
                cbSentThisBatch += fileReader

                DataSync.consumeByte += fileReader
            }
            // If we sent nothing, block for a second
            if (cbSentThisBatch == 0) {
                Log.d(LOG_TAG, "Blocking until more data appears")
                Thread.sleep(1000)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error happened: ", e)
        } finally {
            Log.d(LOG_TAG, "Stream ended")
            inputStreamFromFile?.close()
            client?.close()
        }
    }
}