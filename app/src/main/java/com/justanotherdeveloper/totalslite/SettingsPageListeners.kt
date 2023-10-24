package com.justanotherdeveloper.totalslite

class SettingsPageListeners(private val activity: HomePageActivity,
                            private val fragment: SettingsPageFragment) {

    private val binding = fragment.getFragmentBinding()

    private lateinit var scrollViewButtons: ButtonAnimationScrollView

    init {
        initButtonAnimationListenersInScrollView()
        initOnClickListeners()
    }

    private fun initButtonAnimationListenersInScrollView() {
        scrollViewButtons = ButtonAnimationScrollView(
            binding.settingsPageScroll, binding.settingsPageScrollContents)
        scrollViewButtons.addButton(binding.editProfileInfoButton)
        scrollViewButtons.addButton(binding.missedDueDateActionButton)
        scrollViewButtons.addButton(binding.manageBlockedUsersButton)
        scrollViewButtons.addButton(binding.manageBannedUsersButton)
        scrollViewButtons.addButton(binding.manageReportedPostsButton)
        scrollViewButtons.addButton(binding.manageResolvedReportsButton)
        scrollViewButtons.addButton(binding.aboutTotalsButton)
        scrollViewButtons.addButton(binding.logoutButton)
    }

    private fun initOnClickListeners() {
        binding.editProfileInfoButton.setOnClickListener {
            fragment.openEditProfilePage()
        }

        binding.missedDueDateActionButton.setOnClickListener {
            fragment.showChooseMissedDueTimeActionDialog()
        }

        binding.manageBlockedUsersButton.setOnClickListener {
            fragment.openManageBlockedUsersPage()
        }

        binding.logoutButton.setOnClickListener {
            fragment.showConfirmLogoutDialog()
        }

        binding.manageBannedUsersButton.setOnClickListener {
            fragment.showAccessCodeDialog(manageBannedUsers = true)
        }

        binding.manageReportedPostsButton.setOnClickListener {
            fragment.showAccessCodeDialog(manageReportedPosts = true)
        }

        binding.manageResolvedReportsButton.setOnClickListener {
            fragment.showAccessCodeDialog(manageResolvedReports = true)
        }

        binding.aboutTotalsButton.setOnClickListener {
            fragment.aboutButtonClicked()
        }
    }

}