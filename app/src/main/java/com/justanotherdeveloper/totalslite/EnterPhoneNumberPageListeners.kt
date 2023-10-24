package com.justanotherdeveloper.totalslite

import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher

class EnterPhoneNumberPageListeners(private val activity: EnterPhoneNumberPageActivity) {

    private val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initTextFieldListener()
    }

    /**
     * Adds animation to a view to transition to
     * another state/color when pressed/released
     */
    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.moreButton)
        initButtonAnimationListener(binding.continueButton)
    }

    /** 'setOnClickListener' methods */
    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.returnToEnterBirthdayPage()
        }

        binding.continueButton.setOnClickListener {
            activity.initVerification()
        }

        binding.learnMoreLink.setOnClickListener {
            activity.learnMoreClicked()
        }
    }

    /**
     * Calls method in view class
     * to enable continue button
     * if text field is not empty
     * or is otherwise disabled
     */
    private fun initTextFieldListener() {
        binding.phoneNumberField.addTextChangedListener(PhoneNumberFormattingTextWatcher())

        binding.phoneNumberField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                activity.getView().toggleEnabledContinueButton()
            }
        })
    }

}