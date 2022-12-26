package com.example.eatical_mobile_app

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eatical_mobile_app.databinding.ActivityPhotoBinding

class PhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoBinding
    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)

        setContentView(binding.root)

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

        startActivityForResult(cameraIntent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.image.setImageURI(fileUri)
    }
}