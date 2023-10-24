package com.justanotherdeveloper.totalslite

class SeeAttachmentsListeners(private val activity: SeeAttachmentsActivity) {

    val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.saveButton)
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            activity.saveButtonClicked()
        }

        binding.addNoteButton.setOnClickListener {
            if(!activity.otherProcessStarted())
                activity.openNoteDialog(activity.getPost(), seeAttachmentsPage = activity)
        }

        binding.addLinkButton.setOnClickListener {
            if(!activity.otherProcessStarted())
                activity.openAddLinkDialog(seeAttachmentsPage = activity)
        }
    }
}