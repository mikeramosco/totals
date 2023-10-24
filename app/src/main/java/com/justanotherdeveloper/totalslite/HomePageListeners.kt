package com.justanotherdeveloper.totalslite

class HomePageListeners(private val activity: HomePageActivity) {

    private val binding = activity.getBinding()

    init {
        initOnClickListeners()
    }

    private fun initOnClickListeners() {
        binding.settingsNavOption.setOnClickListener {
            activity.displayFragmentContainer(displaySettings = true)
        }

        binding.sharedNavOption.setOnClickListener {
            activity.displayFragmentContainer(displayShared = true)
        }

        binding.profileNavOption.setOnClickListener {
            activity.displayFragmentContainer()
        }
    }

}