package com.aistudio.fitmirror.auth2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aistudio.fitmirror.auth2.databinding.ActivityMainBinding
import com.aistudio.fitmirror.auth2.ui.driver.DriverActivity
import com.aistudio.fitmirror.auth2.ui.finder.FinderActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.cardDriver.setOnClickListener {
            startActivity(Intent(this, DriverActivity::class.java))
        }

        binding.cardFinder.setOnClickListener {
            startActivity(Intent(this, FinderActivity::class.java))
        }
    }
}