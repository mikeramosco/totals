package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.justanotherdeveloper.totalslite.databinding.ActivityEditActivityPageBinding


class EditActivityPageActivity : AppCompatActivity(),
    DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivityEditActivityPageBinding

    /** 'view' holds all view methods for this activity */
    private lateinit var view: EditActivityPageView

    /** 'listeners' holds all listener methods for this activity */
    private lateinit var listeners: EditActivityPageListeners

    /** 'dialogs' holds all dialog methods for this activity */
    private lateinit var dialogs: EditActivityPageDialogs

    private lateinit var local: TinyDB

    private lateinit var originalGoal: Goal
    private lateinit var goal: Goal

    private lateinit var uploader: GoalUploader

    private var pageSetupFinished = false

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)

        if(forNewGoalLikePost()) {
            goal = Goal()

            val postToCopy = getStaticProgress()
            if(postToCopy != null) {
                setStaticProgress(null)
                goal.addLabel(postToCopy.getLabels()[0])
                goal.setUserId(getSignedInUserId(local))
                updateGoalLabels(goal, local)

                val originalAmount = postToCopy.getOriginalAmount()
                val amountToSet = if(originalAmount != 0)
                    originalAmount else postToCopy.getAmount()

                goal.setAmount(amountToSet)
                goal.setTitle(postToCopy.getTitle())
                for(note in postToCopy.getNotes())
                    goal.getNotes().add(note)
                for(link in postToCopy.getLinks())
                    goal.getLinks().add(link)

                goal.setTime(postToCopy.getHour(), postToCopy.getMinute())
                goal.setRepeatingDays(postToCopy.getRepeatingDays())
                val todaysDate = getTodaysDate().resetTimeOfDay()
                val copyDate = postToCopy.getDate()?.resetTimeOfDay()
                if(copyDate != null && todaysDate.comesBefore(copyDate, true))
                    goal.setDate(copyDate)
            }
        } else if(forNewGoal()) {
            goal = Goal()
            goal.setUserId(getSignedInUserId(local))

            var recentLabel = local.getString(RECENT_GOAL_LABEL_REF)
            if(recentLabel.isEmpty()) {
                recentLabel = getString(R.string.myGoalsProfileTitle)
                goal.addLabel(recentLabel)
                updateGoalLabels(goal, local)
            } else goal.addLabel(recentLabel)
        } else {
            originalGoal = getStaticGoal()?: Goal()
            goal = originalGoal.createClone()
            setStaticGoal(null)
        }
        uploader = GoalUploader(editGoalPage = this)

        listeners = EditActivityPageListeners(this)
        view = EditActivityPageView(this)
        dialogs = EditActivityPageDialogs(this)
        pageSetupFinished = true
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityEditActivityPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun saveButtonClicked() {
        if(!withinRequestsLimit()) return
        val goalAmountString = binding.goalAmountField.text.toString().removeFrontZeros()
        val goalAmount = if(goalAmountString.isNotEmpty()) goalAmountString.toInt() else 0

        if(binding.goalTitleField.text.isNotEmpty())
            binding.goalTitleField.formatEntry()
        else if(binding.unitsField.text.isNotEmpty())
            binding.unitsField.formatEntry()

        var goalText = binding.goalTitleField.text.toString()
        if(goalText.isEmpty()) goalText = binding.unitsField.text.toString()

        if(goalText.isEmpty()) {
            showToast(getString(R.string.goalTitleRequiredMessage))
            return
        } else if(!forNewGoal() && binding.goalAmountField.isVisible) {
            val amountCompleted = getAmountCompleted()
            if(goalAmount <= amountCompleted) {
                showToast(getString(R.string.goalAmountMoreThanCompletedMessage,
                    amountCompleted.toString()))
                return
            }
        }

        view.disableNavButtons()
        goal.setAmount(goalAmount)
        goal.setTitle(goalText)

        if(forNewGoal())
            uploader.addGoal()
        else uploader.checkForGoalUpdate()
    }

    fun confirmGoalUpdatedSuccess(goalUpdated: Boolean = true) {
        setStaticGoal(goal)
        if(goalUpdated) {
            showToast(getString(R.string.goalUpdatedMessage))
            val intentData = Intent()
            intentData.putExtra(GOAL_UPDATED_REF, true)
            setResult(Activity.RESULT_OK, intentData)
        }
        finish()
    }

    fun confirmGoalAddedSuccess() {
        setStaticGoal(goal)
        showToast(getString(R.string.goalAddedMessage))
        val intentData = Intent()
        intentData.putExtra(SELECTED_LABEL_REF, goal.getLabels().last())
        intentData.putExtra(GOAL_ADDED_REF, true)
        setResult(Activity.RESULT_OK, intentData)
        finish()
    }

    fun deleteNote(noteIndex: Int, noteView: View) {
        goal.getNotes().removeAt(noteIndex)
        view.removeNoteView(noteView)
        view.updateAddNoteButton()
    }

    fun deleteLink(linkIndex: Int, linkView: View) {
        goal.getLinks().removeAt(linkIndex)
        view.removeLinkView(linkView)
        view.updateAddLinkButton()
    }

    fun setAsRecentLabel(labelTextString: String) {
        goal.getLabels().remove(labelTextString)
        goal.getLabels().add(labelTextString)
    }

    fun manageLabel(toAdd: Boolean, labelTextString: String) {
        if(toAdd) goal.addLabel(labelTextString)
        else goal.getLabels().remove(labelTextString)
    }

    /** Override Methods */

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    override fun onBackPressed() {
        if(otherProcessStarted()) return
        if(uploader.postingInProgress()) goToHomeScreen()
        else if((forNewGoal() && goalInfoAdded())
            || (!forNewGoal() && (goalTitleChanged() || goal.changedFrom(originalGoal))))
            dialogs.showConfirmClosePageDialog() else finish()
    }

    private fun goalTitleChanged(): Boolean {
        val amountString = binding.goalAmountField.text.toString().removeFrontZeros()
        val amount = if(amountString.isNotEmpty()) amountString.toInt() else 0
        val units = binding.unitsField.text.toString()
        val title = binding.goalTitleField.text.toString()
        val numberShown = binding.goalAmountField.isVisible
                || binding.goalAmountText.isVisible
        if(originalGoal.getAmount() != amount) return true
        return if(numberShown)
            originalGoal.getTitle() != units
        else originalGoal.getTitle() != title
    }

    private fun goalInfoAdded(): Boolean {
        val amount = binding.goalAmountField.text.toString().removeFrontZeros()
        val units = binding.unitsField.text.toString()
        val title = binding.goalTitleField.text.toString()
        return goal.infoAdded() || amount.isNotEmpty()
                || units.isNotEmpty() || title.isNotEmpty()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, day: Int) {
        goal.setDate(createCalendar(year, month+1, day))
        this.view.updateDateButton()
    }

    override fun onTimeSet(view: TimePicker?, hour: Int, minute: Int) {
        saveTimeDataString(local, getTimeDataString(hour, minute))
        goal.setTime(hour, minute)
        this.view.updateTimeButton()
    }

    /** "Get" Methods */

    fun getBinding(): ActivityEditActivityPageBinding {
        return binding
    }

    fun getListeners(): EditActivityPageListeners {
        return listeners
    }

    fun getView(): EditActivityPageView {
        return view
    }

    fun getDialogs(): EditActivityPageDialogs {
        return dialogs
    }

    fun getGoal(): Goal {
        return goal
    }

    fun getDatabase(): TinyDB {
        return local
    }

    fun getOriginalGoal(): Goal {
        return originalGoal
    }

    fun forNewGoal(): Boolean {
        return intent.getBooleanExtra(NEW_GOAL_REF, false) || forNewGoalLikePost()
    }

    fun forNewGoalLikePost(): Boolean {
        return intent.getBooleanExtra(START_GOAL_LIKE_POST_REF, false)
    }

    fun getAmountCompleted(): Int {
        return intent.getIntExtra(AMOUNT_COMPLETED_REF, 0)
    }

    fun goalCompleted(): Boolean {
        return intent.getBooleanExtra(GOAL_COMPLETED_REF, false)
    }

    fun pageSetupFinished(): Boolean {
        return pageSetupFinished
    }

    fun otherProcessStarted(setTrue: Boolean = true): Boolean {
        return if(otherProcessStarted) true else {
            if(setTrue) setOtherProcessStarted()
            false
        }
    }

    fun setOtherProcessStarted(otherProcessStarted: Boolean = true) {
        this.otherProcessStarted = otherProcessStarted
    }
}