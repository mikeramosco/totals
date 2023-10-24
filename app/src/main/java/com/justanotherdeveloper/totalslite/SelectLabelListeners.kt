package com.justanotherdeveloper.totalslite

class SelectLabelListeners(private val activity: SelectLabelActivity) {

    private val binding = activity.getBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListeners()
        initOnClickListeners()
        initButtonAnimationListenersInScrollView()
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
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.labelsScrollView, binding.labelsContents)
    }

    fun getScrollViewButtonAnimation(): ButtonAnimationScrollView {
        return scrollViewButtons
    }
}