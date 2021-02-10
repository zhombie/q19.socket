package kz.q19.socket

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kz.q19.socket.listener.SocketStateListener

class MainActivity : AppCompatActivity(), SocketStateListener {

    private var socketClient: SocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        socketClient = SocketClient.getInstance()
        socketClient?.create("https://kenes2.vlx.kz/user")
        socketClient?.registerSocketConnectEventListener()
        socketClient?.registerSocketDisconnectEventListener()
        socketClient?.setSocketStateListener(this)
        socketClient?.connect()

        println("socketClient.isConnected: " + socketClient?.isConnected())
    }

    override fun onDestroy() {
        socketClient?.release()
        socketClient?.setSocketStateListener(null)
        socketClient = null
        super.onDestroy()
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        println("onSocketConnect()")
    }

    override fun onSocketDisconnect() {
        println("onSocketDisconnect()")
    }

}