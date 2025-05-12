package com.example.agroaviedocalling.activity

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.BounceInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agroaviedocalling.R
import com.example.agroaviedocalling.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivitySplashBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bounceLogo()
        splashLooper()

    }

    private fun bounceLogo() {
        val animator = ObjectAnimator.ofFloat(binding.logoImage, "translationY", 0f, -300f, 0f)
        animator.duration = 1500
        animator.repeatCount = 2 // 3 total bounces = 1 original + 2 repeats
        animator.interpolator = BounceInterpolator()
        animator.start()
    }

    private fun splashLooper(){
        Handler(Looper.getMainLooper()).postDelayed(3000){
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

    }
}