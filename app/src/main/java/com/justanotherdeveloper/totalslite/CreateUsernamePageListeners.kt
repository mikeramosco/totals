package com.justanotherdeveloper.totalslite

import android.text.Editable
import android.text.TextWatcher

class CreateUsernamePageListeners(private val activity: CreateUsernamePageActivity) {

    private val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initTextFieldListener()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.continueButton)
    }

    private fun initOnClickListeners() {
        binding.continueButton.setOnClickListener {
            activity.continueButtonPressed()
        }
    }

    private fun initTextFieldListener() {
        binding.usernameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                activity.getView().setUsernameTakenErrorTextVisible(false)
                activity.getView().toggleEnabledContinueButton()
            }
        })
    }
}