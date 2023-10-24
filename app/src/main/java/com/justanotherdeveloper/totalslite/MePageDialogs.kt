package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*
import kotlin.collections.ArrayList

class MePageDialogs(private val activity: HomePageActivity,
                    private val fragment: MePageFragment) {

    fun openGoalSettingsDialog(goalView: View, goal: Goal,
                               dueDate: Calendar, postButtonIsVisible: Boolean) {
        if(fragment.otherProcessStarted()) return
        val goalSettingsDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_goal_settings, null)
        goalSettingsDialog.setContentView(view)

        val markAsIncompleteText = view.findViewById<TextView>(R.id.markAsIncompleteText)
        val markAsNotRequiredText = view.findViewById<TextView>(R.id.markAsNotRequiredText)
        val markAsNotRequiredOption = view.findViewById<LinearLayout>(R.id.markAsNotRequiredOption)

        if(goal.isRepeating()) {
            markAsIncompleteText.text = activity.getString(
                R.string.markAsIncompleteForDateOption,
                activity.getDateText(dueDate).lowercase(Locale.ROOT))
            markAsNotRequiredText.text = activity.getString(
                R.string.markAsNotRequiredForDateOption,
                activity.getDateText(dueDate).lowercase(Locale.ROOT))
        } else {
            markAsIncompleteText.text = activity.getString(
                R.string.markAsIncompleteOption)
            markAsNotRequiredOption.visibility = View.GONE
        }

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(view.findViewById(R.id.editGoalOption))
        dialogOptions.add(view.findViewById(R.id.deleteGoalOption))
        dialogOptions.add(view.findViewById(R.id.postGoalProgressOption))
        dialogOptions.add(view.findViewById(R.id.markAsIncompleteOption))
        dialogOptions.add(markAsNotRequiredOption)
        initDialogOptions(goalSettingsDialog, dialogOptions)

        if(!postButtonIsVisible) {
            dialogOptions[2].visibility = View.GONE
            dialogOptions[3].visibility = View.GONE
            dialogOptions[4].visibility = View.GONE
        }

        fun clickOption(option: LinearLayout) {
            fragment.setOtherProcessStarted(false)
            when(option) {
                dialogOptions[0] -> fragment.getFragmentView().editGoalViewPressed(goalView)
                dialogOptions[1] -> showConfirmDeleteGoalDialog(goalView)
                dialogOptions[2] -> fragment.getFragmentView().postGoalProgressPressed(goalView)
                dialogOptions[3] -> showConfirmMarkAsIncompleteDialog(goalView, goal, dueDate)
                dialogOptions[4] -> showConfirmMarkAsIncompleteDialog(
                    goalView, goal, dueDate, true)
            }
            goalSettingsDialog.cancel()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        goalSettingsDialog.setOnCancelListener {
            fragment.setOtherProcessStarted(false)
            goalSettingsDialog.dismiss()
        }

        goalSettingsDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showConfirmMarkAsIncompleteDialog(goalView: View, goal: Goal, dueDate: Calendar,
                                                  markAsNotRequired: Boolean = false) {
        fragment.createIncompletePost(goal, markAsNotRequired)
        fragment.getIncompletePost()?.setDueDate(dueDate)
        val incompletePost = fragment.getIncompletePost()?: return

        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_post_incomplete, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmMarkAsIncompleteDialog = builder.create()
        confirmMarkAsIncompleteDialog.setCancelable(true)

        confirmMarkAsIncompleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        if(markAsNotRequired) {
            val promptText = view.findViewById<TextView>(R.id.promptText)
            promptText.text = activity.getString(R.string.markAsNotRequiredPrompt)

            val deleteText = view.findViewById<TextView>(R.id.deleteText)
            deleteText.text = activity.getString(R.string.markAsNotRequiredOption)
        }

        val goalTitleText = view.findViewById<TextView>(R.id.goalTitleText)
        val incompleteForDateText = view.findViewById<TextView>(R.id.incompleteForDateText)

        val amount = incompletePost.getAmount()
        val titleString = if(amount > 0)
            "$amount ${incompletePost.getTitle()}"
        else incompletePost.getTitle()

        val hour = incompletePost.getHour()
        val minute = incompletePost.getMinute()
        goalTitleText.text = activity.getString(R.string.titleAndTimeDueString,
            titleString, getTimeText(hour, minute))

        if(markAsNotRequired)
            incompleteForDateText.text = activity.getString(
                R.string.notRequiredDetailsString, dueDate.toDateString())
        else incompleteForDateText.text = activity.getString(
            R.string.incompleteDetailsString, dueDate.toDateString())

        if(markAsNotRequired) {
            val incompleteIcon = view.findViewById<ImageView>(R.id.incompleteIcon)
            incompleteIcon.setImageResource(R.drawable.ic_cancelled)
        }

        val moreButton = view.findViewById<ImageView>(R.id.moreButton)
        moreButton.setOnClickListener {
            if(!fragment.postingInProgress())
                showIncompletePostDialogMoreOptionsDialog()
        }

        val addACaptionLayout = view.findViewById<LinearLayout>(R.id.addACaptionLayout)
        val addACaptionOption = view.findViewById<TextView>(R.id.addACaptionOption)
        val captionText = view.findViewById<TextView>(R.id.captionText)
        addACaptionLayout.setOnClickListener {
            if(!fragment.postingInProgress())
                openAddIncompletePostCaptionDialog(activity,
                    addACaptionOption, captionText, goalPage = fragment)
        }

        val markIncompleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        val disabledButton = view.findViewById<LinearLayout>(R.id.disabledButton)
        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        initButtonAnimationListener(markIncompleteButton)

        fun markIncompletePressed() {
            if(!activity.withinRequestsLimit()) return
            disabledButton.visibility = View.VISIBLE
            markIncompleteButton.visibility = View.GONE
            cancelButton.visibility = View.INVISIBLE
            confirmMarkAsIncompleteDialog.setCancelable(false)
            fragment.postIncompleteGoal(confirmMarkAsIncompleteDialog,
                disabledButton, markIncompleteButton, cancelButton,
                goalView, goal)
        }

        markIncompleteButton.setOnClickListener {
            markIncompletePressed()
        }

        cancelButton.setOnClickListener {
            confirmMarkAsIncompleteDialog.dismiss()
        }

        confirmMarkAsIncompleteDialog.setOnCancelListener {
            fragment.clearIncompletePost()
        }

        confirmMarkAsIncompleteDialog.show()
    }

    private fun showIncompletePostDialogMoreOptionsDialog() {
        val postMoreOptionsDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_post_settings, null)
        postMoreOptionsDialog.setContentView(view)

        val seePostLikesOption = view.findViewById<LinearLayout>(R.id.seePostLikesOption)
        val seeAttachmentsOption = view.findViewById<LinearLayout>(R.id.seeAttachmentsOption)
        val manageLabelsOption = view.findViewById<LinearLayout>(R.id.manageLabelsOption)
        val repostAsIncompleteOption = view.findViewById<LinearLayout>(R.id.repostAsIncompleteOption)
        val repostAsNotRequiredOption = view.findViewById<LinearLayout>(R.id.repostAsNotRequiredOption)
        val reportOption = view.findViewById<LinearLayout>(R.id.reportOption)
        val seePhotoOption = view.findViewById<LinearLayout>(R.id.seePhotoOption)
        val startGoalOption = view.findViewById<LinearLayout>(R.id.startGoalOption)

        val labelsInfoText = view.findViewById<TextView>(R.id.labelsInfoText)
        val labels = fragment.getIncompletePost()?.getLabels()
        if(labels != null) {
            var labelsString = ""
            for(label in labels.reversed()) {
                if(labelsString.isNotEmpty())
                    labelsString += ", "
                labelsString += label
            }
            labelsInfoText.text = activity.getString(R.string.includedLabelsInfo, labelsString)
        } else labelsInfoText.visibility = View.GONE

        startGoalOption.visibility = View.GONE
        seePostLikesOption.visibility = View.GONE
        repostAsIncompleteOption.visibility = View.GONE
        repostAsNotRequiredOption.visibility = View.GONE
        reportOption.visibility = View.GONE
        seePhotoOption.visibility = View.GONE

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(seeAttachmentsOption)
        dialogOptions.add(manageLabelsOption)
        initDialogOptions(postMoreOptionsDialog, dialogOptions)

        seeAttachmentsOption.setOnClickListener {
            fragment.manageAttachmentsForIncompletePost()
            postMoreOptionsDialog.dismiss()
        }

        manageLabelsOption.setOnClickListener {
            fragment.manageLabelsForIncompletePost()
            postMoreOptionsDialog.dismiss()
        }

        postMoreOptionsDialog.show()
    }

    private fun showConfirmDeleteGoalDialog(goalView: View) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmDeleteDialog = builder.create()
        confirmDeleteDialog.setCancelable(true)

        confirmDeleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val promptText = view.findViewById<TextView>(R.id.promptText)
        promptText.text = activity.getString(R.string.deleteGoalPrompt)

        val deleteText = view.findViewById<TextView>(R.id.deleteText)
        deleteText.text = activity.getString(R.string.option_delete)

        val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        initButtonAnimationListener(deleteButton)
        deleteButton.setOnClickListener {
            if(activity.withinRequestsLimit()) {
                fragment.deleteGoal(goalView)
                confirmDeleteDialog.dismiss()
            }
        }

        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmDeleteDialog.dismiss()
        }

        confirmDeleteDialog.show()
    }
}