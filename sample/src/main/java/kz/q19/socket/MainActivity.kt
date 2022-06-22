package kz.q19.socket

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import kz.garage.chat.model.Message
import kz.q19.domain.model.call.type.CallType
import kz.q19.domain.model.language.Language
import kz.q19.socket.listener.CoreListener
import kz.q19.socket.listener.SocketStateListener
import kz.q19.socket.model.CallInitialization
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class MainActivity : AppCompatActivity(), SocketStateListener, CoreListener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private var textView: TextView? = null
    private var connectionButton: Button? = null
    private var callButton: Button? = null
    private var redialButton: Button? = null
    private var cancelButton: Button? = null

    private var socketClient: SocketClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        connectionButton = findViewById(R.id.connectionButton)
        callButton = findViewById(R.id.callButton)
        redialButton = findViewById(R.id.redialButton)
        cancelButton = findViewById(R.id.cancelButton)

        SocketClientConfig.init(true, Language.RUSSIAN)

        textView?.text = "Status: IDLE"

        connectionButton?.text = "Connect"
        connectionButton?.setOnClickListener {
            if (socketClient?.isConnected() == true) {
                socketClient?.disconnect()
                socketClient = null

                textView?.text = "Status: Disconnecting"
            } else {
                var handshakeCertificates: HandshakeCertificates? = null
                try {
                    val certificateFactory = CertificateFactory.getInstance("X.509")

                    val certificate =
                        certificateFactory.generateCertificateSafely(R.raw.certificate)

                    handshakeCertificates = HandshakeCertificates.Builder()
                        .addTrustedCertificate(certificate)
                        .addPlatformTrustedCertificates()
                        .build()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val okHttpClient = OkHttpClient.Builder()
                    .apply {
                        if (handshakeCertificates != null) {
                            sslSocketFactory(
                                handshakeCertificates.sslSocketFactory(),
                                handshakeCertificates.trustManager
                            )
                        }
                    }
                    .build()

                socketClient = SocketClient.getInstance()
                socketClient?.create(
                    "https://kenes.1414.kz/user",
                    okHttpClient = okHttpClient
                )
                socketClient?.setSocketStateListener(this)
                socketClient?.setCoreListener(this)
                socketClient?.registerSocketConnectEventListener()
                socketClient?.registerSocketDisconnectEventListener()
                socketClient?.registerMessageEventListener()
                socketClient?.connect()

                textView?.text = "Status: Connecting"
            }
        }

        callButton?.setOnClickListener {
            if (socketClient?.isConnected() == true) {
                socketClient?.sendCallInitialization(
                    CallInitialization(
                        callType = CallType.VIDEO,
                        domain = "digitaltson",
                        topic = "mtest2",
                        iin = "901020304060",
                        phone = "77771234561",
                        lastName = "Test Lastname",
                        firstName = "Test Firstname",
                        patronymic = "Test Patronymic",
                        language = Language.RUSSIAN
                    )
                )

                cancelButton?.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "No WebSocket connection", Toast.LENGTH_SHORT).show()

                cancelButton?.visibility = View.GONE
            }
        }

        redialButton?.setOnClickListener {
            if (socketClient?.isConnected() == true) {
                socketClient?.sendCallReinitialization(
                    CallInitialization(
                        callType = CallType.VIDEO,
                        domain = "digitaltson",
                        topic = "mtest2",
                        iin = "901020304060",
                        phone = "77771234561",
                        lastName = "Test Lastname",
                        firstName = "Test Firstname",
                        patronymic = "Test Patronymic",
                        language = Language.RUSSIAN
                    )
                )

                cancelButton?.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "No WebSocket connection", Toast.LENGTH_SHORT).show()

                cancelButton?.visibility = View.GONE
            }
        }

        cancelButton?.visibility = View.GONE
        cancelButton?.setOnClickListener {
            if (socketClient?.isConnected() == true) {
                socketClient?.sendPendingCallCancellation()
                cancelButton?.visibility = View.GONE
            } else {
                Toast.makeText(this, "No WebSocket connection", Toast.LENGTH_SHORT).show()
                cancelButton?.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        socketClient?.disconnect()
        socketClient?.removeAllListeners()
        socketClient = null

        textView = null
        connectionButton = null
        callButton = null
        redialButton = null
    }

    /**
     * [SocketStateListener] implementation
     */

    override fun onSocketConnect() {
        Log.d(TAG, "onSocketConnect()")

        runOnUiThread {
            textView?.text = "Status: Connected\nid: ${socketClient?.getId()}"

            connectionButton?.text = "Disconnect"
        }
    }

    override fun onSocketDisconnect() {
        Log.d(TAG, "onSocketDisconnect()")

        socketClient?.release()

        runOnUiThread {
            cancelButton?.visibility = View.GONE

            textView?.text = "Status: Disconnected"

            connectionButton?.text = "Connect"
        }
    }

    override fun onMessage(message: Message) {
        Log.d(TAG, "onMessage() -> message: $message")
    }

    private fun CertificateFactory.generateCertificateSafely(@RawRes certificateId: Int): Certificate? {
        val inputStream = try {
            resources.openRawResource(certificateId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        return inputStream?.use {
            generateCertificate(inputStream)
        }
    }

    private fun HandshakeCertificates.Builder.addTrustedCertificate(certificate: Certificate?): HandshakeCertificates.Builder {
        if (certificate is X509Certificate) {
            addTrustedCertificate(certificate)
        }
        return this
    }

}