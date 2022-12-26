package com.example.eatical_mobile_app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.eatical_mobile_app.databinding.ActivityPhotoBinding

class PhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoBinding
    private var fileUri: Uri? = null
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            takePhoto()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        Toast.makeText(this, "Image sent", Toast.LENGTH_SHORT).show()
        // TODO: Stefan needs to implement call to backend
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
        handler.postDelayed(runnable, 5000)
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

    companion object {
        private const val CAMERA_CODE = 0
        private const val GALLERY_CODE = 1
    }
}