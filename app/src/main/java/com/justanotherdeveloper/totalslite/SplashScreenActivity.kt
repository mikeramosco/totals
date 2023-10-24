package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.justanotherdeveloper.totalslite.databinding.ActivitySplashScreenBinding

/**
 * Main Activity on app launch
 * - shows logo fade in
 * - opens intro page after a delay or when screen is tapped
 */
@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    /** set true when openIntroPage called */
    private var introPageOpened = false

    /**
     * 'handler' used to have a delay before running
     * some methods on another thread
     */
    @Suppress("DEPRECATION")
    private val handler = Handler()

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivitySplashScreenBinding

    /** Main */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()
        initTapToSkipListener()
        showLogoAfterDelay()
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Adds listener to layout to open
     * intro page when screen is tapped
     */
    private fun initTapToSkipListener() {
        binding.tapToSkipLayout.setOnClickListener { openIntroPage() }
    }

    private fun showLogoAfterDelay() {
        handler.postDelayed({
            showIntroPageAfterDelay()
        }, LOGO_FADE_DELAY)
    }

    private fun showIntroPageAfterDelay() {

        // Logo fade in
        beginTransition(binding.logoParent)
        binding.logo.visibility = View.VISIBLE

        // Opens intro page after delay if not already launched
        handler.postDelayed({ openIntroPage() }, SPLASH_SCREEN_DELAY)
    }

    private fun openIntroPage() {
        if(introPageOpened) return
        introPageOpened = true
        val intent = Intent(this, IntroPageActivity::class.java)
        startActivity(intent)
        finish()
    }
}