package com.example.triptogether

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.triptogether.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("imageUrl")
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.ivFullImage)
        } else {
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
