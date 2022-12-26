package com.example.eatical_mobile_app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eatical_mobile_app.databinding.ActivityPhotoBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import java.io.InputStream

class PhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoBinding
    private var fileUri: Uri? = null

    private lateinit var mqttClient: MqttAndroidClient
    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)

        setContentView(binding.root)

        connectToBroker(this)

        binding.descriptionText.text = String.format(getString(R.string.photo_description), intent.getStringExtra("type"))

        when (intent.getStringExtra("source")) {
            "camera" -> openCamera()
            "gallery" -> Unit // TODO: Gallery
            "intervalShooter" -> Unit // TODO: Interval Shooter
            else -> finish()
        }

        observeButtons()
    }

    private fun observeButtons() = with(binding) {
        sendButton.setOnClickListener {
            sendImageToBackend()
        }
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun sendImageToBackend() {
        val type = intent.getStringExtra("type")
        val longitude = intent.getStringExtra("longitude")
        val latitude = intent.getStringExtra("latitude")
        Toast.makeText(this, "Send Button Clicked", Toast.LENGTH_SHORT).show()

        val file = getFileFromUri()
        var stringMessage = "{ " + "\"coordinates\":[${longitude},${latitude}], \"image\":\"${file}\" }"
        stringMessage = stringMessage.replace("\n", "#")
        publishToBroker(type.toString(), stringMessage)
    }

    private fun getFileFromUri(): String {
        val inputStream: InputStream? = fileUri?.let { contentResolver.openInputStream(it) }
        val bytes: ByteArray = inputStream?.readBytes() ?: ByteArray(0)
        inputStream?.close()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.TITLE, "image")
            put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        }

        fileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
        }

        startActivityForResult(cameraIntent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.image.setImageURI(fileUri)
    }

    fun publishToBroker(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.tag(TAG).d(msg + " published to " + topic)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.tag(TAG).d("Failed to publish " + msg + " to " + topic)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun connectToBroker(context: Context) {
        val serverURI = "tcp://164.8.48.81:3002"
        mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client")
        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Timber.tag(TAG).d("Receive message: " + message.toString() + " from topic: " + topic)
            }

            override fun connectionLost(cause: Throwable?) {
                Timber.tag(TAG).d("Connection lost " + cause.toString())
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Timber.tag(TAG).d("Message is sent to broker!")
            }
        })
        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.tag(TAG).d("Connection success")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.tag(TAG).d("Connection failure")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}