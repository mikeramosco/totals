package com.justanotherdeveloper.totalslite

import android.view.View

class EnterNamePageView(private val activity: EnterNamePageActivity) {

    private val binding = activity.getBinding()

    init {
        // autofills text field with saved name if there is one
        binding.nameField.setText(activity.getName())

        // puts cursor to end of text of name text field
        binding.enterNamePageParent.post {
            moveCursorTo(binding.nameField)
            toggleEnabledContinueButton()
        }
    }

    /**
     * enables continue button
     * if name field is not empty
     * or is otherwise disabled
     */
    fun toggleEnabledContinueButton() {
        val nameString = binding.nameField.text.toString()

        var disabledButtonVisibility = View.VISIBLE
        var continueButtonVisibility = View.VISIBLE

        if(nameString.isNotEmpty())
            disabledButtonVisibility = View.GONE
        else continueButtonVisibility = View.GONE

        binding.continueButtonDisabled.visibility = disabledButtonVisibility
        binding.continueButton.visibility = continueButtonVisibility
    }

}