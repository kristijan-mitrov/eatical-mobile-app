package com.example.eatical_mobile_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.eatical_mobile_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //BUTTON LISTENERS
        binding.cameraButton.setOnClickListener{

        }
        binding.galleryButton.setOnClickListener{

        }
        binding.intervalShooterButton.setOnClickListener{

        }

        //CHECKBOXES LISTENERS
        binding.restaurantCheckbox.setOnClickListener{

        }
        binding.menuCheckbox.setOnClickListener{

        }
        binding.foodCheckbox.setOnClickListener{

        }
    }
}