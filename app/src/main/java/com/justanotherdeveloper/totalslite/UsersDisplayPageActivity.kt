package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.iterator
import com.justanotherdeveloper.totalslite.databinding.ActivityUsersDisplayPageBinding
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class UsersDisplayPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersDisplayPageBinding

    private lateinit var listeners: UsersDisplayPageListeners

    private lateinit var profile: Profile
    private lateinit var profiles: ProfileSender

    private lateinit var signedInUser: TotalsUser
    private lateinit var accounts: TotalsAccountEditor

    private lateinit var likedPost: GoalProgress
    private lateinit var uploader: GoalUploader
    private lateinit var newViewTimestampMap: HashMap<View, Long>
    private lateinit var seenViewTimestampMap: HashMap<View, Long>

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        listeners = UsersDisplayPageListeners(this)

        when(intent.getStringExtra(USERS_TO_DISPLAY_REF)) {
            FOR_POST_LIKES_REF -> setupToDisplayPostLikes()
            FOR_ALL_TOTALS_REF -> setupToDisplayAllTotals()
            FOR_MANAGE_FOLLOWERS_REF -> setupToManageFollowers()
            FOR_POST_LIKES_NOTIFS_REF -> setupToDisplayPostLikesNotifs()
            FOR_MANAGE_BLOCKED_USERS_REF -> setupToManageBlockedUsers()
            FOR_MANAGE_BANNED_USERS_REF -> setupToManageBannedUsers()
            else -> finish()
        }
    }

    private fun setupToDisplayPostLikesNotifs() {
        val selectedLabel = intent.getStringExtra(SELECTED_LABEL_REF)?: ""
        if(selectedLabel.isNotEmpty()) {
            binding.usersDisplayTitleDetails.visibility = View.VISIBLE
            binding.usersDisplayTitleDetails.text =
                getString(R.string.postLikesNotifsDetails, selectedLabel)
        }
        binding.usersDisplayTitleText.text = getString(R.string.postLikesNotifsTitle)
        uploader = GoalUploader(postLikesNotifsPage = this)
        uploader.updateSeenLikes(selectedLabel)
        createNotifViews(selectedLabel)
        addNotifViewsInOrderByRecency()
        updateEmptyPageMessageVisibility()
    }

    private fun updateEmptyPageMessageVisibility() {
        binding.emptyLikesPageMessageLayout.visibility =
            if(binding.usersContainer.isEmpty())
                View.VISIBLE else View.GONE
    }

    private fun addNotifViewsInOrderByRecency() {
        val orderedNewViews = ArrayList<View>()
        val orderedSeenViews = ArrayList<View>()

        fun orderViews(timestampMap: HashMap<View, Long>, orderedViews: ArrayList<View>) {
            for (notifView in timestampMap.keys.iterator()) {
                var notifViewAdded = false
                val timestamp = timestampMap[notifView]
                if (timestamp != null) {
                    for ((i, addedNotifView) in orderedViews.withIndex()) {
                        val addedTimestamp = timestampMap[addedNotifView]
                        if (addedTimestamp != null) {
                            if (timestamp > addedTimestamp) {
                                orderedViews.add(i, notifView)
                                notifViewAdded = true
                                break
                            }
                        }
                    }
                }
                if (!notifViewAdded) orderedViews.add(notifView)
            }
        }

        orderViews(newViewTimestampMap, orderedNewViews)
        orderViews(seenViewTimestampMap, orderedSeenViews)

        for(view in orderedNewViews) binding.usersContainer.addView(view)
        for(view in orderedSeenViews) binding.usersContainer.addView(view)
    }

    private fun createNotifViews(selectedLabel: String) {
        newViewTimestampMap = HashMap()
        seenViewTimestampMap = HashMap()

        val newLikesMap = getStaticNewLikesMap()
        val seenLikesMap = getStaticSeenLikesMap()

        for(post in newLikesMap.keys.iterator()) {
            if(selectedLabel.isEmpty() || post.getLabels().contains(selectedLabel)) {
                val postLikes = newLikesMap[post]
                if (postLikes != null)
                    createNotifView(post, postLikes, true)
            }
        }

        for(post in seenLikesMap.keys.iterator()) {
            if(selectedLabel.isEmpty() || post.getLabels().contains(selectedLabel)) {
                val postLikes = seenLikesMap[post]
                if (postLikes != null)
                    createNotifView(post, postLikes)
            }
        }
    }

    private fun createNotifView(post: GoalProgress,
                                postLikes: HashMap<Int, Long>,
                                isNew: Boolean = false) {
        val notifView = layoutInflater.inflate(R.layout.view_post_like_notif, null)
        val notifText = notifView.findViewById<TextView>(R.id.notifText)
        val recencyText = notifView.findViewById<TextView>(R.id.recencyText)
        val profilePhotoLayout = notifView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = notifView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = notifView.findViewById<TextView>(R.id.profilePhotoLetter)
        val profileLayout = notifView.findViewById<LinearLayout>(R.id.profileLayout)
        val newLikesNotifCircle = notifView.findViewById<ImageView>(R.id.newLikesNotifCircle)

        if(isNew) newLikesNotifCircle.visibility = View.VISIBLE

        val orderedPostLikes = getOrderedPostLikes(postLikes)
        val mostRecentTimestamp = getMostRecentTimestamp(postLikes)
        val userToDisplay = getStaticUsersMap()[orderedPostLikes[0]]

        notifText.text = getNotifText(orderedPostLikes)
        recencyText.text = getRecencyText(mostRecentTimestamp)

        if(userToDisplay != null) {
            profilePhotoLayout.visibility = View.VISIBLE
            userToDisplay.displayOnView(this, profilePhotoImage, profilePhotoLetter)
        }

        listeners.getScrollViewButtonAnimation().addButton(profileLayout)
        profileLayout.setOnClickListener {
            newLikesNotifCircle.visibility = View.GONE
            openPostLikesPage(post)
        }

        if(isNew)
            newViewTimestampMap[notifView] = mostRecentTimestamp
        else seenViewTimestampMap[notifView] = mostRecentTimestamp
    }

    private fun openPostLikesPage(post: GoalProgress) {
        if(otherProcessStarted()) return
        val intent = Intent(this, UsersDisplayPageActivity::class.java)
        intent.putExtra(USERS_TO_DISPLAY_REF, FOR_POST_LIKES_REF)
        intent.putExtra(INCLUDE_POST_WITH_LIKES_REF, true)
        setStaticProgress(post)
        startActivityForResult(intent, 0)
    }

    private fun getMostRecentTimestamp(postLikes: HashMap<Int, Long>): Long {
        var mostRecentTimestamp: Long = 0
        for(timestamp in postLikes.values.iterator())
            if(timestamp > mostRecentTimestamp)
                mostRecentTimestamp = timestamp
        return mostRecentTimestamp
    }

    private fun getNotifText(orderedPostLikes: ArrayList<Int>): String {
        val users = getStaticUsersMap()
        val firstName = users[orderedPostLikes[0]]?.getName()
        val secondText = when(orderedPostLikes.size) {
            1 -> ""
            2 -> users[orderedPostLikes[1]]?.getName()
            else -> getString(R.string.xOthersString,
                (orderedPostLikes.size - 1).toString())
        }

        if(firstName != null && secondText != null) {
            val prefixText = if(secondText.isEmpty()) firstName
            else getString(R.string.xAndYString, firstName, secondText)
            return getString(R.string.xLikedAPostString, prefixText)
        }
        return ""
    }

    @SuppressLint("SetTextI18n")
    private fun setupToManageBannedUsers() {
        accounts = TotalsAccountEditor(manageBannedUsersPage = this)

        binding.usersDisplayTitleText.text = "Banned users"

        for(userId in getStaticBannedUsers()) {
            val user = getStaticUsersMap()[userId]
            if(user != null) addBannedUserView(user)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addBannedUserView(user: TotalsUser) {
        val bannedUserView = layoutInflater.inflate(R.layout.view_user_follower, null)
        val profilePhotoLayout = bannedUserView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = bannedUserView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = bannedUserView.findViewById<TextView>(R.id.profilePhotoLetter)
        val usernameText = bannedUserView.findViewById<TextView>(R.id.usernameText)
        val nameText = bannedUserView.findViewById<TextView>(R.id.nameText)
        val removeButton = bannedUserView.findViewById<LinearLayout>(R.id.removeButton)
        val removeText = bannedUserView.findViewById<TextView>(R.id.removeText)
        removeText.text = "Unban"

        profilePhotoLayout.visibility = View.VISIBLE

        user.displayOnView(this, profilePhotoImage,
            profilePhotoLetter, usernameText, nameText)

        listeners.getScrollViewButtonAnimation().addButton(removeButton)
        removeButton.setOnClickListener {
            showAccessCodeDialog(user, bannedUserView)
        }

        binding.usersContainer.addView(bannedUserView)
    }

    private fun showAccessCodeDialog(user: TotalsUser, bannedUserView: View) {
        val view = layoutInflater.inflate(R.layout.dialog_access_code, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val accessCodeDialog = builder.create()
        accessCodeDialog.setCancelable(true)

        accessCodeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val accessCodeField = view.findViewById<EditText>(R.id.accessCodeField)

        var firstPartCompleted = false

        fun extendFieldForSecondPart() {
            firstPartCompleted = true
            val filterArray = arrayOfNulls<InputFilter>(1)
            filterArray[0] = InputFilter.LengthFilter(ACCESS_CODE.length)
            accessCodeField.filters = filterArray
        }

        val accessCodeFirstPart = ACCESS_CODE.substring(0, 6)
        accessCodeField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                val enteredCode = accessCodeField.text.toString()
                if (firstPartCompleted && enteredCode == ACCESS_CODE) {
                    accounts.unbanUser(user)
                    beginTransition(binding.usersDisplayPageParent)
                    binding.usersContainer.removeView(bannedUserView)
                    accessCodeDialog.dismiss()
                } else if (enteredCode == accessCodeFirstPart)
                    extendFieldForSecondPart()
            }
        })

        accessCodeField.requestFocus()
        accessCodeDialog.show()
    }

    private fun setupToManageBlockedUsers() {
        val staticUser = getStaticSignedInTotalsUser(this, TinyDB(this))
        if(staticUser == null) { finish(); return }
        signedInUser = staticUser
        accounts = TotalsAccountEditor(manageBlockedUsersPage = this)

        binding.usersDisplayTitleText.text = getString(R.string.blockedUsersTitle)

        for(userId in signedInUser.getBlockedUsers()) {
            val user = getStaticUsersMap()[userId]
            if(user != null) addBlockedUserView(user)
        }
    }

    private fun addBlockedUserView(user: TotalsUser) {
        val blockedUserView = layoutInflater.inflate(R.layout.view_user_follower, null)
        val profilePhotoLayout = blockedUserView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = blockedUserView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = blockedUserView.findViewById<TextView>(R.id.profilePhotoLetter)
        val usernameText = blockedUserView.findViewById<TextView>(R.id.usernameText)
        val nameText = blockedUserView.findViewById<TextView>(R.id.nameText)
        val removeButton = blockedUserView.findViewById<LinearLayout>(R.id.removeButton)
        val removeText = blockedUserView.findViewById<TextView>(R.id.removeText)
        removeText.text = getString(R.string.option_unblock)

        profilePhotoLayout.visibility = View.VISIBLE

        user.displayOnView(this, profilePhotoImage,
            profilePhotoLetter, usernameText, nameText)

        listeners.getScrollViewButtonAnimation().addButton(removeButton)
        removeButton.setOnClickListener {
            showConfirmUnblockUserDialog(user.getUserId(), blockedUserView)
        }

        binding.usersContainer.addView(blockedUserView)
    }

    private fun showConfirmUnblockUserDialog(userId: Int, blockedUserView: View) {
        if(otherProcessStarted()) return
        val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmUnblockUserDialog = builder.create()
        confirmUnblockUserDialog.setCancelable(true)

        confirmUnblockUserDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val promptText = view.findViewById<TextView>(R.id.promptText)
        promptText.text = getString(R.string.confirmUnblockUserPrompt)
        val deleteText = view.findViewById<TextView>(R.id.deleteText)
        deleteText.text = getString(R.string.option_unblock)

        val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        initButtonAnimationListener(deleteButton)
        deleteButton.setOnClickListener {
            showToast(getString(R.string.userUnblockedMessage))
            beginTransition(binding.usersDisplayPageParent)
            binding.usersContainer.removeView(blockedUserView)
            signedInUser.getBlockedUsers().remove(userId)
            accounts.updateBlockedUsers(signedInUser)
            setSignedInTotalsUser(TinyDB(this), signedInUser)
            confirmUnblockUserDialog.cancel()
            if(binding.usersContainer.isEmpty()) finish()
        }

        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmUnblockUserDialog.cancel()
        }

        confirmUnblockUserDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            confirmUnblockUserDialog.dismiss()
        }

        confirmUnblockUserDialog.show()
    }

    private fun setupToManageFollowers() {
        val staticProfile = getStaticProfile()
        if(staticProfile == null) { finish(); return }
        setStaticProfile(null)

        binding.usersDisplayTitleText.text = getString(R.string.profileFollowersTitle)
        binding.usersContainer.visibility = View.GONE

        profiles = ProfileSender(usersDisplayPage = this)
        profile = staticProfile
        val followers = profile.getFollowers()
        for(userId in followers.keys.iterator()) {
            val user = getStaticUsersMap()[userId]
            val following = followers[userId]
            if(user != null && following != null)
                addFollowerView(user, following)
        }
    }

    private fun addFollowerView(user: TotalsUser, following: Boolean = true) {
        val followerView = layoutInflater.inflate(R.layout.view_user_follower, null)
        val profilePhotoLayout = followerView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = followerView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = followerView.findViewById<TextView>(R.id.profilePhotoLetter)
        val usernameText = followerView.findViewById<TextView>(R.id.usernameText)
        val nameText = followerView.findViewById<TextView>(R.id.nameText)
        val removeButton = followerView.findViewById<LinearLayout>(R.id.removeButton)

        profilePhotoLayout.visibility = View.VISIBLE

        user.displayOnView(this, profilePhotoImage,
            profilePhotoLetter, usernameText, nameText)

        listeners.getScrollViewButtonAnimation().addButton(removeButton)
        removeButton.setOnClickListener {
            showConfirmRemoveFollowerDialog(user.getUserId(), followerView, following)
        }

        if(!following) {
            val removeText = followerView.findViewById<TextView>(R.id.removeText)
            removeText.text = getString(R.string.option_uninvite)

            binding.invitedSectionTitle.visibility = View.VISIBLE
            binding.invitedContainer.visibility = View.VISIBLE
            binding.invitedContainer.addView(followerView)
        } else {
            binding.followersSectionTitle.visibility = View.VISIBLE
            binding.followersContainer.visibility = View.VISIBLE
            binding.followersContainer.addView(followerView)
        }
    }

    private fun showConfirmRemoveFollowerDialog(invitedUserId: Int, followerView: View, following: Boolean) {
        if(otherProcessStarted()) return
        val view = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmRemoveFollowerDialog = builder.create()
        confirmRemoveFollowerDialog.setCancelable(true)

        confirmRemoveFollowerDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val promptText = view.findViewById<TextView>(R.id.promptText)
        if(following) promptText.text = getString(R.string.confirmRemoveFollowerPrompt)
        else {
            val deleteText = view.findViewById<TextView>(R.id.deleteText)
            deleteText.text = getString(R.string.option_uninvite)
            promptText.text = getString(R.string.confirmUninviteUserPrompt)
        }

        val deleteButton = view.findViewById<LinearLayout>(R.id.deleteButton)
        initButtonAnimationListener(deleteButton)
        deleteButton.setOnClickListener {
            beginTransition(binding.usersDisplayPageParent)
            if(following) {
                binding.followersContainer.removeView(followerView)
                showToast(getString(R.string.followerRemovedMessage))
            } else {
                binding.invitedContainer.removeView(followerView)
                showToast(getString(R.string.userUninvitedMessage))
            }
            updateFollowerContainersVisibility()
            profiles.respondToInvite(profile, invitedUserId)
            profile.getFollowers().remove(invitedUserId)
            confirmRemoveFollowerDialog.cancel()
        }

        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmRemoveFollowerDialog.cancel()
        }

        confirmRemoveFollowerDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            confirmRemoveFollowerDialog.dismiss()
        }

        confirmRemoveFollowerDialog.show()
    }

    private fun updateFollowerContainersVisibility() {
        var invitedShown = true
        if(binding.invitedContainer.isEmpty()) {
            binding.invitedSectionTitle.visibility = View.GONE
            binding.invitedContainer.visibility = View.GONE
            invitedShown = false
        }

        var followersShown = true
        if(binding.followersContainer.isEmpty()) {
            binding.followersSectionTitle.visibility = View.GONE
            binding.followersContainer.visibility = View.GONE
            followersShown = false
        }

        if(!invitedShown && !followersShown) finish()
    }

    private fun setupToDisplayAllTotals() {
        val percentString = intent.getStringExtra(FOR_PERCENT_TOTALS_REF)
        val allTotalsString = intent.getStringExtra(FOR_ALL_TOTALS_REF)
        val totals = getStaticTotals()
        if(totals == null) { finish(); return }
        setStaticTotals(null)

        val profileLabel = intent.getStringExtra(SELECTED_LABEL_REF)
        binding.usersDisplayTitleText.text = if(profileLabel != null)
            getString(R.string.profileTotalsPageTitle, profileLabel)
        else getString(R.string.totalsSectionTitle)

        val percentTotalsAdded = if(percentString != null && allTotalsString != null) {
            val percentTotalsView = createTotalView(percentString,
                getString(R.string.percentGoalsCompletedLabelString), true)
            val allTotalsView = createTotalView(allTotalsString,
                getString(R.string.totalGoalsCompletedLabelString))
            binding.usersContainer.addView(percentTotalsView)
            binding.usersContainer.addView(allTotalsView)
            true
        } else false

        for(title in totals.keys.iterator()) {
            val amount = totals[title]
            if(amount != null) addTotalViewInOrder(percentTotalsAdded,
                createTotalView(amount.toString(), title), amount)
        }

        val firstView = binding.usersContainer[0]
        val divider = firstView.findViewById<LinearLayout>(R.id.divider)
        divider.visibility = View.GONE
    }

    private fun addTotalViewInOrder(percentTotalsAdded: Boolean, totalView: View, amount: Int) {
        for((index, addedTotalView) in binding.usersContainer.iterator().withIndex()) {
            if(!percentTotalsAdded || index > 1) {
                val addedAmountText = addedTotalView.findViewById<TextView>(R.id.totalAmountText)
                val addedAmount = addedAmountText.text.toString().toInt()
                if(amount > addedAmount) {
                    binding.usersContainer.addView(totalView, index)
                    return
                }
            }
        }
        binding.usersContainer.addView(totalView)
    }

    private fun createTotalView(amountString: String, title: String, forPercent: Boolean = false): View {
        val totalView = layoutInflater.inflate(R.layout.view_total, null)
        val totalAmountText = totalView.findViewById<TextView>(R.id.totalAmountText)
        val totalTitleText = totalView.findViewById<TextView>(R.id.totalTitleText)
        totalAmountText.text = amountString
        totalTitleText.text = title
        if(forPercent) {
            val totalsInfoText = totalView.findViewById<TextView>(R.id.totalsInfoText)
            totalsInfoText.visibility = View.VISIBLE
        }
        return totalView
    }

    private fun setupToDisplayPostLikes() {
        val staticPost = getStaticProgress()
        if(staticPost == null) { finish(); return }
        setStaticProgress(null)

        likedPost = staticPost
        val postLikes = likedPost.getPostLikes()

        binding.usersDisplayTitleText.text = if(postLikes.size == 1)
            getString(R.string.personLikedInfo)
        else getString(R.string.peopleLikedInfo, postLikes.size.toString())

        val signedInUserId = getSignedInUserId(TinyDB(this))
        val orderedPostLikes = getOrderedPostLikes(postLikes)

        for(userId in orderedPostLikes)
            addUserView(userId, signedInUserId == userId)

        val includePost = intent.getBooleanExtra(INCLUDE_POST_WITH_LIKES_REF, false)
        if(includePost) setupToIncludePost()
    }

    private fun setupToIncludePost() {
        val postView = layoutInflater.inflate(R.layout.view_post_progress, null)
        val progressPhotoLayout = postView.findViewById<LinearLayout>(R.id.progressPhotoLayout)
        val progressPhoto = postView.findViewById<ImageView>(R.id.progressPhoto)
        val transparentLayer = postView.findViewById<LinearLayout>(R.id.transparentLayer)
        val goalAmountText = postView.findViewById<TextView>(R.id.goalAmountText)
        val goalTitleText = postView.findViewById<TextView>(R.id.goalTitleText)
        val dateText = postView.findViewById<TextView>(R.id.dateText)
        val postDetailsText = postView.findViewById<TextView>(R.id.postDetailsText)
        val clickableLayout = postView.findViewById<LinearLayout>(R.id.clickableLayout)
        val captionText = postView.findViewById<TextView>(R.id.captionText)
        val likeButton = postView.findViewById<ImageView>(R.id.likeButton)
        val moreButton = postView.findViewById<ImageView>(R.id.moreButton)

        progressPhotoLayout.visibility = View.VISIBLE
        if(likedPost.photoLoaded()) progressPhoto.setImageBitmap(likedPost.getBitmapPhoto())
        else likedPost.retrieveBitmap(this, getStaticHomePage()?.getPhotos(), progressPhoto)
        transparentLayer.visibility = View.VISIBLE
        val amount = likedPost.getAmount()
        if(amount == 0) goalAmountText.visibility = View.GONE
        else goalAmountText.text = amount.toString()
        goalTitleText.text = likedPost.getTitle()
        if(likedPost.hasCaption()) captionText.text = likedPost.getCaption()
        else captionText.visibility = View.GONE
        dateText.text = getDateTextString(likedPost, true, likedPost.getDueDate())
        postDetailsText.text = getPostedTextString(likedPost)

        moreButton.visibility = View.GONE
        likeButton.visibility = View.GONE

        clickableLayout.setOnClickListener{
            if(likedPost.photoLoaded()) openViewPhotoPage()
        }

        binding.postContainer.visibility = View.VISIBLE
        binding.postContainer.addView(postView)
    }

    private fun openViewPhotoPage() {
        if(otherProcessStarted()) return
        val signedInUser = getStaticSignedInTotalsUser(this, TinyDB(this))
        setBitmapPhotoToView(likedPost.getBitmapPhoto())
        setStaticTotalsUser(signedInUser)
        val intent = Intent(this, ViewPhotoPageActivity::class.java)
        intent.putExtra(PHOTO_DETAILS_REF, likedPost.getPhotoDetails())
        startActivity(intent)
    }

    private fun getOrderedPostLikes(postLikes: HashMap<Int, Long>): ArrayList<Int> {
        val orderedPostLikes = ArrayList<Int>()
        for(userId in postLikes.keys.iterator()) {
            var userAdded = false
            val timeLiked = postLikes[userId]
            if(timeLiked != null) {
                val dateLiked = getCalendar(timeLiked)
                for((i, addedUserId) in orderedPostLikes.withIndex()) {
                    val addedTimeLiked = postLikes[addedUserId]
                    if(addedTimeLiked != null) {
                        val addedDateLiked = getCalendar(addedTimeLiked)
                        if(dateLiked.comesAfter(addedDateLiked)) {
                            orderedPostLikes.add(i, userId)
                            userAdded = true
                            break
                        }
                    }
                }
                if(!userAdded) orderedPostLikes.add(userId)
            }
        }
        return orderedPostLikes
    }

    private fun addUserView(userId: Int, isSignedInUser: Boolean) {
        val user = getStaticUsersMap()[userId]?: return
        val userView = layoutInflater.inflate(R.layout.view_user_searched, null)
        val profilePhotoLayout = userView.findViewById<LinearLayout>(R.id.profilePhotoLayout)
        val profilePhotoImage = userView.findViewById<ImageView>(R.id.profilePhotoImage)
        val profilePhotoLetter = userView.findViewById<TextView>(R.id.profilePhotoLetter)
        val usernameText = userView.findViewById<TextView>(R.id.usernameText)
        val nameText = userView.findViewById<TextView>(R.id.nameText)
        val checkbox = userView.findViewById<ImageView>(R.id.checkbox)
        checkbox.visibility = View.GONE
        profilePhotoLayout.visibility = View.VISIBLE
        user.displayOnView(this, profilePhotoImage,
            profilePhotoLetter, usernameText, nameText)

        if (isSignedInUser)
            binding.usersContainer.addView(userView, 0)
        else binding.usersContainer.addView(userView)
    }

    private fun initBinding() {
        binding = ActivityUsersDisplayPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    fun getBinding(): ActivityUsersDisplayPageBinding {
        return binding
    }

    fun otherProcessStarted(setTrue: Boolean = true): Boolean {
        return if(otherProcessStarted) true else {
            if(setTrue) setOtherProcessStarted()
            false
        }
    }

    fun setOtherProcessStarted(otherProcessStarted: Boolean = true) {
        this.otherProcessStarted = otherProcessStarted
    }
}