package com.justanotherdeveloper.totalslite

import android.view.View

class CreateUsernamePageView(private val activity: CreateUsernamePageActivity) {

    private val binding = activity.getBinding()

    init {
        binding.createUsernamePageParent.post {
            moveCursorTo(binding.usernameField)
        }
    }

    fun toggleEnabledContinueButton() {
        val nameString = binding.usernameField.text.toString()

        var disabledButtonVisibility = View.VISIBLE
        var continueButtonVisibility = View.VISIBLE

        if(nameString.length >= MIN_USERNAME_CHAR &&
            !activity.accounts().accountManagementInProgress())
            disabledButtonVisibility = View.GONE
        else continueButtonVisibility = View.GONE

        binding.continueButtonDisabled.visibility = disabledButtonVisibility
        binding.continueButton.visibility = continueButtonVisibility
    }

    fun setUsernameTakenErrorTextVisible(isVisible: Boolean = true) {
        binding.usernameTakenErrorText.visibility =
            if(isVisible) View.VISIBLE else View.GONE
    }

    fun showProgressCircle() {
        toggleEnabledContinueButton()
        binding.progressCircle.visibility = View.VISIBLE
        binding.disabledContinueText.visibility = View.GONE
    }

    fun hideProgressCircle() {
        toggleEnabledContinueButton()
        binding.progressCircle.visibility = View.GONE
        binding.disabledContinueText.visibility = View.VISIBLE
    }
}