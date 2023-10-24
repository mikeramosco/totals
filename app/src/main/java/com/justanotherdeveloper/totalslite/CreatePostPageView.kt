package com.justanotherdeveloper.totalslite

import android.net.Uri
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.isNotEmpty
import androidx.core.view.size

class CreatePostPageView(private val activity: CreatePostPageActivity) {

    private val binding = activity.getBinding()

    init {
        initDisplay()
        updateLabels(false)
    }

    private fun initDisplay() {
        val progress = activity.getGoalProgress()
        if(progress.getAmount() == 0) {
            binding.goalAmountField.visibility = View.GONE
            binding.goalAmountField.setText("")
        } else {
            binding.goalAmountField.setText(activity.getAmountRemaining().toString())
            binding.goalAmountField.post {
                moveCursorTo(binding.goalAmountField)
            }
        }
        binding.goalTitleText.text = progress.getTitle()

        binding.dateText.text = activity.getDateTextString(
            progress, true, progress.getDueDate())
        binding.postDetailsText.text = activity.getPostedTextString(
            activity.getGoalProgress(), getTimeStamp())

        for(note in progress.getNotes()) addNoteView(note, false)
        for(link in progress.getLinks()) addLinkView(link, false)

        updateAddNoteButton()
        updateAddLinkButton()
    }

    fun updateAddNoteButton() {
        binding.addNoteButton.visibility =
            if(activity.getGoalProgress().getNotes().size < MAX_NOTES_AND_LINKS)
                View.VISIBLE else View.GONE
    }

    fun updateAddLinkButton() {
        binding.addLinkButton.visibility =
            if(activity.getGoalProgress().getLinks().size < MAX_NOTES_AND_LINKS)
                View.VISIBLE else View.GONE
    }

    fun updateLabels(animate: Boolean = true) {
        if(animate) beginTransition(binding.createPostPageParent)
        while(binding.labelsContainer.size > 1)
            binding.labelsContainer.removeViewAt(0)

        val goalLabels = activity.getGoalProgress().getLabels()
        for(label in goalLabels) addLabelView(label)

        binding.goalTagsScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT)
    }

    fun photoSet(): Boolean {
        return binding.progressPhotoLayout.visibility == View.VISIBLE
    }

    fun setCaption(caption: String) {
        binding.captionText.text = caption
        if(caption.isEmpty()) {
            binding.addACaptionOption.visibility = View.VISIBLE
            binding.captionText.visibility = View.GONE
        } else {
            binding.addACaptionOption.visibility = View.GONE
            binding.captionText.visibility = View.VISIBLE
        }
    }

    private fun addLabelView(label: String) {
        val labelView = activity.layoutInflater.inflate(R.layout.view_label_added, null)
        val labelButton = labelView.findViewById<LinearLayout>(R.id.labelButton)
        val labelText = labelView.findViewById<TextView>(R.id.labelText)
        val removeLabelButton = labelView.findViewById<ImageView>(R.id.removeLabelButton)

        labelText.text = label

        labelButton.setOnClickListener {
            activity.showManageLabelsDialog(activity.getGoalProgress(), createPostPage = activity)
        }

        removeLabelButton.setOnClickListener {
            activity.showRemoveLabelDialog(activity.getGoalProgress(), label, labelView,
                createPostPageView = this)
        }

        activity.getListeners().getScrollViewAnimation().addButton(labelButton)

        binding.labelsContainer.addView(labelView, 0)
    }

    fun removeLabel(label: String, labelView: View) {
        beginTransition(binding.createPostPageParent)
        binding.labelsContainer.removeView(labelView)
        activity.getGoalProgress().getLabels().remove(label)
        updateGoalLabels(activity.getGoalProgress(), activity.getDatabase())
    }

    fun addLinkView(linkTextString: String, animate: Boolean = true) {
        if(animate) beginTransition(binding.createPostPageParent)
        val linkView = createLinkView(linkTextString, activity, createPostPageView = this)
        binding.linksContainer.addView(linkView)
    }

    fun addNoteView(noteTextString: String, animate: Boolean = true) {
        if(animate) beginTransition(binding.createPostPageParent)
        val hideDivider = binding.notesContainer.isNotEmpty()
        val noteView = createNoteView(noteTextString, hideDivider,
            activity, createPostPageView = this)
        binding.notesContainer.addView(noteView)
    }

    fun linkDeleteButtonPressed(linkView: View) {
        val linkIndex = binding.linksContainer.indexOfChild(linkView)
        activity.showDeleteLinkDialog(linkIndex, linkView, createPostPage = activity)
    }

    fun clearFocus() {
        binding.goalAmountField.clearFocus()
    }

    fun noteButtonPressed(noteView: View) {
        val noteIndex = binding.notesContainer.indexOfChild(noteView)
        activity.openNoteDialog(activity.getGoalProgress(), noteIndex, noteView,
            createPostPageDialogs = activity.getDialogs())
        clearFocus()
    }

    fun noteDeleteButtonPressed(noteView: View) {
        val noteIndex = binding.notesContainer.indexOfChild(noteView)
        activity.showDeleteNoteDialog(noteIndex, noteView, createPostPage = activity)
    }

    fun removeNoteView(noteView: View) {
        beginTransition(binding.createPostPageParent)
        binding.notesContainer.removeView(noteView)
        if(binding.notesContainer.isNotEmpty()) {
            val topNoteView = binding.notesContainer[0]
            val divider = topNoteView.findViewById<LinearLayout>(R.id.itemDividerTop)
            divider.visibility = View.VISIBLE
        }
    }

    fun removeLinkView(linkView: View) {
        beginTransition(binding.createPostPageParent)
        binding.linksContainer.removeView(linkView)
    }

    fun setBackgroundPhoto(imageUri: Uri) {
        binding.transparentLayer.visibility = View.VISIBLE
        binding.progressPhotoLayout.visibility = View.VISIBLE
        binding.progressPhoto.setImageURI(imageUri)
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
        setStaticUsersPostsUpdated()
    }
}