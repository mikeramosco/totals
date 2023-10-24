package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.core.view.iterator

class EditActivityPageListeners(private val activity: EditActivityPageActivity) {

    private val binding = activity.getBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initButtonAnimationListenersInScrollView()
        initHorizontalScrollViewListener()
        initOnClickListeners()
        initTextFieldListeners()
    }

    fun getScrollViewAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.saveButton)
        initButtonAnimationListener(binding.nextButton)
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.editActivityPageScroll, binding.editActivityPageScrollContents)
        scrollViewButtons.addButton(binding.addLabelButton)
        scrollViewButtons.addButton(binding.setADateButton)
        scrollViewButtons.addButton(binding.setATimeButton)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initHorizontalScrollViewListener() {
        binding.goalTagsScrollView.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_MOVE -> {
                    releaseGoalTags()
                }
            }
            false
        }
    }

    private fun releaseGoalTags() {
        for(view in binding.labelsContainer.iterator()) {
            val labelButton = view.findViewById<LinearLayout>(R.id.labelButton)
            if(labelButton != null)
                scrollViewButtons.release(labelButton)
            else scrollViewButtons.release(view)
        }
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            activity.saveButtonClicked()
        }

        binding.addLabelButton.setOnClickListener {
            if(!activity.otherProcessStarted())
                activity.showManageLabelsDialog(activity.getGoal(), true,
                    editGoalPage = activity)
        }

        binding.nextButton.setOnClickListener {
            if(!activity.getGoal().hasDate() && !activity.getGoal().isRepeating())
                activity.getDialogs().showChooseCalendarMethodDialog()
            else activity.getDialogs().openTimeDialog()
        }

        binding.setADateButton.setOnClickListener {
            activity.getDialogs().showChooseCalendarMethodDialog()
        }

        binding.setATimeButton.setOnClickListener {
            activity.getDialogs().openTimeDialog()
        }

        binding.includeGoalAmountLayout.setOnClickListener {
            activity.getView().toggleGoalAmountVisibility()
        }

        binding.addNoteButton.setOnClickListener {
            if(!activity.otherProcessStarted()) {
                activity.openNoteDialog(activity.getGoal(),
                    editGoalPageDialogs = activity.getDialogs())
                activity.getView().clearFocus()
            }
        }

        binding.addLinkButton.setOnClickListener {
            if(!activity.otherProcessStarted()) {
                activity.openAddLinkDialog(editGoalPageDialogs = activity.getDialogs())
                activity.getView().clearFocus()
            }
        }

        binding.goalActiveSwitch.setOnCheckedChangeListener { _, isChecked ->
            if(activity.pageSetupFinished()) {
                activity.getGoal().setActive(isChecked)
                activity.getView().setActiveText(isChecked)
            }
        }
    }

    private fun initTextFieldListeners() {
        binding.goalAmountField.setOnKeyListener { _, keyCode, event ->
            if(event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.goalAmountField.post {
                    moveCursorTo(binding.unitsField)
                }
            }
            false
        }

        binding.unitsField.setOnKeyListener { _, keyCode, event ->
            if(binding.unitsField.text.toString().isEmpty() && event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                moveCursorTo(binding.goalAmountField)
            }
            false
        }

        binding.unitsField.setOnEditorActionListener { _, actionId, _ ->
            val donePressed = actionId == EditorInfo.IME_ACTION_DONE
            if(donePressed) activity.getView().clearFocus()
            false
        }

        binding.goalTitleField.setOnEditorActionListener { _, actionId, _ ->
            val donePressed = actionId == EditorInfo.IME_ACTION_DONE
            if(donePressed) activity.getView().clearFocus()
            false
        }
    }

}