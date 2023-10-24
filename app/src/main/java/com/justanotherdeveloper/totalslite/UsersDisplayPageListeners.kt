package com.justanotherdeveloper.totalslite

class UsersDisplayPageListeners(private val activity: UsersDisplayPageActivity) {

    private val binding = activity.getBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initButtonAnimationListenersInScrollView()
    }

    private fun initButtonAnimationListeners() {
        initButtonAnimationListener(binding.backButton)
    }

    private fun initOnClickListeners() {
        binding.backButton.setOnClickListener {
            activity.onBackPressed()
        }
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.usersScrollView, binding.usersContents)
    }

    fun getScrollViewButtonAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }
}