package com.justanotherdeveloper.totalslite

class IntroPageListeners(private val activity: IntroPageActivity) {

    private val binding = activity.getBinding()

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
    }

    /**
     * Adds animation to a view to transition to
     * another state/color when pressed/released
     */
    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.skipButton)
        initButtonAnimationListener(binding.continueButton)
    }

    /** 'setOnClickListener' methods */
    private fun initOnClickListeners() {

        // option to skip app description/details
        // to start sign up/login flow
        binding.skipButton.setOnClickListener {
            activity.openEnterNamePage()
        }

        binding.continueButton.setOnClickListener {
            if(activity.tutorialFinished()) {
                activity.openEnterNamePage()
            } else activity.continuePressed()
        }

        binding.introTextLayout.setOnClickListener {
            if(activity.getPageNumber() == 4)
                activity.showPage(1)
            else activity.continuePressed()
        }

        binding.introBubble1.setOnClickListener {
            activity.showPage(1)
        }

        binding.introBubble2.setOnClickListener {
            activity.showPage(2)
        }

        binding.introBubble3.setOnClickListener {
            activity.showPage(3)
        }

        binding.introBubble4.setOnClickListener {
            activity.showPage(4)
        }
    }

}