package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*
import kotlin.collections.ArrayList

class ProgressPageDialogs(private val activity: ProgressPageActivity) {

    fun showProfileOptionsDialog() {
        if(activity.otherProcessStarted()) return
        val profileOptionsDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_progress_page_settings, null)
        profileOptionsDialog.setContentView(view)

        val sendProfileOption = view.findViewById<LinearLayout>(R.id.sendProfileOption)
        val manageFollowersOption = view.findViewById<LinearLayout>(R.id.manageFollowersOption)
        val setTotalsTodayOption = view.findViewById<LinearLayout>(R.id.setTotalsTodayOption)
        val setTotalsAllPostsOption = view.findViewById<LinearLayout>(R.id.setTotalsAllPostsOption)
        val setTotalsAllPostsInfo = view.findViewById<TextView>(R.id.setTotalsAllPostsInfo)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(sendProfileOption)
        dialogOptions.add(manageFollowersOption)
        dialogOptions.add(setTotalsTodayOption)
        dialogOptions.add(setTotalsAllPostsOption)
        initDialogOptions(profileOptionsDialog, dialogOptions)

        val profile = activity.getProfile()
        if(profile.getFollowers().isEmpty())
            manageFollowersOption.visibility = View.GONE

        val totalsStartDate = profile.getTotalsStartDate()
        if(totalsStartDate != null) {
            setTotalsAllPostsOption.visibility = View.VISIBLE
            setTotalsAllPostsInfo.text = activity.getString(R.string.showingTotalsForDateInfo,
                activity.getDateText(totalsStartDate).lowercase(Locale.getDefault()))
            if(datesAreTheSame(getTodaysDate(), totalsStartDate))
                setTotalsTodayOption.visibility = View.GONE
        }

        fun clickOption(option: LinearLayout) {
            activity.setOtherProcessStarted(false)
            when(option) {
                dialogOptions[0] -> activity.searchUsersToSendProfile()
                dialogOptions[1] -> activity.openManageFollowersPage()
                dialogOptions[2] -> showConfirmSetTotalsDialog(true)
                dialogOptions[3] -> showConfirmSetTotalsDialog()
            }

            profileOptionsDialog.cancel()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        profileOptionsDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            profileOptionsDialog.dismiss()
        }

        profileOptionsDialog.show()
    }

    private fun showConfirmSetTotalsDialog(resetForPostsAfterToday: Boolean = false) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_reset_totals, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmSetTotalsDialog = builder.create()
        confirmSetTotalsDialog.setCancelable(true)

        confirmSetTotalsDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        if(resetForPostsAfterToday) {
            val promptText = view.findViewById<TextView>(R.id.promptText)
            val detailsText = view.findViewById<TextView>(R.id.detailsText)
            promptText.text = activity.getString(R.string.resetTotalsTodayDialogPrompt)
            detailsText.text = activity.getString(R.string.resetTotalsTodayDialogDetails)
        }

        val resetTotalsButton = view.findViewById<LinearLayout>(R.id.resetTotalsButton)
        val disabledButton = view.findViewById<LinearLayout>(R.id.disabledButton)
        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        initButtonAnimationListener(resetTotalsButton)

        val totalsStartDate = if(resetForPostsAfterToday)
            getTodaysDate().resetTimeOfDay() else null

        fun resetTotalsPressed() {
            disabledButton.visibility = View.VISIBLE
            resetTotalsButton.visibility = View.GONE
            cancelButton.visibility = View.INVISIBLE
            confirmSetTotalsDialog.setCancelable(false)
            activity.getProfiles().resetTotals(totalsStartDate,
                disabledButton, resetTotalsButton,
                cancelButton, confirmSetTotalsDialog)
        }

        resetTotalsButton.setOnClickListener {
            resetTotalsPressed()
        }

        cancelButton.setOnClickListener {
            confirmSetTotalsDialog.dismiss()
        }

        confirmSetTotalsDialog.show()
    }

    fun showIncompletePostMoreOptionsDialog(post: GoalProgress, postView: View) {
        if(activity.otherProcessStarted()) return
        val postMoreOptionsDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_post_settings, null)
        postMoreOptionsDialog.setContentView(view)

        val startGoalOption = view.findViewById<LinearLayout>(R.id.startGoalOption)
        val seePostLikesOption = view.findViewById<LinearLayout>(R.id.seePostLikesOption)
        val seeAttachmentsOption = view.findViewById<LinearLayout>(R.id.seeAttachmentsOption)
        val manageLabelsOption = view.findViewById<LinearLayout>(R.id.manageLabelsOption)
        val repostAsIncompleteOption = view.findViewById<LinearLayout>(R.id.repostAsIncompleteOption)
        val repostAsNotRequiredOption = view.findViewById<LinearLayout>(R.id.repostAsNotRequiredOption)
        val reportOption = view.findViewById<LinearLayout>(R.id.reportOption)
        val seePhotoOption = view.findViewById<LinearLayout>(R.id.seePhotoOption)
        val deletePostOption = view.findViewById<LinearLayout>(R.id.deletePostOption)

        seePostLikesOption.visibility = View.GONE
        seePhotoOption.visibility = View.GONE

        if(!activity.userIsProfileOwner()) {
            manageLabelsOption.visibility = View.GONE
            repostAsIncompleteOption.visibility = View.GONE
            repostAsNotRequiredOption.visibility = View.GONE
            deletePostOption.visibility = View.GONE
        } else reportOption.visibility = View.GONE

        if(post.isNotRequired()) {
            repostAsNotRequiredOption.visibility = View.GONE
            if(!post.isLastPostOfDueDate() || post.isFromDeletedReportedPost())
                repostAsIncompleteOption.visibility = View.GONE
        } else repostAsIncompleteOption.visibility = View.GONE

        val hasNotes = post.getNotes().isNotEmpty()
        val hasLinks = post.getLinks().isNotEmpty()
        if(!hasNotes && !hasLinks) {
            if(!activity.userIsProfileOwner())
                seeAttachmentsOption.visibility = View.GONE
        } else {
            val attachmentsInfoText = view.findViewById<TextView>(R.id.attachmentsInfoText)
            val infoTextCode =
                if(!hasLinks) R.string.notesAttachedInfo
                else if(!hasNotes) R.string.linksAttachedInfo
                else R.string.bothAttachedInfo
            attachmentsInfoText.text = activity.getString(infoTextCode)
        }

        val labelsInfoText = view.findViewById<TextView>(R.id.labelsInfoText)
        val labels = post.getLabels()
        var labelsString = ""
        for(label in labels.reversed()) {
            if(labelsString.isNotEmpty())
                labelsString += ", "
            labelsString += label
        }
        labelsInfoText.text = activity.getString(R.string.includedLabelsInfo, labelsString)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(startGoalOption)
        dialogOptions.add(seeAttachmentsOption)
        dialogOptions.add(manageLabelsOption)
        dialogOptions.add(repostAsIncompleteOption)
        dialogOptions.add(repostAsNotRequiredOption)
        dialogOptions.add(reportOption)
        dialogOptions.add(deletePostOption)
        initDialogOptions(postMoreOptionsDialog, dialogOptions)

        fun clickOption(option: LinearLayout) {
            activity.setOtherProcessStarted(false)
            when(option) {
                dialogOptions[0] -> activity.startGoalLikePost(post)
                dialogOptions[1] -> activity.seeAttachments(post, postView)
                dialogOptions[2] -> activity.manageLabels(post, postView)
                dialogOptions[3] -> showConfirmMarkAsIncompleteDialog(post, postView)
                dialogOptions[4] -> showConfirmMarkAsIncompleteDialog(
                    post, postView, true)
                dialogOptions[5] -> showReportPostDialog(post)
                dialogOptions[6] -> showConfirmDeletePostDialog(post, postView)
            }
            postMoreOptionsDialog.cancel()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        postMoreOptionsDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            postMoreOptionsDialog.dismiss()
        }

        postMoreOptionsDialog.show()
    }

    fun showPostMoreOptionsDialog(post: GoalProgress, postView: View) {
        if(activity.otherProcessStarted()) return
        val postMoreOptionsDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_post_settings, null)
        postMoreOptionsDialog.setContentView(view)

        val startGoalOption = view.findViewById<LinearLayout>(R.id.startGoalOption)
        val seePostLikesOption = view.findViewById<LinearLayout>(R.id.seePostLikesOption)
        val seeAttachmentsOption = view.findViewById<LinearLayout>(R.id.seeAttachmentsOption)
        val manageLabelsOption = view.findViewById<LinearLayout>(R.id.manageLabelsOption)
        val repostAsIncompleteOption = view.findViewById<LinearLayout>(R.id.repostAsIncompleteOption)
        val repostAsNotRequiredOption = view.findViewById<LinearLayout>(R.id.repostAsNotRequiredOption)
        val reportOption = view.findViewById<LinearLayout>(R.id.reportOption)
        val seePhotoOption = view.findViewById<LinearLayout>(R.id.seePhotoOption)
        val deletePostOption = view.findViewById<LinearLayout>(R.id.deletePostOption)

        if(!activity.userIsProfileOwner()) {
            manageLabelsOption.visibility = View.GONE
            repostAsIncompleteOption.visibility = View.GONE
            repostAsNotRequiredOption.visibility = View.GONE
            deletePostOption.visibility = View.GONE
        } else reportOption.visibility = View.GONE

        if(!post.isLastPostOfDueDate())
            repostAsIncompleteOption.visibility = View.GONE

        val postLikesSize = post.getPostLikes().size
        if(postLikesSize == 0) seePostLikesOption.visibility = View.GONE
        else {
            val postLikeCountText = view.findViewById<TextView>(R.id.postLikeCountText)
            val countTextString = if(postLikesSize == 1)
                activity.getString(R.string.personLikedInfo)
            else activity.getString(R.string.peopleLikedInfo, postLikesSize.toString())
            postLikeCountText.text = countTextString
        }

        val hasNotes = post.getNotes().isNotEmpty()
        val hasLinks = post.getLinks().isNotEmpty()
        if(!hasNotes && !hasLinks) {
            if(!activity.userIsProfileOwner())
                seeAttachmentsOption.visibility = View.GONE
        } else {
            val attachmentsInfoText = view.findViewById<TextView>(R.id.attachmentsInfoText)
            val infoTextCode =
                if(!hasLinks) R.string.notesAttachedInfo
                else if(!hasNotes) R.string.linksAttachedInfo
                else R.string.bothAttachedInfo
            attachmentsInfoText.text = activity.getString(infoTextCode)
        }

        val labelsInfoText = view.findViewById<TextView>(R.id.labelsInfoText)
        val labels = post.getLabels()
        var labelsString = ""
        for(label in labels.reversed()) {
            if(labelsString.isNotEmpty())
                labelsString += ", "
            labelsString += label
        }
        labelsInfoText.text = activity.getString(R.string.includedLabelsInfo, labelsString)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(startGoalOption)
        dialogOptions.add(seePostLikesOption)
        dialogOptions.add(seeAttachmentsOption)
        dialogOptions.add(manageLabelsOption)
        dialogOptions.add(repostAsIncompleteOption)
        dialogOptions.add(repostAsNotRequiredOption)
        dialogOptions.add(reportOption)
        dialogOptions.add(seePhotoOption)
        dialogOptions.add(deletePostOption)
        initDialogOptions(postMoreOptionsDialog, dialogOptions)

        fun clickOption(option: LinearLayout) {
            activity.setOtherProcessStarted(false)
            when(option) {
                dialogOptions[0] -> activity.startGoalLikePost(post)
                dialogOptions[1] -> activity.seePostLikes(post)
                dialogOptions[2] -> activity.seeAttachments(post, postView)
                dialogOptions[3] -> activity.manageLabels(post, postView)
                dialogOptions[4] -> showConfirmMarkAsIncompleteDialog(post, postView)
                dialogOptions[5] -> showConfirmMarkAsIncompleteDialog(
                    post, postView, true)
                dialogOptions[6] -> showReportPostDialog(post)
                dialogOptions[7] -> {
                    val updatedPost = activity.getViewPostMap()[postView]?: post
                    activity.openViewPhotoPage(updatedPost)
                }
                dialogOptions[8] -> showConfirmDeletePostDialog(post, postView)
            }
            postMoreOptionsDialog.cancel()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        postMoreOptionsDialog.setOnCancelListener {
            activity.setOtherProcessStarted(false)
            postMoreOptionsDialog.dismiss()
        }

        postMoreOptionsDialog.show()
    }

    private fun showConfirmDeletePostDialog(post: GoalProgress, postView: View) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmDeletePostDialog = builder.create()
        confirmDeletePostDialog.setCancelable(true)

        confirmDeletePostDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val promptText = view.findViewById<TextView>(R.id.promptText)
        val deleteText = view.findViewById<TextView>(R.id.deleteText)
        promptText.text = activity.getString(R.string.confirmDeletePostPrompt)
        deleteText.text = activity.getString(R.string.option_delete)

        val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        val disabledButton = view.findViewById<LinearLayout>(R.id.disabledButton)
        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)

        fun deletePressed() {
            if(!activity.withinRequestsLimit()) return
            deleteButton.visibility = View.GONE
            disabledButton.visibility = View.VISIBLE
            cancelButton.visibility = View.INVISIBLE
            confirmDeletePostDialog.setCancelable(false)
            activity.deletePost(post, postView,
                deleteButton, disabledButton,
                cancelButton, confirmDeletePostDialog)
        }

        initButtonAnimationListener(deleteButton)
        deleteButton.setOnClickListener {
            deletePressed()
        }

        cancelButton.setOnClickListener {
            confirmDeletePostDialog.dismiss()
        }

        confirmDeletePostDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showConfirmMarkAsIncompleteDialog(post: GoalProgress, postView: View,
                                                  markAsNotRequired: Boolean = false) {
        val incompletePost = post.createProgressClone()
        incompletePost.setAsIncomplete()
        incompletePost.setBitmapPhotoUrl("")
        incompletePost.getPostLikes().clear()
        incompletePost.setAsNotRequired(markAsNotRequired,
            post.isLastPostOfDueDate())

        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_post_incomplete, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmMarkAsIncompleteDialog = builder.create()
        confirmMarkAsIncompleteDialog.setCancelable(true)

        confirmMarkAsIncompleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val promptText = view.findViewById<TextView>(R.id.promptText)
        if(markAsNotRequired) {
            promptText.text = activity.getString(R.string.deleteAndMarkAsNotRequiredPrompt)

            val deleteText = view.findViewById<TextView>(R.id.deleteText)
            deleteText.text = activity.getString(R.string.markAsNotRequiredOption)
        } else promptText.text = activity.getString(R.string.deleteAndMarkAsIncompletePrompt)

        val goalTitleText = view.findViewById<TextView>(R.id.goalTitleText)
        val incompleteForDateText = view.findViewById<TextView>(R.id.incompleteForDateText)
        val amount = post.getAmount()

        val titleString = if(amount > 0)
            "$amount ${incompletePost.getTitle()}"
        else incompletePost.getTitle()

        val hour = incompletePost.getHour()
        val minute = incompletePost.getMinute()
        goalTitleText.text = activity.getString(R.string.titleAndTimeDueString,
            titleString, getTimeText(hour, minute))

        val dueDate = post.getDueDate()
        if(dueDate != null) {
            if(markAsNotRequired)
                incompleteForDateText.text = activity.getString(
                    R.string.notRequiredDetailsString, dueDate.toDateString())
            else incompleteForDateText.text = activity.getString(
                R.string.incompleteDetailsString, dueDate.toDateString())
        } else incompleteForDateText.visibility = View.GONE

        val moreButton = view.findViewById<ImageView>(R.id.moreButton)
        moreButton.visibility = View.INVISIBLE

        val addACaptionLayout = view.findViewById<LinearLayout>(R.id.addACaptionLayout)
        val addACaptionOption = view.findViewById<TextView>(R.id.addACaptionOption)
        val captionText = view.findViewById<TextView>(R.id.captionText)
        if(post.hasCaption()) {
            captionText.text = post.getCaption()
            captionText.visibility = View.VISIBLE
            addACaptionOption.visibility = View.GONE
        }
        addACaptionLayout.setOnClickListener {
            if(!activity.postingInProgress())
                openAddIncompletePostCaptionDialog(activity,
                    addACaptionOption, captionText, incompletePost = incompletePost)
        }

        val markIncompleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        val disabledButton = view.findViewById<LinearLayout>(R.id.disabledButton)
        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        initButtonAnimationListener(markIncompleteButton)

        fun markIncompletePressed() {
            disabledButton.visibility = View.VISIBLE
            markIncompleteButton.visibility = View.GONE
            cancelButton.visibility = View.INVISIBLE
            confirmMarkAsIncompleteDialog.setCancelable(false)
            activity.postIncompleteGoal(post, incompletePost, postView,
                confirmMarkAsIncompleteDialog, disabledButton,
                markIncompleteButton, cancelButton)
        }

        markIncompleteButton.setOnClickListener {
            markIncompletePressed()
        }

        cancelButton.setOnClickListener {
            confirmMarkAsIncompleteDialog.dismiss()
        }

        confirmMarkAsIncompleteDialog.show()
    }

    private fun showReportPostDialog(post: GoalProgress) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_report_post, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val reportDialog = builder.create()
        reportDialog.setCancelable(true)

        reportDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val reportField = view.findViewById<EditText>(R.id.reportField)
        val reportButton = view.findViewById<LinearLayout>(R.id.reportButton)
        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        val disabledButton = view.findViewById<LinearLayout>(R.id.disabledButton)

        initButtonAnimationListener(reportButton)
        reportButton.setOnClickListener {
            val reportReason = reportField.text.toString()
            if(reportReason.isEmpty())
                activity.showToast(activity.getString(R.string.emptyReportReasonMessage))
            else {
                reportButton.visibility = View.GONE
                cancelButton.visibility = View.INVISIBLE
                disabledButton.visibility = View.VISIBLE
                reportDialog.setCancelable(false)
                post.setReasonReported(reportReason)
                activity.getUploader().reportPost(post, activity.getPostsUser(),
                    reportButton, cancelButton, disabledButton, reportDialog)
            }
        }

        cancelButton.setOnClickListener {
            reportDialog.dismiss()
        }

        reportField.requestFocus()
        reportDialog.show()
    }

    fun showDeletedPostNoticeDialog(post: GoalProgress) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_deleted_post_notice, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val deletedPostNoticeDialog = builder.create()
        deletedPostNoticeDialog.setCancelable(false)

        deletedPostNoticeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val amount = post.getAmount()
        val titleString = if(amount > 0)
            "$amount ${post.getTitle()}"
        else post.getTitle()

        val postedDate = getCalendar(post.getTimestampPosted()).toDateString()

        val detailsText = view.findViewById<TextView>(R.id.detailsText)
        detailsText.text = activity.getString(R.string.postAgainstStandardsDetails,
            titleString, postedDate)

        val okButton = view.findViewById<LinearLayout>(R.id.okButton)
        initButtonAnimationListener(okButton)
        okButton.setOnClickListener {
            deletedPostNoticeDialog.setCancelable(true)
            deletedPostNoticeDialog.cancel()
        }

        deletedPostNoticeDialog.setOnCancelListener {
            activity.getUploader().setDeletedPostNoticeSeen(post)
            deletedPostNoticeDialog.dismiss()
        }

        deletedPostNoticeDialog.show()
    }
}