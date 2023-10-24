package com.justanotherdeveloper.totalslite

import android.view.View
import androidx.core.view.isVisible
import java.util.regex.Pattern

class EnterPhoneNumberPageView(private val activity: EnterPhoneNumberPageActivity) {

    private val binding = activity.getBinding()

    init {
        val countryCode = activity.getCountryCode()
        if(countryCode > 0) {
            binding.phoneNumberField.setText(activity.getPhoneNumber())
            binding.countryCodePicker.setCountryForPhoneCode(countryCode)
        }
        binding.enterPhoneNumberPageParent.post {
            moveCursorTo(binding.phoneNumberField)
            binding.countryCodePicker
                .registerCarrierNumberEditText(
                    binding.phoneNumberField)
        }
    }

    // code found on:
    // https://tutorialspots.com/android-how-to-check-a-valid-phone-number-2382.html
    private fun phoneNumberIsValid(phoneNumber: String): Boolean {
        val expression = "^([0-9\\+]|\\(\\d{1,3}\\))[0-9\\-\\. ]{3,15}$"
        val pattern = Pattern.compile(expression)
        val matcher = pattern.matcher(phoneNumber)
        return matcher.matches()
    }

    /**
     * enables continue button
     * if phone number field is not empty
     * or is otherwise disabled
     */
    private fun toggleEnabledContinueButton(enable: Boolean) {
        var disabledButtonVisibility = View.VISIBLE
        var continueButtonVisibility = View.VISIBLE

        if(enable)
            disabledButtonVisibility = View.GONE
        else continueButtonVisibility = View.GONE

        binding.continueButtonDisabled.visibility = disabledButtonVisibility
        binding.continueButton.visibility = continueButtonVisibility
    }

    fun toggleEnabledContinueButton() {
        val phoneNumber = binding.phoneNumberField.text.toString()
        toggleEnabledContinueButton(phoneNumberIsValid(phoneNumber))
    }

    fun getPhoneNumber(): String {
        return binding.countryCodePicker.fullNumber
    }

    fun toggleLoadingContinueButton() {
        val authInProgress = activity.getAuthentication().authInProgress()
        toggleEnabledContinueButton(!authInProgress)

        var progressCircleVisibility = View.VISIBLE
        var continueTextVisibility = View.VISIBLE

        if(authInProgress) {
            binding.phoneNumberField.isFocusable = false
            continueTextVisibility = View.GONE
        } else {
            binding.phoneNumberField.isFocusableInTouchMode = true
            progressCircleVisibility = View.GONE
        }

        binding.continueText.visibility = continueTextVisibility
        binding.progressCircle.visibility = progressCircleVisibility
    }

    fun toggleErrorTextVisibility() {
        binding.phoneNumberVerificationErrorText.visibility =
            if(activity.getAuthentication().authInProgress())
                View.GONE else View.VISIBLE
    }

    fun getFullPhoneNumber(): String {
        return "+${binding.countryCodePicker.fullNumber}"
    }

    fun disableBackButton() {
        binding.backButton.visibility = View.GONE
        binding.actionBarSpacer.visibility = View.VISIBLE
    }

    fun backButtonEnabled(): Boolean {
        return binding.backButton.isVisible
    }

}