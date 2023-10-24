package com.justanotherdeveloper.totalslite

import android.os.CountDownTimer
import android.view.View

class EnterVerificationCodePageView(private val activity: EnterVerificationCodePageActivity) {
    private lateinit var resendCodeTimer: CountDownTimer

    private val binding = activity.getBinding()

    private var timeRemaining = 0

    init {
        initResendCodeTimer()
        showPromptWithNumber()
        showResendCodeButtonWithTimer()

        binding.verificationPageParent.post {
            moveCursorTo(binding.verificationCodeField)
        }
    }

    private fun initResendCodeTimer() {
        resendCodeTimer = object : CountDownTimer(RESEND_CODE_WAIT_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                setResendCodeSecondsText(timeRemaining--)
            }
            override fun onFinish() {
                toggleEnabledResendCodeButton(true)
            }
        }
    }

    fun verifyCodeIfEntered() {
        hideErrorText()
        val code = binding.verificationCodeField.text.toString()
        if(code.length != 6) return
        disableResendCodeButton()
        activity.getAuthentication().verifyCode(code)
    }

    fun restartResendCodeTimer() {
        toggleEnabledResendCodeButton(false)
        showResendCodeButtonWithTimer()
    }

    fun showErrorText() {
        binding.incorrectVerificationCodeErrorText.visibility = View.VISIBLE
    }

    private fun hideErrorText() {
        binding.incorrectVerificationCodeErrorText.visibility = View.GONE
    }

    private fun toggleProgressCircleVisibility(isVisible: Boolean) {
        var resendCodeTextVisibility = View.VISIBLE
        var progressCircleVisibility = View.VISIBLE

        if(isVisible) {
            binding.verificationCodeField.isFocusable = false
            resendCodeTextVisibility = View.GONE
        } else {
            binding.verificationCodeField.isFocusableInTouchMode = true
            progressCircleVisibility = View.GONE
        }

        binding.resendCodeText.visibility = resendCodeTextVisibility
        binding.progressCircle.visibility = progressCircleVisibility
    }

    private fun disableResendCodeButton() {
        resendCodeTimer.cancel()
        toggleEnabledResendCodeButton(false)
        toggleProgressCircleVisibility(true)
    }

    private fun resetTimeRemaining() {
        timeRemaining = (RESEND_CODE_WAIT_TIME / 1000).toInt()
    }

    private fun showPromptWithNumber() {
        val phoneNumber = activity.getFullPhoneNumber()?: return
        binding.enterVerificationCodePromptText.text =
            activity.getString(R.string.enterVerificationCodePrompt, phoneNumber)
    }

    private fun showResendCodeButtonWithTimer() {
        toggleProgressCircleVisibility(false)
        hideErrorText()
        resetTimeRemaining()
        setResendCodeSecondsText(timeRemaining)
        resendCodeTimer.start()
    }

    private fun setResendCodeSecondsText(seconds: Int) {
        binding.resendCodeText.text = activity.getString(R.string.resendInSOption, seconds.toString())
    }

    private fun toggleEnabledResendCodeButton(enable: Boolean) {
        var disabledButtonVisibility = View.VISIBLE
        var continueButtonVisibility = View.VISIBLE

        if(enable)
            disabledButtonVisibility = View.GONE
        else continueButtonVisibility = View.GONE

        binding.waitToResendCodeButton.visibility = disabledButtonVisibility
        binding.sendNewCodeButton.visibility = continueButtonVisibility
    }
}