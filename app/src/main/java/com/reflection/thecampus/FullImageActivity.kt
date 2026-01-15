package com.reflection.thecampus

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullImageActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_image)
        
        // Make status bar transparent
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: return
        
        val ivFullImage = findViewById<ImageView>(R.id.ivFullImage)
        val btnClose = findViewById<ImageView>(R.id.btnClose)
        
        // Load image
        Glide.with(this)
            .load(imageUrl)
            .into(ivFullImage)
        
        // Close button
        btnClose.setOnClickListener {
            finish()
        }
        
        // Click anywhere to close
        ivFullImage.setOnClickListener {
            finish()
        }
    }
}
