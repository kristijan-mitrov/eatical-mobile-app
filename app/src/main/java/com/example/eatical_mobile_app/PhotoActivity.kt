package com.example.eatical_mobile_app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.eatical_mobile_app.databinding.ActivityPhotoBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import java.io.InputStream

class PhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoBinding
    private var fileUri: Uri? = null
    private val handler = Handler()
    private var lastBrightness:Float = 100f
    private val runnable = object : Runnable {
        override fun run() {
            takePhoto()
            handler.postDelayed(this, 5000)
        }
    }

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
            "gallery" -> openGallery()
            "intervalShooter" -> {
                binding.sendButton.isVisible = false
                takePhotoOnInterval()
            }
            else -> finish()
        }

        observeButtons()
    }

    private fun observeButtons() = with(binding) {
        sendButton.setOnClickListener {
            sendImageToBackend()
        }
        backButton.setOnClickListener {
            handler.removeCallbacks(runnable)
            finish()
        }
    }

    private fun sendImageToBackend() {
        val type = intent.getStringExtra("type")
        val longitude = intent.getStringExtra("longitude")
        val latitude = intent.getStringExtra("latitude")

        val file = getFileFromUri()
        var stringMessage = "{ " + "\"coordinates\":[${longitude},${latitude}], \"image\":\"${file}\" }"
        stringMessage = stringMessage.replace("\n", "#")
        publishToBroker(type.toString(), stringMessage)

        Toast.makeText(this, "Image sent", Toast.LENGTH_SHORT).show()
    }

    private fun sendBrightnessAlert() {
        val type = "brightnessAlert"
        Toast.makeText(this, "Brightness to low!", Toast.LENGTH_SHORT).show()
        // TODO: Stefan needs to implement call to backend

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

        startActivityForResult(cameraIntent, CAMERA_CODE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_CODE)
    }

    private fun takePhotoOnInterval() {
        checkBrightness()
        handler.postDelayed(runnable, 5000)
    }

    private fun checkBrightness(){
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            ?: return

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val brightness = event.values[0]
                if (brightness < 20 && lastBrightness >= 20) {
                    handler.removeCallbacks(runnable)
                    sendBrightnessAlert()
                }else if(brightness >= 20 && lastBrightness < 20){
                    handler.postDelayed(runnable, 5000)
                }
                lastBrightness = brightness
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun takePhoto() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0] // use the first camera on the device
        val imageReader = ImageReader.newInstance( 10800,19200, ImageFormat.JPEG, 1)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.TITLE, "image")
                put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
            }

            fileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if(fileUri!=null) {
                contentResolver.openOutputStream(fileUri!!).use { outputStream ->
                    outputStream?.write(bytes)
                    binding.image.setImageURI(fileUri)
                    sendImageToBackend()
                }
            }
        }, null)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, -100)
                    addTarget(imageReader.surface)
                }
                camera.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, null)
            }
            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_CODE) {
            fileUri = data?.data
        }
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

    companion object {
        private const val CAMERA_CODE = 0
        private const val GALLERY_CODE = 1

    }
}