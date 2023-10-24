package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.size
import com.justanotherdeveloper.totalslite.databinding.ActivityProgressPageBinding
import java.util.Calendar

class ProgressPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressPageBinding

    private lateinit var local: TinyDB

    private lateinit var uploader: GoalUploader
    private lateinit var profiles: ProfileSender

    private lateinit var listeners: ProgressPageListeners
    private lateinit var view: ProgressPageView
    private lateinit var dialogs: ProgressPageDialogs

    private lateinit var profile: Profile
    private lateinit var posts: ArrayList<GoalProgress>

    private var postsLoaded = 0
    private var loadingPosts = false

    private val viewPostMap = HashMap<View, GoalProgress>()
    private val postTitleCounter = HashMap<String, Int>()

    private var selectedPostView: View? = null
    private var doSlideTransition = true

    private var otherProcessStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)

        val user = getStaticProgressPageUser()
        val label = intent.getStringExtra(SELECTED_LABEL_REF)?: ""
        if(user == null || label.isEmpty()) { finish(); return }
        setStaticProgressPageUser(null)

        profile = Profile(user, label)
        val followers = getStaticProfileFollowers(label)
        if(followers != null) profile.setFollowers(followers)
        val totalsStartDate = getStaticTotalsStartDate(user.getUserId(), label)
        if(totalsStartDate != null) profile.setTotalsStartDate(totalsStartDate)

        val postsList = getLabeledPosts(profile)?.reversed()
        posts = if(postsList != null) ArrayList(postsList) else ArrayList()

        uploader = GoalUploader(profilePage = this)
        profiles = ProfileSender(profilePage = this)

        dialogs = ProgressPageDialogs(this)
        listeners = ProgressPageListeners(this)
        view = ProgressPageView(this)
    }

    fun searchUsersToSendProfile() {
        if(otherProcessStarted()) return
        val intent = Intent(this, SendProfileActivity::class.java)
        setStaticProfile(profile)
        startActivityForResult(intent, 0)
    }

    fun startGoalLikePost(post: GoalProgress) {
        if(otherProcessStarted()) return
        doSlideTransition = false
        val postToCopy = post.createProgressClone()
        postToCopy.getLabels().clear()
        postToCopy.addLabel(profile.getLabel())
        setStaticProgress(postToCopy)
        val intentData = Intent()
        intentData.putExtra(START_GOAL_LIKE_POST_REF, true)
        setResult(Activity.RESULT_OK, intentData)
        finish()
    }

    fun openManageFollowersPage() {
        if(otherProcessStarted()) return
        val intent = Intent(this, UsersDisplayPageActivity::class.java)
        intent.putExtra(USERS_TO_DISPLAY_REF, FOR_MANAGE_FOLLOWERS_REF)
        setStaticProfile(profile)
        startActivityForResult(intent, 0)
    }

    fun openAllTotalsPage() {
        if(otherProcessStarted()) return
        val intent = Intent(this, UsersDisplayPageActivity::class.java)
        intent.putExtra(SELECTED_LABEL_REF, profile.getLabel())
        intent.putExtra(USERS_TO_DISPLAY_REF, FOR_ALL_TOTALS_REF)
        setStaticTotals(postTitleCounter)
        if(binding.percentGoalsCompletedLayout.isVisible
            && binding.totalGoalsCompletedLayout.isVisible) {
            intent.putExtra(FOR_PERCENT_TOTALS_REF,
                binding.percentGoalsCompletedText.text.toString())
            intent.putExtra(FOR_ALL_TOTALS_REF,
                binding.totalGoalsCompletedText.text.toString())
        }
        startActivity(intent)
    }

    fun seePostLikes(post: GoalProgress) {
        if(otherProcessStarted()) return
        val intent = Intent(this, UsersDisplayPageActivity::class.java)
        intent.putExtra(USERS_TO_DISPLAY_REF, FOR_POST_LIKES_REF)
        setStaticProgress(post)
        startActivity(intent)
    }

    fun seeAttachments(post: GoalProgress, postView: View) {
        if(otherProcessStarted()) return
        val intent = Intent(this, SeeAttachmentsActivity::class.java)
        setStaticProgress(post)
        if(userIsProfileOwner()) {
            intent.putExtra(UPDATE_FIREBASE_REF, true)
            intent.putExtra(MANAGE_ATTACHMENTS_REF, true)
            selectedPostView = postView
        }
        startActivityForResult(intent, 0)
    }

    fun manageLabels(post: GoalProgress, postView: View) {
        if(otherProcessStarted()) return
        val intent = Intent(this, SelectLabelActivity::class.java)
        setStaticProgress(post)
        intent.putExtra(TO_UPDATE_POST_REF, true)
        intent.putExtra(UPDATE_FIREBASE_REF, true)
        selectedPostView = postView
        startActivityForResult(intent, 0)
    }

    fun deletePost(post: GoalProgress, postView: View,
                   deleteButton: LinearLayout, disabledButton: LinearLayout,
                   cancelButton: LinearLayout, confirmDeletePostDialog: AlertDialog) {
        selectedPostView = postView
        uploader.deletePost(post, deleteButton, disabledButton,
            cancelButton, confirmDeletePostDialog)
    }

    fun updateWithDeletedPost() {
        val postView = selectedPostView
        selectedPostView = null
        if(postView != null)
            updateProgressPageWithRemovedPost(postView)
    }

    fun postIncompleteGoal(originalPost: GoalProgress,
                           incompletePost: GoalProgress, postView: View,
                           confirmMarkAsIncompleteDialog: AlertDialog,
                           disabledButton: LinearLayout,
                           markIncompleteButton: LinearLayout,
                           cancelButton: LinearLayout) {
        selectedPostView = postView
        uploader.postIncompleteGoal(incompletePost,
            confirmMarkAsIncompleteDialog, disabledButton,
            markIncompleteButton, cancelButton, originalPost)
    }

    fun updateWithIncompletePost(incompletePost: GoalProgress) {
        val postView = selectedPostView
        selectedPostView = null
        if(postView != null) {
            updateProgressPageWithRemovedPost(postView)
            view.addIncompletePostView(incompletePost, true)
            val reversedPosts = ArrayList(posts.reversed())
            reversedPosts.add(incompletePost)
            posts = ArrayList(reversedPosts.reversed())
            postsLoaded++
        }
    }

    fun openViewPhotoPage(post: GoalProgress) {
        if(otherProcessStarted()) return
        setBitmapPhotoToView(post.getBitmapPhoto())
        setStaticTotalsUser(profile.getUser())
        val intent = Intent(this, ViewPhotoPageActivity::class.java)
        intent.putExtra(PHOTO_DETAILS_REF, post.getPhotoDetails())
        startActivity(intent)
    }

    fun confirmResetTotalsSuccess(totalsStartDate: Calendar?) {
        showToast(getString(R.string.totalsUpdatedMessage))
        profile.setTotalsStartDate(totalsStartDate)
        view.updateTotals()
    }

    fun confirmPostReported(post: GoalProgress, user: TotalsUser) {
        sendReportedPostDetailsEmail(post, user)
        showToast(getString(R.string.postReportedMessage))
    }

    private fun sendReportedPostDetailsEmail(post: GoalProgress, user: TotalsUser) {
        val amount = post.getAmount()
        val postTitle = if(amount > 0)
            "$amount ${post.getTitle()}" else post.getTitle()
        val subject = "Post Reported: $postTitle by ${user.getUsername()}"
        val message = getEmailReportedPostMessage(post, user)
        ReportedPostsDataUploader().uploadReportedPostData(this, subject, message)
    }

    private fun updateProgressPageWithRemovedPost(postView: View) {
        val post = viewPostMap[postView]
        if(post != null) {
            posts.remove(post)
            postsLoaded--
        }
        view.updateTotals()
        beginTransition(binding.progressPageParent)
        binding.postsContainer.removeView(postView)
        if(binding.postsContainer.size == 0) {
            binding.postsContainer.visibility = View.GONE
            binding.postsSectionTitle.visibility = View.GONE
        }
        viewPostMap.remove(postView)
    }

    /** Syntax to init binding */
    private fun initBinding() {
        binding = ActivityProgressPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /** Override Methods */

    override fun onResume() {
        super.onResume()
        setOtherProcessStarted(false)
    }

    override fun finish() {
        super.finish()
        if(doSlideTransition)
            slideTransitionLeftIfSignedIn(local)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setOtherProcessStarted(false)
        val toUpdatePost = data?.getBooleanExtra(TO_UPDATE_POST_REF, false)?: false
        if(toUpdatePost) {
            val postView = selectedPostView
            val post = getStaticProgress()
            selectedPostView = null
            setStaticProgress(null)
            if(postView != null && post != null) {
                if(post.getLabels().contains(profile.getLabel()))
                    viewPostMap[postView] = post
                else updateProgressPageWithRemovedPost(postView)
            }
        }
    }

    /** "Get" Methods */

    fun getBinding(): ActivityProgressPageBinding {
        return binding
    }

    fun getPhotos(): PhotoDatabase? {
        return getStaticHomePage()?.getPhotos()
    }

    fun getDatabase(): TinyDB {
        return local
    }

    fun getDialogs(): ProgressPageDialogs {
        return dialogs
    }

    fun getView(): ProgressPageView {
        return view
    }

    fun getViewPostMap(): HashMap<View, GoalProgress> {
        return viewPostMap
    }

    fun getUploader(): GoalUploader {
        return uploader
    }

    fun getProfiles(): ProfileSender {
        return profiles
    }

    fun getPosts(): ArrayList<GoalProgress> {
        return posts
    }

    fun getPostTitleCounter(): HashMap<String, Int> {
        return postTitleCounter
    }

    fun getProfile(): Profile {
        return profile
    }

    fun getPostsLabel(): String {
        return profile.getLabel()
    }

    fun getPostsUser(): TotalsUser {
        return profile.getUser()
    }

    fun getPostsUsersName(): String {
        return profile.getUser().getName()
    }

    fun getPostsLoaded(): Int {
        return postsLoaded
    }

    fun updatePostsLoaded(postsLoaded: Int) {
        this.postsLoaded = postsLoaded
    }

    fun loadingPosts(): Boolean {
        return loadingPosts
    }

    fun setLoadingPosts(loadingPosts: Boolean = true) {
        this.loadingPosts = loadingPosts
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

    fun userIsProfileOwner(): Boolean {
        return getSignedInUserId(local) == profile.getUser().getUserId()
    }

    fun postingInProgress(): Boolean {
        return uploader.postingInProgress()
    }
}