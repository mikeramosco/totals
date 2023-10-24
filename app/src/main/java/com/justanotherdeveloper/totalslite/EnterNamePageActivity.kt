package com.justanotherdeveloper.totalslite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.justanotherdeveloper.totalslite.databinding.ActivityEnterNamePageBinding

/**
 * This page is the first step of sign up/login flow;
 * the entered name will be saved when 'continue' is pressed
 * and will autofill name field with the saved name whenever
 * user returns to this page in the future.
 */
class EnterNamePageActivity : AppCompatActivity() {

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivityEnterNamePageBinding

    /** 'view' holds all view methods for this activity */
    private lateinit var view: EnterNamePageView

    /** 'listeners' holds all listener methods for this activity */
    private lateinit var listeners: EnterNamePageListeners

    /** Local database */
    private lateinit var local: TinyDB

    private var otherProcessStarted = false

    /** Main */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)
        view = EnterNamePageView(this)
        listeners = EnterNamePageListeners(this)
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityEnterNamePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Opens Enter Birthday Page - second step of sign up/login flow;
     * shows animation of page sliding from right
     */
    @Suppress("DEPRECATION")
    fun openEnterBirthdayPage() {
        if(otherProcessStarted()) return
        local.putString(NAME_REF, binding.nameField.text.toString())
        val intent = Intent(this, EnterBirthdayPageActivity::class.java)
        startActivityForResult(intent, 0)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    fun returnToIntroPage() {
        if(otherProcessStarted()) return
        finish()
    }

    /** Override Methods */

    /**
     * Returns to enter name page with closing animation
     * of this page sliding to the left if not yet signed in
     */
    override fun finish() {
        super.finish()
        if(!isSignedIn(local))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setOtherProcessStarted(false)
        if(isSignedIn(local)) returnToIntroPage()
    }

    /** "Get" Methods */

    fun getBinding(): ActivityEnterNamePageBinding {
        return binding
    }

    fun getView(): EnterNamePageView {
        return view
    }

    fun getName(): String {
        return local.getString(NAME_REF)?: ""
    }

    fun otherProcessStarted(setTrue: Boolean = true): Boolean {
        return if(otherProcessStarted) true else {
            if(setTrue) setOtherProcessStarted()
            false
        }
    }

    fun setOtherProcessStarted(otherProcessStarted: Boolean = true) {
        this.otherProcessStarted = otherProcessStarted
    }
}