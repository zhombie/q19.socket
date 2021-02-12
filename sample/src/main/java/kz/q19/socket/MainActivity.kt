package kz.q19.socket

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kz.q19.socket.listener.SocketStateListener

class MainActivity : AppCompatActivity(), SocketStateListener {

    private var textView: TextView? = null
    private var button: Button? = null

    private var socketClient: SocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        button = findViewById(R.id.button)

        textView?.text = "Status: IDLE"

        button?.text = "Connect"
        button?.setOnClickListener {
            if (socketClient?.isConnected() == true) {
                socketClient?.disconnect()
                socketClient = null

                textView?.text = "Status: Disconnecting"
            } else {
                socketClient = SocketClient.getInstance()
                socketClient?.create("https://kenes2.vlx.kz/user")
                socketClient?.registerSocketConnectEventListener()
                socketClient?.registerSocketDisconnectEventListener()
                socketClient?.setSocketStateListener(this)
                socketClient?.connect()

                textView?.text = "Status: Connecting"
            }
        }
    }

    override fun onDestroy() {
        socketClient?.disconnect()
        socketClient?.setSocketStateListener(null)
        socketClient = null
        super.onDestroy()
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        println("onSocketConnect()")
        runOnUiThread {
            textView?.text = "Status: Connected"

            button?.text = "Disconnect"
        }
    }

    override fun onSocketDisconnect() {
        println("onSocketDisconnect()")

        socketClient?.release()

        runOnUiThread {
            textView?.text = "Status: Disconnected"

            button?.text = "Connect"
        }
    }

}