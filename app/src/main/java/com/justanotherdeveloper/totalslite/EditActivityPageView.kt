package com.justanotherdeveloper.totalslite

import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import androidx.core.view.size

class EditActivityPageView(private val activity: EditActivityPageActivity) {

    private val binding = activity.getBinding()

    init {
        initDisplay()
        updateLabels(false)
    }

    private fun initDisplay() {
        if(activity.forNewGoal()) {
            binding.editActivityTitle.text = activity.getString(R.string.createNewActivityTitle)
            binding.postButton.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
            binding.nextButton.visibility = View.VISIBLE
            if(!activity.forNewGoalLikePost())
                binding.editActivityPageParent.post {
                    moveCursorTo(binding.goalTitleField)
                }
        } else {
            val goal = activity.getGoal()
            updatePermanentDateAndTimeLayouts()
            displayGoalActiveSwitchViews()
            val goalHasAmount = goal.getAmount() > 0
            if(goalHasAmount) {
                if(activity.goalCompleted()) {
                    binding.goalTitleField.visibility = View.GONE
                    binding.goalAmountText.visibility = View.VISIBLE
                    binding.unitsField.visibility = View.VISIBLE
                    binding.cantChangeAmountMessage.visibility = View.VISIBLE
                } else toggleGoalAmountVisibility(false)
            }
            binding.includeGoalAmountLayout.visibility = View.GONE
            if(goalHasAmount) {
                val goalAmount = goal.getAmount().toString()
                binding.goalAmountText.text = goalAmount
                binding.goalAmountField.setText(goalAmount)
                binding.unitsField.setText(goal.getTitle())
            } else binding.goalTitleField.setText(goal.getTitle())
            for(note in goal.getNotes()) addNoteView(note, false)
            for(link in goal.getLinks()) addLinkView(link, false)
        }

        if(activity.forNewGoalLikePost()) {
            val goal = activity.getGoal()
            if(goal.getAmount() > 0)
                toggleGoalAmountVisibility(false)
            if(binding.goalAmountField.isVisible) {
                binding.goalAmountField.setText(goal.getAmount().toString())
                binding.unitsField.setText(goal.getTitle())
            } else binding.goalTitleField.setText(goal.getTitle())
            for(note in goal.getNotes()) addNoteView(note, false)
            for(link in goal.getLinks()) addLinkView(link, false)
            if(goal.hasDate() || goal.isRepeating())
                updateDateButton(false)
            updateTimeButton(false)
        }

        updateAddNoteButton()
        updateAddLinkButton()
    }

    fun updateAddNoteButton() {
        binding.addNoteButton.visibility =
            if(activity.getGoal().getNotes().size < MAX_NOTES_AND_LINKS)
                View.VISIBLE else View.GONE
    }

    fun updateAddLinkButton() {
        binding.addLinkButton.visibility =
            if(activity.getGoal().getLinks().size < MAX_NOTES_AND_LINKS)
                View.VISIBLE else View.GONE
    }

    fun disableNavButtons() {
        binding.backButtonDisabled.visibility = View.VISIBLE
        binding.backButton.visibility = View.GONE
        binding.progressCircle.visibility = View.VISIBLE
        binding.saveButton.visibility = View.GONE
    }

    fun notifyPostFailure() {
        binding.backButtonDisabled.visibility = View.GONE
        binding.backButton.visibility = View.VISIBLE
        binding.progressCircle.visibility = View.GONE
        binding.saveButton.visibility = View.VISIBLE
        activity.showRequestUnavailableToast()
    }

    fun clearFocus() {
        binding.goalAmountField.clearFocus()
        binding.goalTitleField.clearFocus()
        binding.unitsField.clearFocus()
    }

    fun setActiveText(isActive: Boolean) {
        if(isActive) {
            binding.activeText.visibility = View.VISIBLE
            binding.inactiveText.visibility = View.GONE
        } else {
            binding.activeText.visibility = View.GONE
            binding.inactiveText.visibility = View.VISIBLE
        }
    }

    fun addLinkView(linkTextString: String, animate: Boolean = true) {
        if(animate) beginTransition(binding.editActivityPageParent)
        val linkView = createLinkView(linkTextString, activity, editGoalPageView = this)
        binding.linksContainer.addView(linkView)
    }

    fun linkDeleteButtonPressed(linkView: View) {
        val linkIndex = binding.linksContainer.indexOfChild(linkView)
        activity.showDeleteLinkDialog(linkIndex, linkView, editGoalPage = activity)
    }

    fun noteButtonPressed(noteView: View) {
        val noteIndex = binding.notesContainer.indexOfChild(noteView)
        activity.openNoteDialog(activity.getGoal(), noteIndex, noteView,
            editGoalPageDialogs = activity.getDialogs())
        clearFocus()
    }

    fun noteDeleteButtonPressed(noteView: View) {
        val noteIndex = binding.notesContainer.indexOfChild(noteView)
        activity.showDeleteNoteDialog(noteIndex, noteView, editGoalPage = activity)
    }

    fun addNoteView(noteTextString: String, animate: Boolean = true) {
        if(animate) beginTransition(binding.editActivityPageParent)
        val hideDivider = binding.notesContainer.isNotEmpty()
        val noteView = createNoteView(noteTextString, hideDivider,
            activity, editGoalPageView = this)
        binding.notesContainer.addView(noteView)
    }

    fun removeNoteView(noteView: View) {
        beginTransition(binding.editActivityPageParent)
        binding.notesContainer.removeView(noteView)
        if(binding.notesContainer.isNotEmpty()) {
            val topNoteView = binding.notesContainer[0]
            val divider = topNoteView.findViewById<LinearLayout>(R.id.itemDividerTop)
            divider.visibility = View.VISIBLE
        }
    }

    fun removeLinkView(linkView: View) {
        beginTransition(binding.editActivityPageParent)
        binding.linksContainer.removeView(linkView)
    }

    private fun displayGoalActiveSwitchViews() {
//        binding.editActivityTitleSpacer.visibility = View.VISIBLE
        binding.goalActiveSwitch.visibility = View.VISIBLE
        binding.goalActiveSwitch.isChecked = activity.getGoal().isActive()
        setActiveText(activity.getGoal().isActive())
    }

    private fun updateNextStepButton() {
        val goal = activity.getGoal()
        if((goal.hasDate() || goal.isRepeating()) && goal.hasTime()) {
            binding.nextButton.visibility = View.GONE
            binding.saveButton.visibility = View.VISIBLE
        }
    }

    private fun updatePermanentDateAndTimeLayouts() {
        val goal = activity.getGoal()

        val goalDate = goal.getDate()
        val dateText = if(goalDate != null)
            activity.getDateText(goalDate)
        else goal.getRepeatingDaysString(activity)

        if(goal.isRepeating())
            binding.permanentDateIcon.setImageResource(R.drawable.ic_repeating)
        binding.permanentDateText.text = dateText
        binding.permanentTimeText.text = getTimeText(goal.getHour(), goal.getMinute())

        binding.permanentDateLayout.visibility = View.VISIBLE
        binding.permanentTimeLayout.visibility = View.VISIBLE
        binding.setADateButton.visibility = View.GONE
        binding.setATimeButton.visibility = View.GONE
    }

    fun updateTimeButton(showToast: Boolean = true) {
        val goal = activity.getGoal()
        binding.timeIcon.setImageResource(R.drawable.ic_time_theme)
        binding.setATimeText.text = getTimeText(goal.getHour(), goal.getMinute())
        binding.setATimeText.setTextColor(activity.getValuesColor(R.color.color_theme))
        updateNextStepButton()
        if(showToast) activity.showToast(activity.getString(R.string.dateAndTimePermanentMessage))
    }

    fun updateDateButton(showToast: Boolean = true) {
        val goal = activity.getGoal()

        val goalDate = goal.getDate()
        val dateText = if(goalDate != null)
            activity.getDateText(goalDate)
        else goal.getRepeatingDaysString(activity)
        val dateIconCode = if(goal.isRepeating())
            R.drawable.ic_repeating
        else R.drawable.ic_date_theme

        binding.dateIcon.setImageResource(dateIconCode)
        binding.setADateText.text = dateText
        binding.setADateText.setTextColor(activity.getValuesColor(R.color.color_theme))
        updateNextStepButton()
        if(showToast) activity.showToast(activity.getString(R.string.dateAndTimePermanentMessage))
    }

    fun updateLabels(animate: Boolean = true) {
        if(animate) beginTransition(binding.editActivityPageParent)
        while(binding.labelsContainer.size > 1)
            binding.labelsContainer.removeViewAt(0)

        val goalLabels = activity.getGoal().getLabels()
        for(label in goalLabels) addLabelView(label)

        binding.goalTagsScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT)
    }

    private fun addLabelView(label: String) {
        val labelView = activity.layoutInflater.inflate(R.layout.view_label_added, null)
        val labelButton = labelView.findViewById<LinearLayout>(R.id.labelButton)
        val labelText = labelView.findViewById<TextView>(R.id.labelText)
        val removeLabelButton = labelView.findViewById<ImageView>(R.id.removeLabelButton)

        labelText.text = label

        labelButton.setOnClickListener {
            activity.showManageLabelsDialog(activity.getGoal(), editGoalPage = activity)
        }

        removeLabelButton.setOnClickListener {
            activity.showRemoveLabelDialog(activity.getGoal(), label, labelView,
                editGoalPageView = this)
        }

        activity.getListeners().getScrollViewAnimation().addButton(labelButton)

        binding.labelsContainer.addView(labelView, 0)
    }

    fun removeLabel(label: String, labelView: View) {
        beginTransition(binding.editActivityPageParent)
        binding.labelsContainer.removeView(labelView)
        activity.getGoal().getLabels().remove(label)
        updateGoalLabels(activity.getGoal(), activity.getDatabase())
    }

    fun toggleGoalAmountVisibility(requestFocus: Boolean = true) {
        if(binding.goalAmountField.isVisible) {
            binding.includeGoalAmountCheckbox.setImageResource(
                R.drawable.ic_check_box_outline_blank_gray)

            binding.goalTitleField.setText(
                binding.unitsField.text.toString())
            binding.goalAmountField.setText("")
            binding.goalAmountField.visibility = View.GONE
            binding.unitsField.setText("")
            binding.unitsField.visibility = View.GONE
            binding.goalTitleField.visibility = View.VISIBLE
            if(requestFocus) moveCursorTo(binding.goalTitleField)
        } else {
            binding.includeGoalAmountCheckbox.setImageResource(
                R.drawable.ic_check_box_checked)
            binding.unitsField.setText(
                binding.goalTitleField.text.toString())
            binding.goalTitleField.setText("")
            binding.goalTitleField.visibility = View.GONE
            binding.goalAmountField.visibility = View.VISIBLE
            binding.unitsField.visibility = View.VISIBLE
            if(requestFocus) moveCursorTo(binding.goalAmountField)
        }
    }

}