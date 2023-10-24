package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import java.util.*
import kotlin.collections.ArrayList

class ProgressPageView(private val activity: ProgressPageActivity) {

    private val binding = activity.getBinding()

    init {
        binding.progressPageTitle.text = activity.getString(
            R.string.progressPageTitle, activity.getPostsLabel())
        binding.progressPageUserText.text = activity.getString(
            R.string.progressPageTitleDetails, activity.getPostsUsersName())
        displayNextBatchOfPosts(FIRST_BATCH_SIZE)
        displayTotals()
        if(!activity.userIsProfileOwner())
            setToVisitorState()
        updateEmptyPageMessageVisibility()
    }

    private fun updateEmptyPageMessageVisibility() {
        binding.emptyProfilePageMessageLayout.visibility =
            if(binding.postsContainer.isEmpty())
                View.VISIBLE else View.GONE
    }

    private fun setToVisitorState() {
        binding.sendButton.visibility = View.GONE
        binding.moreButton.visibility = View.GONE
//        binding.notificationStatusLayout.visibility = View.VISIBLE
    }

    fun updateTotals() {
        binding.totalsSectionTitle.visibility = View.GONE
        binding.percentGoalsCompletedLayout.visibility = View.GONE
        binding.totalGoalsCompletedLayout.visibility = View.GONE
        binding.mostCompletedGoalLayout.visibility = View.GONE
        binding.nextMostCompletedGoalLayout.visibility = View.GONE
        binding.lastMostCompletedGoalLayout.visibility = View.GONE
        binding.seeAllTotalsLayout.visibility = View.GONE
        activity.getPostTitleCounter().clear()
        displayTotals()
    }

    private fun displayTotals() {
        val posts = activity.getPosts().ifEmpty { return }
        displayTotalGoalCounts(posts)
        displayGoalTitleCounts()
    }

    @SuppressLint("SetTextI18n")
    private fun displayTotalGoalCounts(posts: List<GoalProgress>) {
        val totalsStartDate = activity.getProfile().getTotalsStartDate()
        var finalTotalForPercent = 0.0
        var finalIncompleteForPercent = 0.0
        var totalLastPosts = 0.0
        var incompleteCounter = 0.0
        for(post in posts.reversed()) {
            val datePosted = getCalendar(post.getTimestampPosted())
            if(totalsStartDate == null || datePosted.comesAfter(totalsStartDate, true)) {
                if (!post.isNotRequired()) {
                    if (post.isLastPostOfDueDate())
                        totalLastPosts++
                    if (post.isIncomplete())
                        incompleteCounter++
                    else {
                        val titleCount = activity.getPostTitleCounter()[post.getTitle()] ?: 0
                        val amount = if (post.getAmount() > 0) post.getAmount() else 1
                        activity.getPostTitleCounter()[post.getTitle()] = titleCount + amount
                    }
                    if (finalTotalForPercent == 0.0 && totalLastPosts == 100.0) {
                        finalTotalForPercent = 100.0
                        finalIncompleteForPercent = incompleteCounter
                    }
                }
            }
        }

        if(finalTotalForPercent == 0.0) {
            finalTotalForPercent = totalLastPosts
            finalIncompleteForPercent = incompleteCounter
        }

        if(totalLastPosts > 1) {
            val finalCompletedForPercent = finalTotalForPercent - finalIncompleteForPercent
            val percentDouble = (finalCompletedForPercent / finalTotalForPercent) * 100
            val percent = percentDouble.toInt()
            val totalCompleted = totalLastPosts - incompleteCounter
            binding.totalsSectionTitle.visibility = View.VISIBLE
            binding.totalsContainer.visibility = View.VISIBLE
            binding.percentGoalsCompletedLayout.visibility = View.VISIBLE
            binding.totalGoalsCompletedLayout.visibility = View.VISIBLE
            binding.percentGoalsCompletedText.text = "$percent%"
            binding.totalGoalsCompletedText.text = totalCompleted.toInt().toString()
        }
    }

    private fun displayGoalTitleCounts() {
        val titleCounter = activity.getPostTitleCounter()
        val titleOrder = ArrayList<String>()
        for(title in titleCounter.keys.reversed().iterator()) {
            var titleAdded = false
            val titleCount = titleCounter[title]
            if(titleCount != null) {
                for((i, addedTitle) in titleOrder.withIndex()) {
                    val addedTitleCount = titleCounter[addedTitle]
                    if(addedTitleCount != null && titleCount > addedTitleCount) {
                        titleOrder.add(i, title)
                        titleAdded = true
                        break
                    }
                }
            }
            if(!titleAdded) titleOrder.add(title)
        }

        fun displayCompletedGoalLayout(title: String,
                                       completedGoalLayout: LinearLayout,
                                       completedGoalAmountText: TextView,
                                       completedGoalTitleText: TextView) {
            val titleCount = titleCounter[title]
            if(titleCount != null) {
                binding.totalsSectionTitle.visibility = View.VISIBLE
                binding.totalsContainer.visibility = View.VISIBLE
                completedGoalLayout.visibility = View.VISIBLE
                completedGoalAmountText.text = titleCount.toString()
                completedGoalTitleText.text =
                    activity.getString(R.string.goalTitleCompletedLabelString, title)
            }
        }

        for((i, title) in titleOrder.withIndex()) {
            if(binding.percentGoalsCompletedLayout.isVisible) {
                when(i) {
                    0 -> displayCompletedGoalLayout(title,
                        binding.mostCompletedGoalLayout,
                        binding.mostCompletedGoalAmountText,
                        binding.mostCompletedGoalTitleText)
                    else -> {
                        binding.seeAllTotalsLayout.visibility = View.VISIBLE
                        break
                    }
                }
            } else {
                when(i) {
                    0 -> displayCompletedGoalLayout(title,
                        binding.mostCompletedGoalLayout,
                        binding.mostCompletedGoalAmountText,
                        binding.mostCompletedGoalTitleText)
                    1 -> displayCompletedGoalLayout(title,
                        binding.nextMostCompletedGoalLayout,
                        binding.nextMostCompletedGoalAmountText,
                        binding.nextMostCompletedGoalTitleText)
                    2 -> displayCompletedGoalLayout(title,
                        binding.lastMostCompletedGoalLayout,
                        binding.lastMostCompletedGoalAmountText,
                        binding.lastMostCompletedGoalTitleText)
                    else -> {
                        binding.seeAllTotalsLayout.visibility = View.VISIBLE
                        break
                    }
                }
            }
        }
    }

    fun displayNextBatchOfPosts(batchSize: Int = NEXT_BATCH_SIZE) {
        val posts = activity.getPosts().ifEmpty { return }
        val startIndex = activity.getPostsLoaded()
        if(posts.lastIndex < startIndex) return
        activity.setLoadingPosts(true)
        var endIndex = startIndex + batchSize - 1
        if(posts.lastIndex < endIndex) endIndex = posts.lastIndex
        activity.updatePostsLoaded(endIndex + 1)
        displayPosts(posts, startIndex, endIndex)
        activity.setLoadingPosts(false)
    }

    private fun displayPosts(posts: ArrayList<GoalProgress>, startIndex: Int, endIndex: Int) {
        for(i in startIndex..endIndex) {
            val post = posts[i]
            if(post.isIncomplete())
                addIncompletePostView(post) else addPostView(post)
            if(activity.userIsProfileOwner() && post.isFromDeletedReportedPost()) {
                val noticeSeen = post.deletedPostNoticeSeen()?: true
                if(!noticeSeen) activity.getDialogs().showDeletedPostNoticeDialog(post)
            }
        }
    }

    fun addIncompletePostView(post: GoalProgress, addToTop: Boolean = false) {
        binding.postsSectionTitle.visibility = View.VISIBLE
        binding.postsContainer.visibility = View.VISIBLE
        val postView = activity.layoutInflater.inflate(R.layout.view_post_incomplete, null)
        val postLayout = postView.findViewById<LinearLayout>(R.id.postLayout)
        val goalTitleText = postView.findViewById<TextView>(R.id.goalTitleText)
        val incompleteForDateText = postView.findViewById<TextView>(R.id.incompleteForDateText)
        val captionText = postView.findViewById<TextView>(R.id.captionText)
        val moreButton = postView.findViewById<ImageView>(R.id.moreButton)
        val incompleteIcon = postView.findViewById<ImageView>(R.id.incompleteIcon)

        activity.getViewPostMap()[postView] = post

        postLayout.visibility = View.VISIBLE

        val amount = post.getAmount()
        val titleString = if(amount > 0)
            "$amount ${post.getTitle()}"
        else post.getTitle()

        val hour = post.getHour()
        val minute = post.getMinute()
        goalTitleText.text = activity.getString(R.string.titleAndTimeDueString,
            titleString, getTimeText(hour, minute))

        val dueDate = post.getDueDate()
        if(dueDate != null) {
            incompleteForDateText.text = if (post.isNotRequired())
                activity.getString(R.string.notRequiredDetailsString, dueDate.toDateString())
            else activity.getString(R.string.incompleteDetailsString, dueDate.toDateString())
        } else incompleteForDateText.visibility = View.GONE
        if(post.hasCaption()) captionText.text = post.getCaption()
        else captionText.visibility = View.GONE

        if(post.isNotRequired())
            incompleteIcon.setImageResource(R.drawable.ic_cancelled)

        moreButton.setOnClickListener {
            val progress = activity.getViewPostMap()[postView]
            if(progress != null)
                activity.getDialogs().showIncompletePostMoreOptionsDialog(progress, postView)
        }

        if(addToTop) binding.postsContainer.addView(postView, 0)
        else binding.postsContainer.addView(postView)
    }

    private fun addPostView(post: GoalProgress) {
        binding.postsSectionTitle.visibility = View.VISIBLE
        binding.postsContainer.visibility = View.VISIBLE
        val postView = activity.layoutInflater.inflate(R.layout.view_post_progress, null)
        val progressPhotoLayout = postView.findViewById<LinearLayout>(R.id.progressPhotoLayout)
        val progressPhoto = postView.findViewById<ImageView>(R.id.progressPhoto)
        val transparentLayer = postView.findViewById<LinearLayout>(R.id.transparentLayer)
        val goalAmountText = postView.findViewById<TextView>(R.id.goalAmountText)
        val goalTitleText = postView.findViewById<TextView>(R.id.goalTitleText)
        val dateText = postView.findViewById<TextView>(R.id.dateText)
        val postDetailsText = postView.findViewById<TextView>(R.id.postDetailsText)
        val heartImageLayout = postView.findViewById<LinearLayout>(R.id.heartImageLayout)
        val heartImage = postView.findViewById<ImageView>(R.id.heartImage)
        val clickableLayout = postView.findViewById<LinearLayout>(R.id.clickableLayout)
        val captionText = postView.findViewById<TextView>(R.id.captionText)
        val likeButton = postView.findViewById<ImageView>(R.id.likeButton)
        val moreButton = postView.findViewById<ImageView>(R.id.moreButton)

        activity.getViewPostMap()[postView] = post

        progressPhotoLayout.visibility = View.VISIBLE
        if(post.photoLoaded()) progressPhoto.setImageBitmap(post.getBitmapPhoto())
        else post.retrieveBitmap(activity, activity.getPhotos(), progressPhoto)
        transparentLayer.visibility = View.VISIBLE
        val amount = post.getAmount()
        if(amount == 0) goalAmountText.visibility = View.GONE
        else goalAmountText.text = amount.toString()
        goalTitleText.text = post.getTitle()
        if(post.hasCaption()) captionText.text = post.getCaption()
        else captionText.visibility = View.GONE
        dateText.text = activity.getDateTextString(post, true, post.getDueDate())
        postDetailsText.text = activity.getPostedTextString(post)

        moreButton.setOnClickListener {
            val progress = activity.getViewPostMap()[postView]
            if(progress != null)
                activity.getDialogs().showPostMoreOptionsDialog(progress, postView)
        }

        val userId = getSignedInUserId(activity.getDatabase())
        if(post.postLiked(userId)) likeButton.setImageResource(R.drawable.ic_liked)

        fun toggleLikeButton() {
            if(!activity.withinRequestsLimit()) return
            val progress = activity.getViewPostMap()[postView]?: return

            if(progress.postLiked(userId)) {
                activity.getViewPostMap()[postView]?.unlikePost(userId)
                likeButton.setImageResource(R.drawable.ic_like)
                activity.getUploader().updatePostLike(progress, userId, false)
            } else {
                activity.getViewPostMap()[postView]?.likePost(userId)
                likeButton.setImageResource(R.drawable.ic_liked)
                activity.getUploader().updatePostLike(progress, userId)
            }
        }

        clickableLayout.setOnClickListener(object : DoubleClickListener() {

            private var heartImageInstance = 0
            private var doubleClickTimer = Timer()

            fun animateHeartImage() {
                heartImageInstance++
                if(heartImageInstance > 100)
                    heartImageInstance = 0


                val currentInstance = heartImageInstance

                fun hideHeartImage() {
                    Handler().postDelayed({
                        if(currentInstance == heartImageInstance
                            && heartImage.visibility != View.GONE) {
                            beginTransition(heartImageLayout)
                            heartImage.visibility = View.GONE
                            hideHeartImage()
                        }
                    }, HEART_IMAGE_SHOWN_DELAY)
                }

                Handler().post {
                    heartImage.visibility = View.GONE
                    beginTransition(heartImageLayout)
                    heartImage.visibility = View.VISIBLE
                    hideHeartImage()
                }
            }

            override fun onSingleClick(v: View) {
                doubleClickTimer.cancel()
                doubleClickTimer = Timer()
                doubleClickTimer.schedule(object: TimerTask() { override fun run() {
                    if(post.photoLoaded()) activity.openViewPhotoPage(post) } }, DOUBLE_TAP_DELAY)
            }

            override fun onDoubleClick(v: View) {
                doubleClickTimer.cancel()
                animateHeartImage()
                val progress = activity.getViewPostMap()[postView]?: return
                if(!progress.postLiked(userId)) toggleLikeButton()
            }
        })

        likeButton.setOnClickListener {
            toggleLikeButton()
        }

        binding.postsContainer.addView(postView)
    }

    abstract inner class DoubleClickListener : View.OnClickListener {

        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            val clickTime = System.currentTimeMillis()
            if (clickTime - lastClickTime < DOUBLE_TAP_DELAY)
                onDoubleClick(v) else onSingleClick(v)

            lastClickTime = clickTime
        }

        abstract fun onSingleClick(v: View)
        abstract fun onDoubleClick(v: View)
    }

}