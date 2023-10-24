package com.justanotherdeveloper.totalslite

class HomePageView(private val activity: HomePageActivity) {

    private val binding = activity.getBinding()

    fun initProfileIcon() {
        getStaticSignedInTotalsUser(activity, activity.getDatabase())
            ?.displayOnView(activity, binding.profileIconPhoto, binding.profileIconLetter)
    }

}