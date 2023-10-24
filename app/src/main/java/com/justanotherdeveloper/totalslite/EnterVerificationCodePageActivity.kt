package com.justanotherdeveloper.totalslite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.justanotherdeveloper.totalslite.databinding.ActivityEnterVerificationCodePageBinding

class EnterVerificationCodePageActivity : AppCompatActivity() {

    private lateinit var local: TinyDB

    private lateinit var view: EnterVerificationCodePageView
    private lateinit var listeners: EnterVerificationCodePageListeners
    private lateinit var authentication: PhoneNumberAuthentication
    private lateinit var accountEditor: TotalsAccountEditor

    private lateinit var binding: ActivityEnterVerificationCodePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)
        view = EnterVerificationCodePageView(this)
        listeners = EnterVerificationCodePageListeners(this)

        val verificationId = intent.getStringExtra(VERIFICATION_ID_REF)?: ""
        authentication = PhoneNumberAuthentication(verificationPage = this,
            verificationId = verificationId)

        accountEditor = TotalsAccountEditor(verificationPage = this)
    }

    private fun initBinding() {
        binding = ActivityEnterVerificationCodePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun checkIfAccountExists() {
        val fullPhoneNumber = getFullPhoneNumber()?: ""
        accountEditor.checkIfAccountExists(fullPhoneNumber)
    }

    fun signInWithPhoneNumber() {
        finish()
    }

    fun openCreateUsernamePage() {
        val intent = Intent(this, CreateUsernamePageActivity::class.java)
        intent.putExtra(PHONE_NUMBER_REF, getFullPhoneNumber())
        startActivityForResult(intent, 0)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    fun sendNewCode() {
        val phoneNumberPage = getPhoneNumberPage()
        if(phoneNumberPage != null) {
            phoneNumberPage.resendVerificationCode()
            view.restartResendCodeTimer()
        }
    }

    /** Override Methods */

    @Deprecated("Deprecated in Java", ReplaceWith("goToHomeScreen()"))
    override fun onBackPressed() {
        goToHomeScreen()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        signInWithPhoneNumber()
    }

    /** "Get" Methods */

    fun getDatabase(): TinyDB {
        return local
    }

    fun getBinding(): ActivityEnterVerificationCodePageBinding {
        return binding
    }

    fun getView(): EnterVerificationCodePageView {
        return view
    }

    fun getAuthentication(): PhoneNumberAuthentication {
        return authentication
    }

    fun getFullPhoneNumber(): String? {
        return intent.getStringExtra(PHONE_NUMBER_REF)
    }
}