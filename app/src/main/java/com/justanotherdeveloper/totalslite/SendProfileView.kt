package com.justanotherdeveloper.totalslite

import android.os.Handler
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.iterator

class SendProfileView(private val activity: SendProfileActivity) {

    private val userViews = SparseArray<View>()
    val binding = activity.getBinding()
    private val handler = Handler()

    init {
        addUserViews()
    }

    fun applySearch(searchedText: String = binding.searchField.text.toString().lowercase(),
                    animate: Boolean = true) {
        val usersSortedBySearch = getUsersSortedBySearch(searchedText)
        handler.post {
            if(animate) beginTransition(binding.sendProfilePageParent)
            for((index, userId) in usersSortedBySearch.withIndex()) {
                val userView = userViews[userId]
                if(userView != null) {
                    binding.usersContainer.removeView(userView)
                    binding.usersContainer.addView(userView, index)
                }
            }
            for(view in binding.usersContainer.iterator())
                view.visibility = View.GONE
            for(userId in usersSortedBySearch)
                userViews[userId]?.visibility = View.VISIBLE
            updateEmptyPageMessageVisibility(usersSortedBySearch, searchedText)
        }
    }

    private fun updateEmptyPageMessageVisibility(usersSortedBySearch: ArrayList<Int>,
                                                 searchedText: String) {
        binding.emptySearchPageMessageLayout.visibility =
            if(usersSortedBySearch.isNotEmpty() || searchedText.isEmpty())
                View.GONE else View.VISIBLE
    }

    private fun getUsersSortedBySearch(searchedText: String): ArrayList<Int> {
        val usersSortedBySearch = ArrayList<Int>()

        fun conditionMet(conditionIndex: Int, user: TotalsUser): Boolean {
            return when(conditionIndex) {
                0 -> user.getUsername().lowercase().startsWith(searchedText)
                1 -> user.getName().lowercase().startsWith(searchedText)
                2 -> user.getUsername().lowercase().contains(searchedText)
                else -> user.getName().lowercase().contains(searchedText)
            }
        }

        fun addOnceToList(userId: Int) {
            if(!usersSortedBySearch.contains(userId))
                usersSortedBySearch.add(userId)
        }

        fun addUsersForCondition(conditionIndex: Int) {
            for(user in getStaticUsersMap().values.iterator())
                if(user.getUserId() != getSignedInUserId(activity.getDatabase())
                    && conditionMet(conditionIndex, user))
                    addOnceToList(user.getUserId())
        }

        if(searchedText.isNotEmpty()) {
            addUsersForCondition(0)
            addUsersForCondition(1)
            addUsersForCondition(2)
            addUsersForCondition(3)
        }

        return usersSortedBySearch
    }

    private fun addUserViews() {
        val signedInUserId = getSignedInUserId(activity.getDatabase())
        val users = getStaticUsersMap().values.iterator()
        val followers = activity.getProfile().getFollowers().keys
        for(user in users) {
            val userId = user.getUserId()
            if(!user.isBanned()
                && !followers.contains(userId)
                && userId != signedInUserId
                && !user.getBlockedUsers().contains(signedInUserId)) {
                val userView = user.toUserView()
                userViews[user.getUserId()] = userView
                userView.visibility = View.GONE
                binding.usersContainer.addView(userView)
            }
        }
    }

    private fun TotalsUser.toUserView(): View {
        val userView = activity.layoutInflater.inflate(R.layout.view_user_searched, null)
        val profilePhotoLayout = userView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = userView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = userView.findViewById<TextView>(R.id.profilePhotoLetter)
        val usernameText = userView.findViewById<TextView>(R.id.usernameText)
        val nameText = userView.findViewById<TextView>(R.id.nameText)
        profilePhotoLayout.visibility = View.VISIBLE
        displayOnView(activity, profilePhotoImage, profilePhotoLetter, usernameText, nameText)
        initUserViewListener(userView, this)
        return userView
    }

    private fun initUserViewListener(userView: View, user: TotalsUser) {
        var userSelected = false
        val userLayout = userView.findViewById<LinearLayout>(R.id.totalsUserLayout)
        val checkbox = userView.findViewById<ImageView>(R.id.checkbox)
        activity.getListeners().getScrollViewButtonAnimation().addButton(userLayout)
        userLayout.setOnClickListener {
            userSelected = if(userSelected) {
                activity.getSelectedUsers().remove(user.getUserId())
                checkbox.setImageResource(R.drawable.ic_check_box_circle_unchecked)
                false
            } else {
                activity.getSelectedUsers().add(user.getUserId())
                checkbox.setImageResource(R.drawable.ic_check_box_circle_checked)
                true
            }
            updateSendButton()
        }
    }

    private fun updateSendButton() {
        if(activity.getSelectedUsers().isEmpty()) {
            binding.sendButtonDisabled.visibility = View.VISIBLE
            binding.sendButton.visibility = View.GONE
        } else {
            binding.sendButtonDisabled.visibility = View.GONE
            binding.sendButton.visibility = View.VISIBLE
        }
    }

    fun showSendingState() {
        binding.sendButtonDisabled.visibility = View.VISIBLE
        binding.sendButton.visibility = View.GONE
        binding.sendButtonProgressCircle.visibility = View.VISIBLE
        binding.sendButtonText.visibility = View.GONE
        binding.backButtonDisabled.visibility = View.VISIBLE
        binding.backButton.visibility = View.GONE
    }

    fun notifyPostFailure() {
        binding.backButtonDisabled.visibility = View.GONE
        binding.backButton.visibility = View.VISIBLE
        binding.sendButtonProgressCircle.visibility = View.GONE
        binding.sendButtonText.visibility = View.VISIBLE
        binding.sendButtonDisabled.visibility = View.GONE
        binding.sendButton.visibility = View.VISIBLE
        activity.showRequestUnavailableToast()
    }
}