package com.justanotherdeveloper.totalslite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import com.google.firebase.auth.PhoneAuthProvider
import com.hbb20.CountryCodePicker
import com.justanotherdeveloper.totalslite.databinding.ActivityEnterPhoneNumberPageBinding

/** Enter Phone Number Page - 2nd step of sign up/login flow */
class EnterPhoneNumberPageActivity : AppCompatActivity() {

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivityEnterPhoneNumberPageBinding

    /** 'view' holds all view methods for this activity */
    private lateinit var view: EnterPhoneNumberPageView

    /** 'listeners' holds all listener methods for this activity */
    private lateinit var listeners: EnterPhoneNumberPageListeners

    /** 'authentication' holds all methods to authenticate entered phone number */
    private lateinit var authentication: PhoneNumberAuthentication

    /** Local database */
    private lateinit var local: TinyDB

    /** sets true when verification code sent successfully & next page opens */
    private var verificationPageOpen = false

    private var otherProcessStarted = false

    /** Main */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        setPhoneNumberPage(this)
        local = TinyDB(this)
        view = EnterPhoneNumberPageView(this)
        listeners = EnterPhoneNumberPageListeners(this)
        authentication = PhoneNumberAuthentication(enterNumberPage = this)
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityEnterPhoneNumberPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun returnToEnterBirthdayPage() {
        if(otherProcessStarted()) return
        finish()
    }

    fun initVerification() {
        if(verificationPageOpen) return
        authentication.sendVerificationCode()
        view.toggleLoadingContinueButton()
        view.disableBackButton()
    }

    fun learnMoreClicked() {
        openLink(LEGAL_POLICIES_SITE)
    }

    private var token: PhoneAuthProvider.ForceResendingToken? = null

    fun codeSentSuccessfully(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
        this.token = token

        if(!verificationPageOpen) {
            openVerificationPage(verificationId)
            view.toggleLoadingContinueButton()
        }
    }

    fun resendVerificationCode() {
        authentication.resendVerificationCode(token)
    }

    @Suppress("DEPRECATION")
    private fun openVerificationPage(verificationId: String) {
        if(otherProcessStarted()) return
        val countryCodePicker = findViewById<CountryCodePicker>(R.id.countryCodePicker)
        val phoneNumberField = findViewById<EditText>(R.id.phoneNumberField)

        local.putString(PHONE_NUMBER_REF, phoneNumberField.text.toString())
        local.putInt(COUNTRY_CODE_REF, countryCodePicker.selectedCountryCode.toInt())
        val intent = Intent(this, EnterVerificationCodePageActivity::class.java)
        intent.putExtra(PHONE_NUMBER_REF, view.getFullPhoneNumber())
        intent.putExtra(VERIFICATION_ID_REF, verificationId)
        startActivityForResult(intent, 0)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        verificationPageOpen = true
    }

    /** Override Methods */

    /**
     * Returns to enter name page with closing animation
     * of this page sliding to the left if not yet signed in
     */
    override fun finish() {
        super.finish()
        setPhoneNumberPage(null)
        if(!isSignedIn(local))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(view.backButtonEnabled()) {
            if(otherProcessStarted()) return
            super.onBackPressed()
        } else goToHomeScreen()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setOtherProcessStarted(false)
        if(isSignedIn(local)) returnToEnterBirthdayPage()
    }

    /** "Get" Methods */

    fun getBinding(): ActivityEnterPhoneNumberPageBinding {
        return binding
    }

    fun getView(): EnterPhoneNumberPageView {
        return view
    }

    fun getAuthentication(): PhoneNumberAuthentication {
        return authentication
    }

    fun getPhoneNumber(): String {
        return local.getString(PHONE_NUMBER_REF)?: ""
    }

    fun getCountryCode(): Int {
        return local.getInt(COUNTRY_CODE_REF)
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