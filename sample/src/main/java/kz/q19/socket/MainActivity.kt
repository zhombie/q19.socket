package kz.q19.socket

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kz.q19.domain.model.language.Language
import kz.q19.socket.listener.SocketStateListener

class MainActivity : AppCompatActivity(), SocketStateListener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private var textView: TextView? = null
    private var button: Button? = null

    private var socketClient: SocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        button = findViewById(R.id.button)

        SocketClientConfig.init(true, Language.RUSSIAN)

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
                socketClient?.setSocketStateListener(this)
                socketClient?.registerSocketConnectEventListener()
                socketClient?.registerSocketDisconnectEventListener()
                socketClient?.connect()

                textView?.text = "Status: Connecting"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        socketClient?.disconnect()
        socketClient?.removeAllListeners()
        socketClient = null

        textView = null
        button = null
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        Log.d(TAG, "onSocketConnect()")

        runOnUiThread {
            textView?.text = "Status: Connected\nid: ${socketClient?.getId()}"

            button?.text = "Disconnect"
        }
    }

    override fun onSocketDisconnect() {
        Log.d(TAG, "onSocketDisconnect()")

        socketClient?.release()

        runOnUiThread {
            textView?.text = "Status: Disconnected"

            button?.text = "Connect"
        }
    }

}