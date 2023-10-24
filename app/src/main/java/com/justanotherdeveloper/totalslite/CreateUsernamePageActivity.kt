package com.justanotherdeveloper.totalslite

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.justanotherdeveloper.totalslite.databinding.ActivityCreateUsernamePageBinding

class CreateUsernamePageActivity : AppCompatActivity() {

    private lateinit var database: TinyDB

    private lateinit var view: CreateUsernamePageView
    private lateinit var listeners: CreateUsernamePageListeners
    private lateinit var accountEditor: TotalsAccountEditor

    private lateinit var binding: ActivityCreateUsernamePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateUsernamePageBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        database = TinyDB(this)

        view = CreateUsernamePageView(this)
        listeners = CreateUsernamePageListeners(this)
        accountEditor = TotalsAccountEditor(usernamePage = this)
    }

    fun getBinding(): ActivityCreateUsernamePageBinding {
        return binding
    }

    fun continueButtonPressed() {
        binding.usernameField.toLowerCase()
        view.showProgressCircle()
        accountEditor.checkAvailability(binding.usernameField.text.toString())
    }

    fun signInWithPhoneNumber(totalsUser: TotalsUser) {
        setSignedInTotalsUser(database, totalsUser, true)
        finish()
    }

    /** Override Methods */

    @Deprecated("Deprecated in Java", ReplaceWith("goToHomeScreen()"))
    override fun onBackPressed() {
        goToHomeScreen()
    }

    /** "Get" Methods */

    fun getView(): CreateUsernamePageView {
        return view
    }

    fun accounts(): TotalsAccountEditor {
        return accountEditor
    }

    fun getName(): String {
        return database.getString(NAME_REF)?: ""
    }

    fun getBirthday(): String {
        return database.getString(BIRTHDAY_REF)?: ""
    }

    fun getFullPhoneNumber(): String? {
        return intent.getStringExtra(PHONE_NUMBER_REF)
    }
}