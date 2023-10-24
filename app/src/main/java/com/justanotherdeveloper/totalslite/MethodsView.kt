package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

fun createLinkView(linkTextString: String, activity: Activity,
                   editGoalPageView: EditActivityPageView? = null,
                   createPostPageView: CreatePostPageView? = null,
                   seeAttachmentsPage: SeeAttachmentsActivity? = null,
                   toManageAttachments: Boolean = true,
                   toUpdateFirebase: Boolean = false): View {
    val linkView = activity.layoutInflater.inflate(R.layout.view_item, null)
    val linkText = linkView.findViewById<TextView>(R.id.itemText)
    linkText.text = linkTextString

    val linkLabelText = linkView.findViewById<TextView>(R.id.itemLabelText)
    linkLabelText.text = activity.getString(R.string.linkLabel)

    val linkButton = linkView.findViewById<LinearLayout>(R.id.itemButton)
    initButtonAnimationListener(linkButton, true)
    linkButton.setOnClickListener {
        activity.openLink(linkText.text.toString())
    }

    linkButton.setOnLongClickListener {
        activity.copyToClipboard(linkText.text.toString())
        true
    }

    val deleteButton = linkView.findViewById<ImageView>(R.id.deleteButton)
    deleteButton.setOnClickListener {
        editGoalPageView?.linkDeleteButtonPressed(linkView)
        createPostPageView?.linkDeleteButtonPressed(linkView)
        seeAttachmentsPage?.linkDeleteButtonPressed(linkView)
    }

    if(!toManageAttachments && !toUpdateFirebase)
        deleteButton.visibility = View.GONE

    val divider = linkView.findViewById<LinearLayout>(R.id.itemDividerBottom)
    divider.visibility = View.GONE
    return linkView
}

fun createNoteView(noteTextString: String, hideDivider: Boolean, activity: Activity,
                   editGoalPageView: EditActivityPageView? = null,
                   createPostPageView: CreatePostPageView? = null,
                   seeAttachmentsPage: SeeAttachmentsActivity? = null,
                   toManageAttachments: Boolean = true,
                   toUpdateFirebase: Boolean = false): View {
    val noteView = activity.layoutInflater.inflate(R.layout.view_item, null)
    val noteText = noteView.findViewById<TextView>(R.id.itemText)
    noteText.text = noteTextString

    val noteButton = noteView.findViewById<LinearLayout>(R.id.itemButton)
    initButtonAnimationListener(noteButton, true)
    noteButton.setOnClickListener {
        editGoalPageView?.noteButtonPressed(noteView)
        createPostPageView?.noteButtonPressed(noteView)
        seeAttachmentsPage?.noteButtonPressed(noteView)
    }

    noteButton.setOnLongClickListener {
        activity.copyToClipboard(noteText.text.toString())
        true
    }

    val deleteButton = noteView.findViewById<ImageView>(R.id.deleteButton)
    deleteButton.setOnClickListener {
        editGoalPageView?.noteDeleteButtonPressed(noteView)
        createPostPageView?.noteDeleteButtonPressed(noteView)
        seeAttachmentsPage?.noteDeleteButtonPressed(noteView)
    }

    if(!toManageAttachments && !toUpdateFirebase)
        deleteButton.visibility = View.GONE

    val divider = noteView.findViewById<LinearLayout>(R.id.itemDividerTop)
    if(hideDivider) divider.visibility = View.GONE
    return noteView
}

fun updateNoteView(noteView: View, noteTextString: String) {
    val noteText = noteView.findViewById<TextView>(R.id.itemText)
    noteText.text = noteTextString
}