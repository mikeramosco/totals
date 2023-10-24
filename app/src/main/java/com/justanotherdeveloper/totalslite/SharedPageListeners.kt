package com.justanotherdeveloper.totalslite

class SharedPageListeners(private val activity: HomePageActivity,
                          private val fragment: SharedPageFragment) {

    private val binding = fragment.getFragmentBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initButtonAnimationListenersInScrollView()
    }

    private fun initButtonAnimationListeners() {
        //
    }

    private fun initOnClickListeners() {
        //
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.goalPageScrollView, binding.goalPageContents)
    }

    fun getScrollViewButtonAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }
}