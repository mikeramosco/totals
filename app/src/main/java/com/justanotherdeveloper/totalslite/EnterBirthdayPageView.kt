package com.justanotherdeveloper.totalslite

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import java.time.LocalDate
import java.time.Period

/** copied from other project */
class EnterBirthdayPageView(private val activity: EnterBirthdayPageActivity) {

    private val binding = activity.getBinding()

    init {
        showPromptWithName()
        setTextFieldsToSavedBirthday()
        moveCursorToBirthdayTextFields()
    }

    private fun showPromptWithName() {
        binding.enterBirthdayPagePromptText.text =
            activity.getString(R.string.enterBirthdayPagePrompt, activity.getName())
    }

    private fun setTextFieldsToSavedBirthday() {
        val birthdayString = activity.getBirthdayString()
        if(birthdayString.isEmpty()) return
        val birthdayContents = birthdayString.split("-")
        val month = birthdayContents[1]
        val day = birthdayContents[0]
        val year = birthdayContents[2]

        binding.monthField.setText(month)
        binding.dayField.setText(day)
        binding.yearField.setText(year)
    }

    private fun moveCursorToBirthdayTextFields() {
        binding.enterBirthdayPageParent.post {
            if(binding.monthField.text.toString().isEmpty()) {
                binding.monthField.requestFocus()
                toggleEnabledContinueButton(false)
            } else {
                moveCursorTo(binding.yearField)
                toggleEnabledContinueButton(true)
            }
        }
    }

    fun checkForValidBirthday(editText: EditText) {
        val month = binding.monthField.text.toString()
        val day = binding.dayField.text.toString()
        val year = binding.yearField.text.toString()

        fun moveCursorToNextField() {
            val nextField = if(day.length == 2)
                binding.yearField else binding.dayField
            moveCursorTo(nextField)
        }

        fun adjustDateFields(fieldToCheck: String) {
            if(fieldToCheck.length == 2)
                moveCursorToNextField()
            else toggleErrorMessage()
        }

        when(editText) {
            binding.monthField -> adjustDateFields(month)
            binding.dayField -> adjustDateFields(day)
            binding.yearField ->
                if(year.length < 4) toggleErrorMessage()
        }

        if(month.isNotEmpty() && day.isNotEmpty() && year.length == 4) {
            val dateExists = dateExists(getFormattedBirthdayString())
            val dateIsValid = if(dateExists) {
                val yearAsInt = year.toInt()
                val monthAsInt = month.toInt()
                val dayAsInt = day.toInt()

                val age = getAge(yearAsInt, monthAsInt, dayAsInt)
                val todaysDate = getTodaysDate().resetTimeOfDay()
                val dateAsCal = createCalendar(yearAsInt, monthAsInt, dayAsInt)
                age < MAX_AGE && todaysDate.comesAfter(dateAsCal, true)
            } else false

            toggleEnabledContinueButton(dateIsValid)
            toggleErrorMessage(!dateIsValid)
        } else toggleEnabledContinueButton(false)
    }

    private fun getAge(year: Int, month: Int, dayOfMonth: Int): Int {
        return Period.between(
            LocalDate.of(year, month, dayOfMonth),
            LocalDate.now()
        ).years
    }

    private fun toggleErrorMessage(showMessage: Boolean = false) {
        val visibility = if(showMessage) View.VISIBLE else View.GONE
        binding.birthdayErrorText.visibility = visibility
    }

    private fun toggleEnabledContinueButton(enableButton: Boolean) {
        var disabledButtonVisibility = View.VISIBLE
        var continueButtonVisibility = View.VISIBLE

        if(enableButton)
            disabledButtonVisibility = View.GONE
        else continueButtonVisibility = View.GONE

        binding.continueButtonDisabled.visibility = disabledButtonVisibility
        binding.continueButton.visibility = continueButtonVisibility
    }

    fun moveCursorToPreviousTextField(editText: EditText) {
        when(editText) {
            binding.yearField -> moveCursorTo(binding.dayField)
            binding.dayField -> moveCursorTo(binding.monthField)
        }
    }

    private fun getFormattedMonthString(): String {
        var month = binding.monthField.text.toString()
        if(month.length == 1) month = "0$month"
        return month
    }

    private fun getFormattedDayString(): String {
        var day = binding.dayField.text.toString()
        if(day.length == 1) day = "0$day"
        return day
    }

    fun getFormattedBirthdayString(): String {
        return "${getFormattedDayString()}-" +
                "${getFormattedMonthString()}-" +
                "${binding.yearField.text}"
    }

    fun formatBirthdayFields() {
        binding.monthField.setText(getFormattedMonthString())
        binding.dayField.setText(getFormattedDayString())
        moveCursorTo(binding.yearField)
    }

    fun userIsOldEnough(): Boolean {
        val month = binding.monthField.text.toString().toInt()
        val day = binding.dayField.text.toString().toInt()
        val year = binding.yearField.text.toString().toInt()
        return getAge(year, month, day) >= MIN_AGE
    }

    fun showAgeRestrictionMessage() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_age_restriction_message, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val ageRestrictionDialog = builder.create()
        ageRestrictionDialog.setCancelable(true)

        ageRestrictionDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val okButton = view.findViewById<LinearLayout>(R.id.okButton)
        initButtonAnimationListener(okButton)
        okButton.setOnClickListener {
            ageRestrictionDialog.dismiss()
        }

        ageRestrictionDialog.show()
    }
}