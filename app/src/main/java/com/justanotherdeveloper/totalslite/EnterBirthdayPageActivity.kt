package com.justanotherdeveloper.totalslite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.justanotherdeveloper.totalslite.databinding.ActivityEnterBirthdayPageBinding

/** Enter Birthday Page - 2nd step of sign up/login flow */
class EnterBirthdayPageActivity : AppCompatActivity() {

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivityEnterBirthdayPageBinding

    /** 'view' holds all view methods for this activity */
    private lateinit var view: EnterBirthdayPageView

    /** 'listeners' holds all listener methods for this activity */
    private lateinit var listeners: EnterBirthdayPageListeners

    /** Local database */
    private lateinit var local: TinyDB

    private var otherProcessStarted = false

    /** Main */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)
        view = EnterBirthdayPageView(this)
        listeners = EnterBirthdayPageListeners(this)
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityEnterBirthdayPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun returnToEnterNamePage() {
        if(otherProcessStarted()) return
        finish()
    }

    /**
     * Opens Enter Phone Number Page - third step of sign up/login flow;
     * shows animation of page sliding from right
     */
    @Suppress("DEPRECATION")
    fun openPhoneNumberActivity() {
        if(otherProcessStarted()) return
        view.formatBirthdayFields()
        local.putString(BIRTHDAY_REF, view.getFormattedBirthdayString())
        val intent = Intent(this, EnterPhoneNumberPageActivity::class.java)
        startActivityForResult(intent, 0)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
        if(isSignedIn(local)) returnToEnterNamePage()
    }

    /** "Get" Methods */

    fun getBinding(): ActivityEnterBirthdayPageBinding {
        return binding
    }

    fun getView(): EnterBirthdayPageView {
        return view
    }

    fun getName(): String {
        return local.getString(NAME_REF)?: ""
    }

    fun getBirthdayString(): String {
        return local.getString(BIRTHDAY_REF)?: ""
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