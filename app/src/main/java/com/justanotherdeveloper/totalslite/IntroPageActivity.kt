package com.justanotherdeveloper.totalslite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.justanotherdeveloper.totalslite.databinding.ActivityIntroPageBinding

/**
 * This page is shown after the splash page when app is
 * launched for the first time or if user is logged out:
 * shows app description/details and sign up/login options.
 */
class IntroPageActivity : AppCompatActivity() {

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivityIntroPageBinding

    /** 'listeners' holds all listener methods for this activity */
    private lateinit var listeners: IntroPageListeners

    /** Local database */
    private lateinit var local: TinyDB

    private var tutorialFinished = false
    private var pageNumber = 1

    /** Main */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)
        listeners = IntroPageListeners(this)

        if(isSignedIn(local)) openHomePage()
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityIntroPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun showPage(pageToOpen: Int) {
        Handler().post {
            binding.continueText.text = if(tutorialFinished)
                getString(R.string.option_get_started)
            else getString(R.string.option_continue)
        }
        if(pageToOpen == pageNumber) return
        if(pageToOpen == 4)
            tutorialFinished = true

        fun getIntroTextOfPage(pageNumber: Int): TextView {
            return when(pageNumber) {
                1 -> binding.introText1
                2 -> binding.introText2
                3 -> binding.introText3
                else -> binding.introText4
            }
        }

        fun getIntroBubbleOfPage(pageNumber: Int): ImageView {
            return when(pageNumber) {
                1 -> binding.introBubble1
                2 -> binding.introBubble2
                3 -> binding.introBubble3
                else -> binding.introBubble4
            }
        }

        val introTextCurrent = getIntroTextOfPage(pageNumber)
        val introTextNext = getIntroTextOfPage(pageToOpen)
        val introBubbleCurrent = getIntroBubbleOfPage(pageNumber)
        val introBubbleNext = getIntroBubbleOfPage(pageToOpen)
        val introPhotoCodeNext = when(pageToOpen) {
            1 -> R.drawable.introphoto1
            2 -> R.drawable.introphoto2
            3 -> R.drawable.introphoto3
            else -> R.drawable.introphoto4
        }

        showNextPage(introTextCurrent, introTextNext,
            introBubbleCurrent, introBubbleNext, introPhotoCodeNext, pageToOpen)
    }

    fun continuePressed() {
        when(pageNumber) {
            1 -> showPage(2)
            2 -> showPage(3)
            3 -> showPage(4)
            4 -> openEnterNamePage()
        }
    }

    private fun showNextPage(introTextCurrent: TextView, introTextNext: TextView,
                             introBubbleCurrent: ImageView, introBubbleNext: ImageView,
                             introPhotoCodeNext: Int, pageToOpen: Int) {
        beginTransition(binding.introPageParent)
        introTextCurrent.visibility = View.GONE
        introTextNext.visibility = View.VISIBLE
        introBubbleCurrent.setImageResource(R.drawable.ic_radio_button_unchecked)
        introBubbleNext.setImageResource(R.drawable.ic_circle_filled_white)
        if(binding.progressPhoto2.isVisible) {
            binding.progressPhoto.setImageResource(introPhotoCodeNext)
            binding.progressPhoto2.visibility = View.GONE
        } else {
            binding.progressPhoto2.setImageResource(introPhotoCodeNext)
            binding.progressPhoto2.visibility = View.VISIBLE
        }
        pageNumber = pageToOpen
    }

    /**
     * Opens Enter Name Page - first step of sign up/login flow;
     * shows animation of page sliding from right
     */
    @Suppress("DEPRECATION")
    fun openEnterNamePage() {
        tutorialFinished = false
        showPage(1)
        val intent = Intent(this, EnterNamePageActivity::class.java)
        startActivityForResult(intent, 0)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    @Suppress("DEPRECATION")
    private fun openHomePage() {
        val intent = Intent(this, HomePageActivity::class.java)
        startActivityForResult(intent, 0)
    }

    private fun showBannedNoticeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_user_banned_message, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val bannedNoticeDialog = builder.create()
        bannedNoticeDialog.setCancelable(true)

        bannedNoticeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val okButton = view.findViewById<LinearLayout>(R.id.okButton)
        initButtonAnimationListener(okButton)
        okButton.setOnClickListener {
            bannedNoticeDialog.cancel()
        }

        bannedNoticeDialog.show()
    }

    /** Override Methods */

    /** minimizes app when back pressed */
    override fun onBackPressed() {
        goToHomeScreen()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(signedOutFromBan()) showBannedNoticeDialog()
        else if(isSignedIn(local)) openHomePage()
    }

    /** "Get" Methods */

    fun getBinding(): ActivityIntroPageBinding {
        return binding
    }

    fun getPageNumber(): Int {
        return pageNumber
    }

    fun tutorialFinished(): Boolean {
        return tutorialFinished
    }
}