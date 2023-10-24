package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import androidx.core.view.isNotEmpty
import com.justanotherdeveloper.totalslite.databinding.ActivitySeeAttachmentsBinding

class SeeAttachmentsActivity : AppCompatActivity() {

    private lateinit var listeners: SeeAttachmentsListeners

    private lateinit var binding: ActivitySeeAttachmentsBinding

    private lateinit var uploader: GoalUploader

    private lateinit var original: GoalProgress
    private lateinit var post: GoalProgress

    private var toManageAttachments = false

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        val progress = getStaticProgress()
        if(progress == null) { finish(); return }
        setStaticProgress(null)
        original = progress.createProgressClone()
        post = progress.createProgressClone()

        listeners = SeeAttachmentsListeners(this)

        toManageAttachments = toManageAttachments()
        val toUpdateFirebase = toUpdateFirebase()

        for(note in progress.getNotes()) addNoteView(note,
            toManageAttachments, toUpdateFirebase, false)
        for(link in progress.getLinks()) addLinkView(link,
            toManageAttachments, toUpdateFirebase, false)

        if(!toManageAttachments) {
            binding.addNoteButton.visibility = View.GONE
            binding.addLinkButton.visibility = View.GONE
        }
        updateAddNoteButton()
        updateAddLinkButton()
        if(toUpdateFirebase)
            uploader = GoalUploader(seeAttachmentsPage = this)
    }

    private fun initBinding() {
        binding = ActivitySeeAttachmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun updateAddNoteButton() {
        if(!toManageAttachments) return
        binding.addNoteButton.visibility =
            if(post.getNotes().size < MAX_NOTES_AND_LINKS)
                View.VISIBLE else View.GONE
    }

    private fun updateAddLinkButton() {
        if(!toManageAttachments) return
        binding.addLinkButton.visibility =
            if(post.getLinks().size < MAX_NOTES_AND_LINKS)
                View.VISIBLE else View.GONE
    }

    private fun addLinkView(linkTextString: String,
                            toManageAttachments: Boolean,
                            toUpdateFirebase: Boolean,
                            animate: Boolean = true) {
        if(animate) beginTransition(binding.seeAttachmentsParent)
        val linkView = createLinkView(linkTextString,
            this, seeAttachmentsPage = this,
            toManageAttachments = toManageAttachments,
            toUpdateFirebase = toUpdateFirebase)
        binding.linksContainer.addView(linkView)
    }

    private fun addNoteView(noteTextString: String,
                            toManageAttachments: Boolean,
                            toUpdateFirebase: Boolean,
                            animate: Boolean = true) {
        if(animate) beginTransition(binding.seeAttachmentsParent)
        val hideDivider = binding.notesContainer.isNotEmpty()
        val noteView = createNoteView(noteTextString, hideDivider,
            this, seeAttachmentsPage = this,
            toManageAttachments = toManageAttachments,
            toUpdateFirebase = toUpdateFirebase)
        binding.notesContainer.addView(noteView)

    }

    fun noteButtonPressed(noteView: View) {
        val noteIndex = binding.notesContainer.indexOfChild(noteView)
        openNoteDialog(post, noteIndex, noteView, seeAttachmentsPage = this,
            toManageAttachments = toManageAttachments())
    }

    fun noteDeleteButtonPressed(noteView: View) {
        val noteIndex = binding.notesContainer.indexOfChild(noteView)
        showDeleteNoteDialog(noteIndex, noteView, seeAttachmentsPage = this)
    }

    fun linkDeleteButtonPressed(linkView: View) {
        val linkIndex = binding.linksContainer.indexOfChild(linkView)
        showDeleteLinkDialog(linkIndex, linkView, seeAttachmentsPage = this)
    }

    fun noteDialogClosed(noteField: EditText, noteView: View?, noteIndex: Int) {
        setOtherProcessStarted(false)
        if(!toManageAttachments()) return
        val noteText = noteField.text.toString()
        if(noteView == null) {
            if(noteText.isNotEmpty()) {
                post.getNotes().add(noteText)
                addNoteView(noteText, toManageAttachments = true, toUpdateFirebase = false)
                updateAddNoteButton()
            }
        } else {
            if(noteText.isEmpty()) deleteNote(noteIndex, noteView)
            else {
                post.getNotes()[noteIndex] = noteText
                updateNoteView(noteView, noteText)
            }
        }
    }

    fun deleteNote(noteIndex: Int, noteView: View) {
        post.getNotes().removeAt(noteIndex)
        removeNoteView(noteView)
        updateAddNoteButton()
    }

    private fun removeNoteView(noteView: View) {
        beginTransition(binding.seeAttachmentsParent)
        binding.notesContainer.removeView(noteView)
        if(binding.notesContainer.isNotEmpty()) {
            val topNoteView = binding.notesContainer[0]
            val divider = topNoteView.findViewById<LinearLayout>(R.id.itemDividerTop)
            divider.visibility = View.VISIBLE
        }
    }

    fun addLinkDialogClosed() {
        setOtherProcessStarted(false)
    }

    fun addLinkButtonClicked(linkField: EditText) {
        val linkText = linkField.text.toString()
        post.getLinks().add(linkText)
        addLinkView(linkText, toManageAttachments = true, toUpdateFirebase = false)
        updateAddLinkButton()
    }

    fun deleteLink(linkIndex: Int, linkView: View) {
        post.getLinks().removeAt(linkIndex)
        removeLinkView(linkView)
        updateAddLinkButton()
    }

    private fun removeLinkView(linkView: View) {
        beginTransition(binding.seeAttachmentsParent)
        binding.linksContainer.removeView(linkView)
    }

    fun saveButtonClicked() {
        if(noChangesMade() || !toUpdateFirebase())
            returnToPreviousPage()
        else {
            binding.saveButton.visibility = View.GONE
            binding.backButton.visibility = View.GONE
            binding.backButtonDisabled.visibility = View.VISIBLE
            binding.progressCircle.visibility = View.VISIBLE
            uploader.updatePostAttachments(post)
        }
    }

    fun confirmAttachmentsUpdatedSuccess() {
        showToast(getString(R.string.attachmentsUpdatedMessage))
        returnToPreviousPage()
    }

    fun notifyUpdateFailure() {
        binding.saveButton.visibility = View.VISIBLE
        binding.backButton.visibility = View.VISIBLE
        binding.backButtonDisabled.visibility = View.GONE
        binding.progressCircle.visibility = View.GONE
        showRequestUnavailableToast()
    }

    private fun returnToPreviousPage() {
        setStaticProgress(post)
        if(!noChangesMade()) {
            val intentData = Intent()
            intentData.putExtra(TO_UPDATE_POST_REF, true)
            setResult(Activity.RESULT_OK, intentData)
        }
        finish()
    }

    private fun showConfirmClosePageDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_confirm_close_page, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmClosePageDialog = builder.create()
        confirmClosePageDialog.setCancelable(true)

        confirmClosePageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val detailsText = view.findViewById<TextView>(R.id.detailsText)
        detailsText.text = getString(R.string.closePostAttachmentsPageDialogDetails)

        val keepEditingButton = view.findViewById<LinearLayout>(R.id.keepEditingButton)
        initButtonAnimationListener(keepEditingButton)
        keepEditingButton.setOnClickListener {
            confirmClosePageDialog.cancel()
        }

        val closeButton = view.findViewById<LinearLayout>(R.id.closeButton)
        closeButton.setOnClickListener {
            finish()
            confirmClosePageDialog.cancel()
        }

        confirmClosePageDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            confirmClosePageDialog.dismiss()
        }

        confirmClosePageDialog.show()
    }

    private fun noChangesMade(): Boolean {
        val originalNotes = original.getNotes().toString()
        val originalLinks = original.getLinks().toString()
        val postNotes = post.getNotes().toString()
        val postLinks = post.getLinks().toString()
        return originalNotes == postNotes && originalLinks == postLinks
    }

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    override fun onBackPressed() {
        if(otherProcessStarted()) return
        if(noChangesMade()) super.onBackPressed()
        else if(uploader.postingInProgress()) goToHomeScreen()
        else showConfirmClosePageDialog()
    }

    fun getBinding(): ActivitySeeAttachmentsBinding {
        return binding
    }

    fun getPost(): GoalProgress {
        return post
    }

    private fun toManageAttachments(): Boolean {
        return intent.getBooleanExtra(MANAGE_ATTACHMENTS_REF, false)
    }

    private fun toUpdateFirebase(): Boolean {
        return intent.getBooleanExtra(UPDATE_FIREBASE_REF, false)
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