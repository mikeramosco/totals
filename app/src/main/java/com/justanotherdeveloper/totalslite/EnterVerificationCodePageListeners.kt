package com.justanotherdeveloper.totalslite

import android.text.Editable
import android.text.TextWatcher

class EnterVerificationCodePageListeners(private val activity: EnterVerificationCodePageActivity) {

    private val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initTextFieldListener()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.sendNewCodeButton)
    }

    private fun initOnClickListeners() {
        binding.sendNewCodeButton.setOnClickListener {
            activity.sendNewCode()
        }
    }

    private fun initTextFieldListener() {
        binding.verificationCodeField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                activity.getView().verifyCodeIfEntered()
            }
        })
    }
}