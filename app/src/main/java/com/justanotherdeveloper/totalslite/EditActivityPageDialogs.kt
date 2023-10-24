package com.justanotherdeveloper.totalslite

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*

class EditActivityPageDialogs(private val activity: EditActivityPageActivity) {

    fun showConfirmClosePageDialog() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_close_page, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmClosePageDialog = builder.create()
        confirmClosePageDialog.setCancelable(true)

        confirmClosePageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val detailsText = view.findViewById<TextView>(R.id.detailsText)
        detailsText.text = activity.getString(R.string.closeEditGoalPageDialogDetails)

        val keepEditingButton = view.findViewById<LinearLayout>(R.id.keepEditingButton)
        initButtonAnimationListener(keepEditingButton)
        keepEditingButton.setOnClickListener {
            confirmClosePageDialog.cancel()
        }

        val closeButton = view.findViewById<LinearLayout>(R.id.closeButton)
        closeButton.setOnClickListener {
            activity.finish()
            confirmClosePageDialog.cancel()
        }

        confirmClosePageDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            confirmClosePageDialog.dismiss()
        }

        confirmClosePageDialog.show()
    }

    fun addLinkDialogClosed() {
        activity.setOtherProcessStarted(false)
    }

    fun addLinkButtonClicked(linkField: EditText) {
        val linkText = linkField.text.toString()
        activity.getGoal().getLinks().add(linkText)
        activity.getView().addLinkView(linkText)
        activity.getView().updateAddLinkButton()
    }

    fun noteDialogClosed(noteField: EditText, noteView: View?, noteIndex: Int) {
        activity.setOtherProcessStarted(false)
        val noteText = noteField.text.toString()
        if(noteView == null) {
            if(noteText.isNotEmpty()) {
                activity.getGoal().getNotes().add(noteText)
                activity.getView().addNoteView(noteText)
                activity.getView().updateAddNoteButton()
            }
        } else {
            if(noteText.isEmpty()) activity.deleteNote(noteIndex, noteView)
            else {
                activity.getGoal().getNotes()[noteIndex] = noteText
                updateNoteView(noteView, noteText)
            }
        }
    }

    fun openTimeDialog() {
        val goal = activity.getGoal()
        val currentTime = getTodaysDate()
        var hour = currentTime.get(Calendar.HOUR_OF_DAY)
        var minute = currentTime.get(Calendar.MINUTE)
        val savedTime = getSavedTimeDataString(activity.getDatabase())
        if(goal.hasTime()) {
            hour = goal.getHour()
            minute = goal.getMinute()
        } else if(savedTime != null) {
            hour = getHourFromTimeDataString(savedTime)
            minute = getMinuteFromTimeDataString(savedTime)
        }
        showTimePickerDialog(hour, minute, activity.supportFragmentManager)
    }

    private fun showTimePickerDialog(hour: Int, minute: Int,
                                     supportFragmentManager: FragmentManager) {
        val timePicker = TimePickerFragment(hour, minute)
        timePicker.show(supportFragmentManager, "time picker")
    }

    fun showChooseCalendarMethodDialog() {
        val chooseCalendarMethodDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_choose_calendar_method, null)
        chooseCalendarMethodDialog.setContentView(view)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(view.findViewById(R.id.addDateOption))
        dialogOptions.add(view.findViewById(R.id.repeatOption))
        initDialogOptions(chooseCalendarMethodDialog, dialogOptions)

        fun clickOption(option: LinearLayout) {
            when(option) {
                dialogOptions[0] -> showDatePickerDialog(
                    activity.getGoal().getDate(), activity.supportFragmentManager)
                dialogOptions[1] -> showChooseRepeatingDaysDialog()
            }

            chooseCalendarMethodDialog.dismiss()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        chooseCalendarMethodDialog.show()
    }

    private fun showDatePickerDialog(setDate: Calendar?, supportFragmentManager: FragmentManager) {
        val date = setDate ?: getTodaysDate()
        val datePicker = DatePickerFragment(date, getTodaysDate(), null)
        datePicker.show(supportFragmentManager, "date picker")
    }

    private fun showChooseRepeatingDaysDialog() {
        val repeatingDays = activity.getGoal().getRepeatingDays()
        val view = activity.layoutInflater.inflate(
            R.layout.dialog_choose_repeating_days, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val chooseRepeatingDaysDialog = builder.create()
        chooseRepeatingDaysDialog.setCancelable(true)

        val defaultColor = ContextCompat.getColor(activity, R.color.colorLightText)
        val defaultBackground = ContextCompat.getDrawable(
            activity, R.drawable.ic_circle_outline_gray)
        val selectedBackground = ContextCompat.getDrawable(
            activity, R.drawable.ic_circle_filled_custom)

        val disabledButton = view.findViewById<LinearLayout>(R.id.saveButtonDisabled)
        val doneOption = view.findViewById<LinearLayout>(R.id.saveButton)
        initButtonAnimationListener(doneOption)

        val dayOptions = ArrayList<TextView>()
        dayOptions.add(view.findViewById(R.id.sundayOption))
        dayOptions.add(view.findViewById(R.id.mondayOption))
        dayOptions.add(view.findViewById(R.id.tuesdayOption))
        dayOptions.add(view.findViewById(R.id.wednesdayOption))
        dayOptions.add(view.findViewById(R.id.thursdayOption))
        dayOptions.add(view.findViewById(R.id.fridayOption))
        dayOptions.add(view.findViewById(R.id.saturdayOption))

        val daysSelected = ArrayList<Boolean>()
        for(option in dayOptions)
            daysSelected.add(false)

        var repeatingDaysCount = 0

        fun toggleEnabledSaveButton() {
            val visibilitySave = if(repeatingDaysCount > 0)
                View.VISIBLE else View.GONE
            val visibilityDisabled = if(repeatingDaysCount == 0)
                View.VISIBLE else View.GONE

            doneOption.visibility = visibilitySave
            disabledButton.visibility = visibilityDisabled
        }

        fun toggleOption(dayIndex: Int) {
            val selected = daysSelected[dayIndex]
            if(selected) {
                daysSelected[dayIndex] = false
                dayOptions[dayIndex].setTextColor(defaultColor)
                dayOptions[dayIndex].background = defaultBackground
                repeatingDaysCount--
                toggleEnabledSaveButton()
            } else {
                daysSelected[dayIndex] = true
                dayOptions[dayIndex].setTextColor(Color.BLACK)
                dayOptions[dayIndex].background = selectedBackground
                repeatingDaysCount++
                toggleEnabledSaveButton()
            }
        }

        if(repeatingDays.contains(Calendar.SUNDAY)) toggleOption(0)
        if(repeatingDays.contains(Calendar.MONDAY)) toggleOption(1)
        if(repeatingDays.contains(Calendar.TUESDAY)) toggleOption(2)
        if(repeatingDays.contains(Calendar.WEDNESDAY)) toggleOption(3)
        if(repeatingDays.contains(Calendar.THURSDAY)) toggleOption(4)
        if(repeatingDays.contains(Calendar.FRIDAY)) toggleOption(5)
        if(repeatingDays.contains(Calendar.SATURDAY)) toggleOption(6)

        for((i, option )in dayOptions.withIndex())
            option.setOnClickListener { toggleOption(i) }

        var repeatingDaysSaved = false

        doneOption.setOnClickListener {
            repeatingDaysSaved = true
            chooseRepeatingDaysDialog.cancel()
        }

        chooseRepeatingDaysDialog.setOnCancelListener {
            chooseRepeatingDaysDialog.dismiss()
            if(repeatingDaysSaved) setRepeatingDays(daysSelected)
        }

        chooseRepeatingDaysDialog.show()
    }

    private fun setRepeatingDays(daysSelected: ArrayList<Boolean>) {
        val repeatingDays = ArrayList<Int>()
        if(daysSelected[0]) repeatingDays.add(Calendar.SUNDAY)
        if(daysSelected[1]) repeatingDays.add(Calendar.MONDAY)
        if(daysSelected[2]) repeatingDays.add(Calendar.TUESDAY)
        if(daysSelected[3]) repeatingDays.add(Calendar.WEDNESDAY)
        if(daysSelected[4]) repeatingDays.add(Calendar.THURSDAY)
        if(daysSelected[5]) repeatingDays.add(Calendar.FRIDAY)
        if(daysSelected[6]) repeatingDays.add(Calendar.SATURDAY)
        activity.getGoal().setRepeatingDays(repeatingDays)
        activity.getView().updateDateButton()
    }

    fun manageLabelsDialogClosed(currentLabelsString: String) {
        activity.setOtherProcessStarted(false)
        val newLabelsString = activity.getGoal().getLabels().toString()
        if(newLabelsString != currentLabelsString) {
            updateGoalLabels(activity.getGoal(), activity.getDatabase())
            activity.getView().updateLabels()
        }
    }
}