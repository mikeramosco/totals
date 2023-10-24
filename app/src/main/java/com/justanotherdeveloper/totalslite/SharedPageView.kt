package com.justanotherdeveloper.totalslite

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.size
import com.google.firebase.database.DataSnapshot

class SharedPageView(private val activity: HomePageActivity,
                     private val fragment: SharedPageFragment) {

    private val binding = fragment.getFragmentBinding()
    private val notifProfiles = ArrayList<String>()
    private val keyNewPostsMap = HashMap<String, ArrayList<String>>()
    private val keyInvitesMap = HashMap<String, Profile>()
    private val keyProfileMap = HashMap<String, Profile>()
    private val keyViewMap = HashMap<String, View>()
    private var lastUpdatedData: DataSnapshot? = null

    fun updateSharedProfiles(data: DataSnapshot? = lastUpdatedData) {
        if(data == null) return
        lastUpdatedData = data
        beginTransition(binding.sharedPageParent)
        val signedInUser = getStaticSignedInTotalsUser(activity, activity.getDatabase())?: return
        val signedInUserId = signedInUser.getUserId()
        val updatedKeyInvitesMap = HashMap<String, Profile>()
        val updatedKeyProfileMap = HashMap<String, Profile>()
        val dataIterator = data.children.iterator()
        while(dataIterator.hasNext()) {
            val profileData = dataIterator.next()
            val label = profileData.child(PROFILE_LABEL_PATH).value.toString()
            val userId = profileData.child(USER_ID_PATH).value.toString().toInt()
            val followingValue = profileData.child(signedInUserId.toString()).value
            val totalsStartDateTimestampValue = profileData.child(TOTALS_START_DATE_PATH).value
            val user = getStaticUsersMap()[userId]
            if(user != null && !user.isBanned()) {
                val profile = Profile(user, label)
                if(followingValue != null) {
                    if(signedInUser.getBlockedUsers().contains(userId))
                        fragment.getProfiles().respondToInvite(profile, signedInUserId)
                    else {
                        val following = followingValue.toString().toBoolean()
                        if (following) {
                            val totalsStartDate = if(totalsStartDateTimestampValue != null)
                                getCalendar(totalsStartDateTimestampValue
                                    .toString().toLong()) else null
                            updateStaticTotalsStartDate(userId, label, totalsStartDate)
                            if (!getStaticUsersListeningList().contains(userId))
                                activity.initPostsListener(userId)
                            updatedKeyProfileMap[profile.getProfileKey()] = profile
                        } else updatedKeyInvitesMap[profile.getProfileKey()] = profile
                    }
                }
            }
        }

        removeOutdatedViews(updatedKeyInvitesMap, updatedKeyProfileMap)
        addNewProfileViews(updatedKeyInvitesMap, updatedKeyProfileMap)
        updateEmptyPageMessageVisibility()
        updateProfileNotifications()
    }

    private fun updateEmptyPageMessageVisibility() {
        binding.emptySharedPageMessageLayout.visibility =
            if(binding.invitesContainer.isVisible
                || binding.profilesContainer.isVisible)
                View.GONE else View.VISIBLE
    }

    fun updateSeenPosts(profile: Profile) {
        val seenPosts = keyNewPostsMap[profile.getProfileKey()]?: return
        if(seenPosts.isNotEmpty())
            setProfileSeenPosts(profile, activity.getDatabase(), seenPosts)
    }

    fun updateProfileNotifications() {
        notifProfiles.clear()
        keyNewPostsMap.clear()
        for(profile in keyProfileMap.values.iterator())
            updateNotification(profile)
        binding.profilesNotifIcon.visibility =
            if(notifProfiles.isEmpty())
                View.GONE else View.VISIBLE
        updateSharedNotifIcon()
    }

    private fun updateSharedNotifIcon() {
        activity.getBinding().sharedNotifIcon.visibility =
            if(notifProfiles.isNotEmpty() || binding.invitesSectionTitleLayout.isVisible)
                View.VISIBLE else View.GONE
    }

    private fun removeOutdatedViews(updatedKeyInvitesMap: HashMap<String, Profile>,
                                    updatedKeyProfileMap: HashMap<String, Profile>) {
        val updatedInviteKeys = updatedKeyInvitesMap.keys
        val profilesToDelete = ArrayList<String>()
        for(inviteKey in keyInvitesMap.keys.iterator()) {
            if(!updatedInviteKeys.contains(inviteKey)) {
                profilesToDelete.add(inviteKey)
                val profileView = keyViewMap[inviteKey]
                if(profileView != null) {
                    keyViewMap.remove(inviteKey)
                    binding.invitesContainer.removeView(profileView)
                    updateInvitesSectionVisibility()
                }
            }
        }
        for(inviteKey in profilesToDelete)
            keyInvitesMap.remove(inviteKey)

        profilesToDelete.clear()
        val updatedProfileKeys = updatedKeyProfileMap.keys
        for(profileKey in keyProfileMap.keys.iterator()) {
            if(!updatedProfileKeys.contains(profileKey)) {
                profilesToDelete.add(profileKey)
                val profileView = keyViewMap[profileKey]
                if(profileView != null) {
                    keyViewMap.remove(profileKey)
                    binding.profilesContainer.removeView(profileView)
                    updateProfilesSectionVisibility()
                }
            }
        }
        for(profileKey in profilesToDelete)
            keyProfileMap.remove(profileKey)
        profilesToDelete.clear()
    }

    private fun addNewProfileViews(updatedKeyInvitesMap: HashMap<String, Profile>,
                                    updatedKeyProfileMap: HashMap<String, Profile>) {
        val invitesToAddMap = HashMap<String, ArrayList<View>>()
        val currentInviteKeys = keyInvitesMap.keys
        for(inviteKey in updatedKeyInvitesMap.keys.iterator()) {
            if(!currentInviteKeys.contains(inviteKey)) {
                val profile = updatedKeyInvitesMap[inviteKey]
                if(profile != null) {
                    keyInvitesMap[inviteKey] = profile
                    val inviteView = createInviteView(profile)
                    val profileUsername = profile.getUser().getUsername()
                    val invites = invitesToAddMap[profileUsername]?: ArrayList()
                    invites.add(inviteView)
                    invitesToAddMap[profileUsername] = invites
                }
            }
        }

        val orderedUsernames = ArrayList<String>()
        for(username in invitesToAddMap.keys.iterator())
            orderedUsernames.add(username)
        orderedUsernames.sort()
        for(username in orderedUsernames) {
            val invites = invitesToAddMap[username]
            if(invites != null)
                for(inviteView in invites)
                    binding.invitesContainer.addView(inviteView)
        }
        orderedUsernames.clear()

        val profilesToAddMap = HashMap<String, ArrayList<View>>()
        val currentProfileKeys = keyProfileMap.keys
        for(profileKey in updatedKeyProfileMap.keys.iterator()) {
            if(!currentProfileKeys.contains(profileKey)) {
                val profile = updatedKeyProfileMap[profileKey]
                if(profile != null) {
                    keyProfileMap[profileKey] = profile
                    val profileView = createProfileView(profile)
                    val profileUsername = profile.getUser().getUsername()
                    val profiles = profilesToAddMap[profileUsername]?: ArrayList()
                    profiles.add(profileView)
                    profilesToAddMap[profileUsername] = profiles
                }
            }
        }

        for(username in profilesToAddMap.keys.iterator())
            orderedUsernames.add(username)
        orderedUsernames.sort()
        for(username in orderedUsernames) {
            val profiles = profilesToAddMap[username]
            if(profiles != null)
                for(profileView in profiles)
                    binding.profilesContainer.addView(profileView)
        }
    }

    private fun createInviteView(profile: Profile): View {
        val inviteView = activity.layoutInflater.inflate(R.layout.view_profile_invite, null)
        val profilePhotoLayout = inviteView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = inviteView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = inviteView.findViewById<TextView>(R.id.profilePhotoLetter)
        val profileText = inviteView.findViewById<TextView>(R.id.profileText)
        val profileInfoText = inviteView.findViewById<TextView>(R.id.profileInfoText)
        val acceptButton = inviteView.findViewById<LinearLayout>(R.id.acceptButton)
        val removeButton = inviteView.findViewById<LinearLayout>(R.id.removeButton)
        val moreButton = inviteView.findViewById<LinearLayout>(R.id.moreButton)

        keyViewMap[profile.getProfileKey()] = inviteView

        profilePhotoLayout.visibility = View.VISIBLE

        val buttonAnimation = fragment.getFragmentListeners().getScrollViewButtonAnimation()
        buttonAnimation.addButton(acceptButton)
        buttonAnimation.addButton(removeButton)
        buttonAnimation.addButton(moreButton)

        val signedInUserId = getSignedInUserId(activity.getDatabase())

        var optionSelected = false

        acceptButton.setOnClickListener {
            if(!optionSelected)
                fragment.getProfiles().respondToInvite(profile, signedInUserId, true)
            optionSelected = true
        }

        removeButton.setOnClickListener {
            if(!optionSelected)
                fragment.getProfiles().respondToInvite(profile, signedInUserId)
            optionSelected = true
        }

        moreButton.setOnClickListener {
            fragment.getFragmentDialogs().showSharedProfileMoreOptionsDialog(profile, true)
        }

        profile.getUser().displayOnView(activity, profilePhotoImage, profilePhotoLetter)

        profileText.text = activity.getString(
            R.string.progressPageTitle, profile.getLabel())
        profileInfoText.text = activity.getString(
            R.string.progressPageTitleDetails, profile.getUser().getName())

        binding.invitesSectionTitleLayout.visibility = View.VISIBLE
        binding.invitesContainer.visibility = View.VISIBLE
//        binding.invitesContainer.addView(inviteView)
        return inviteView
    }

    private fun updateNotification(profile: Profile) {
        val profileView = keyViewMap[profile.getProfileKey()]?: return
        val newPostsCountText = profileView.findViewById<TextView>(R.id.newPostsCountText)
        val newPostsNotifCircle = profileView.findViewById<ImageView>(R.id.newPostsNotifCircle)

        fun clearNotifs() {
            newPostsCountText.visibility = View.GONE
            newPostsNotifCircle.visibility = View.GONE
        }

        val posts = getLabeledPosts(profile)?: return
        val newPosts = ArrayList<String>()
        for(post in posts) {
            val postedDate = getCalendar(post.getTimestampPosted())
            if(isToday(postedDate)) newPosts.add(post.getPostFirebaseKey())
        }
        if(newPosts.isEmpty()) { clearNotifs(); return }
        keyNewPostsMap[profile.getProfileKey()] = newPosts

        val unseenPosts = newPosts.stringArrayListClone()
        val profileNotifDate = getProfileNotifDate(profile, activity.getDatabase())
        if(profileNotifDate != null && isToday(profileNotifDate)) {
            val seenPosts = getProfileSeenPosts(profile, activity.getDatabase())
            if(seenPosts != null) for(postKey in seenPosts) unseenPosts.remove(postKey)
        }
        if(unseenPosts.isEmpty()) { clearNotifs(); return }
        notifProfiles.add(profile.getProfileKey())

        val unseenPostsSize = unseenPosts.size.toString()
        newPostsCountText.text = if(unseenPostsSize == "1")
            activity.getString(R.string.oneNewPostCountText)
        else activity.getString(R.string.newPostsCountText, unseenPostsSize)
        newPostsCountText.visibility = View.VISIBLE
        newPostsNotifCircle.visibility = View.VISIBLE
    }

    private fun createProfileView(profile: Profile): View {
        val profileView = activity.layoutInflater.inflate(R.layout.view_profile_following, null)
        val profileLayout = profileView.findViewById<LinearLayout>(R.id.profileLayout)
        val profilePhotoLayout = profileView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = profileView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = profileView.findViewById<TextView>(R.id.profilePhotoLetter)
        val profileText = profileView.findViewById<TextView>(R.id.profileText)
        val profileInfoText = profileView.findViewById<TextView>(R.id.profileInfoText)
        val moreButton = profileView.findViewById<LinearLayout>(R.id.moreButton)

        keyViewMap[profile.getProfileKey()] = profileView

        profilePhotoLayout.visibility = View.VISIBLE

        val buttonAnimation = fragment.getFragmentListeners().getScrollViewButtonAnimation()
        buttonAnimation.addButton(profileLayout)
        buttonAnimation.addButton(moreButton)

        profileLayout.setOnClickListener {
            fragment.openProgressPage(profile)
        }

        moreButton.setOnClickListener {
            fragment.getFragmentDialogs().showSharedProfileMoreOptionsDialog(profile)
        }

        profile.getUser().displayOnView(activity, profilePhotoImage, profilePhotoLetter)

        profileText.text = activity.getString(
            R.string.progressPageTitle, profile.getLabel())
        profileInfoText.text = activity.getString(
            R.string.progressPageTitleDetails, profile.getUser().getName())

        binding.profilesSectionTitle.visibility = View.VISIBLE
        binding.profilesContainer.visibility = View.VISIBLE
//        binding.profilesContainer.addView(profileView)
        return profileView
    }

    private fun updateInvitesSectionVisibility() {
        if(binding.invitesContainer.size == 0) {
            binding.invitesSectionTitleLayout.visibility = View.GONE
            binding.invitesContainer.visibility = View.GONE
        }
    }

    private fun updateProfilesSectionVisibility() {
        if(binding.profilesContainer.size == 0) {
            binding.profilesSectionTitle.visibility = View.GONE
            binding.profilesContainer.visibility = View.GONE
        }
    }
}