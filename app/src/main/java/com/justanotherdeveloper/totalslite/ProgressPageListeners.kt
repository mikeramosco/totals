package com.justanotherdeveloper.totalslite

class ProgressPageListeners(private val activity: ProgressPageActivity) {

    private val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initScrollViewListener()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
        initButtonAnimationListener(binding.sendButton)
        initButtonAnimationListener(binding.moreButton)
        initButtonAnimationListener(binding.notificationStatusLayout)
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            if(!activity.otherProcessStarted())
                activity.onBackPressed()
        }

        binding.sendButton.setOnClickListener {
            activity.searchUsersToSendProfile()
        }

        binding.moreButton.setOnClickListener {
            activity.getDialogs().showProfileOptionsDialog()
        }

        binding.seeAllTotalsButton.setOnClickListener {
            activity.openAllTotalsPage()
        }

        binding.notificationStatusLayout.setOnClickListener {
            // TODO
        }
    }

    private fun initScrollViewListener() {
        val scrollView = binding.progressPageScrollView
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (binding.progressPageContents.bottom
                <= (scrollView.height + scrollView.scrollY)) {
                if(!activity.loadingPosts())
                    activity.getView().displayNextBatchOfPosts()
            }
        }
    }
}