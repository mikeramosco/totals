package com.justanotherdeveloper.totalslite

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText

class EnterBirthdayPageListeners(private val activity: EnterBirthdayPageActivity) {

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
            activity.returnToEnterNamePage()
        }

        binding.continueButton.setOnClickListener {
            if(activity.getView().userIsOldEnough())
                activity.openPhoneNumberActivity()
            else activity.getView().showAgeRestrictionMessage()
        }
    }

    /** copied from other project: */

    private fun initTextFieldListener() {
        initFieldListener(binding.monthField)
        initFieldListener(binding.dayField)
        initFieldListener(binding.yearField)
    }

    private fun initFieldListener(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                activity.getView().checkForValidBirthday(editText)
            }
        })

        editText.setOnKeyListener { _, keyCode, event ->
            if(editText.text.toString().isEmpty() && event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                activity.getView().moveCursorToPreviousTextField(editText)
            }
            false
        }
    }

}