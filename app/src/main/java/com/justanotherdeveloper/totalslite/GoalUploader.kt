package com.justanotherdeveloper.totalslite

import android.graphics.Bitmap
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class GoalUploader(private val editGoalPage: EditActivityPageActivity? = null,
                   private val createPostPage: CreatePostPageActivity? = null,
                   private val profilePage: ProgressPageActivity? = null,
                   private val seeAttachmentsPage: SeeAttachmentsActivity? = null,
                   private val selectLabelPage: SelectLabelActivity? = null,
                   private val goalPage: MePageFragment? = null,
                   private val postLikesNotifsPage: UsersDisplayPageActivity? = null,
                   private val profileXPage: ProgressPageXActivity? = null) {

    private val storage = FirebaseStorage.getInstance().getReference(PHOTOS_LOCATION)

    private val uploader = this
    private var postingInProgress = false

    fun postingInProgress(): Boolean {
        return postingInProgress
    }

    fun checkForGoalUpdate() {
        if(postingInProgress) return
        val editGoalPage = this.editGoalPage?: return
        val original = editGoalPage.getOriginalGoal()
        val goal = editGoalPage.getGoal()

        if(goal.changedFrom(original)) {
            postingInProgress = true
            val fb = FirebaseDatabase.getInstance().reference
            val goalsTable = fb.child(GOALS_PATH)
            val goalReference = goalsTable.child(goal.getGoalFirebaseKey())
            updateGoal(goal, goalReference, original)
        } else editGoalPage.confirmGoalUpdatedSuccess(false)
    }

    fun setGoalActive(goal: Goal, isActive: Boolean = true) {
        val fb = FirebaseDatabase.getInstance().reference
        val goalsTable = fb.child(GOALS_PATH)
        val goalReference = goalsTable.child(goal.getGoalFirebaseKey())
        val goalData = HashMap<String, Any?>()
        goalData[IS_ACTIVE_PATH] = isActive
        if(isActive) goalData[ACTIVE_START_TIMESTAMP_PATH] = goal.getActiveDate()
        goalReference.updateChildren(goalData)
    }

    private fun updateGoal(goal: Goal, goalReference: DatabaseReference,
                           original: Goal) {
        val editGoalPage = this.editGoalPage?: return
        val newGoalData = goal.toGoalDataMap(original = original)?: return
        goalReference.updateChildren(newGoalData).addOnSuccessListener {
            postingInProgress = false
            editGoalPage.confirmGoalUpdatedSuccess()
        }.addOnFailureListener {
            postingInProgress = false
            editGoalPage.getView().notifyPostFailure()
        }
    }

    fun setDeletedPostNoticeSeen(post: GoalProgress) {
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        postReference.child(DELETED_POST_NOTICE_SEEN_PATH).setValue(true)
    }

    fun reportPost(post: GoalProgress, user: TotalsUser,
                   reportButton: LinearLayout, cancelButton: LinearLayout,
                   disabledButton: LinearLayout, reportDialog: AlertDialog) {
        val reportedMap = HashMap<String, Any>()
        reportedMap[REPORTED_PATH] = true
        reportedMap[REASON_REPORTED_PATH] = post.getReasonReported()

        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        postReference.updateChildren(reportedMap).addOnSuccessListener {
            profilePage?.confirmPostReported(post, user)
            reportDialog.setCancelable(true)
            reportDialog.dismiss()
        }.addOnFailureListener {
            reportButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            disabledButton.visibility = View.GONE
            reportDialog.setCancelable(true)
            profilePage?.showRequestUnavailableToast()
        }
    }

    fun addGoal() {
        if(postingInProgress) return
        val editGoalPage = this.editGoalPage?: return
        val goal = editGoalPage.getGoal()
        val newGoalData = goal.toGoalDataMap(true)?: return
        postingInProgress = true
        val fb = FirebaseDatabase.getInstance().reference
        val goalsTable = fb.child(GOALS_PATH)
        val goalReference = goalsTable.push()
        goalReference.updateChildren(newGoalData).addOnSuccessListener {
            editGoalPage.getGoal().setGoalFirebaseKey(goalReference.key.toString())
            postingInProgress = false
            editGoalPage.confirmGoalAddedSuccess()
        }.addOnFailureListener {
            postingInProgress = false
            editGoalPage.getView().notifyPostFailure()
        }
    }

    fun postProgress() {
        if(postingInProgress) return
        val createPostPage = this.createPostPage?: return
        val bitmapPhoto = createPostPage.getGoalProgress().getBitmapPhoto()?: return
        postingInProgress = true
        val fileReference = storage.child(getTimeStamp().toString())
        val stream = ByteArrayOutputStream()
        bitmapPhoto.compress(Bitmap.CompressFormat.JPEG, 25, stream)
        val data = stream.toByteArray()
        val uploadTask = fileReference.putBytes(data)
        uploadTask.addOnSuccessListener {
            val result = it.storage.downloadUrl
            result.addOnSuccessListener { uri ->
                createPostPage.getGoalProgress().setBitmapPhotoUrl(uri.toString())
                postProgressData()
            }.addOnFailureListener { _ ->
                postingInProgress = false
                createPostPage.getView().notifyPostFailure()
            }
        }.addOnFailureListener {
            postingInProgress = false
            createPostPage.getView().notifyPostFailure()
        }
    }

    fun postIncompleteGoal(incompletePost: GoalProgress?,
                           confirmMarkAsIncompleteDialog: AlertDialog? = null,
                           disabledButton: LinearLayout? = null,
                           markIncompleteButton: LinearLayout? = null,
                           cancelButton: LinearLayout? = null,
                           originalPost: GoalProgress? = null,
                           originalPostView: View? = null,
                           resolvedReportMap: HashMap<String, Any?>? = null,
                           deletePhoto: Boolean = true,
                           goalView: View? = null, updatedGoal: Goal? = null) {
        if(postingInProgress) return
        if(incompletePost == null) return
        val newPostData = incompletePost.toProgressDataMap(resolvedReportMap)
        postingInProgress = true
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.push()
        postReference.updateChildren(newPostData).addOnSuccessListener {
            if(goalPage != null) {
                confirmMarkAsIncompleteDialog?.setCancelable(true)
                confirmMarkAsIncompleteDialog?.dismiss()
                goalPage.getIncompletePost()?.setPostFirebaseKey(postReference.key.toString())
                if(goalView != null && updatedGoal != null) {
                    goalPage.getFragmentView().updateGoal(goalView, updatedGoal)
                    setGoalActive(updatedGoal)
                }
                goalPage.refreshGoalProgressDetails()
                goalPage.setPostingFinished()
            } else if(profilePage != null || profileXPage != null) {
                incompletePost.setPostFirebaseKey(postReference.key.toString())
                deleteOriginalPost(incompletePost, originalPost,
                    confirmMarkAsIncompleteDialog, disabledButton,
                    markIncompleteButton, cancelButton, deletePhoto, originalPostView)
            }
            postingInProgress = false
        }.addOnFailureListener {
            confirmMarkAsIncompleteDialog?.setCancelable(true)
            disabledButton?.visibility = View.GONE
            markIncompleteButton?.visibility = View.VISIBLE
            cancelButton?.visibility = View.VISIBLE
            goalPage?.setPostingFinished()
            goalPage?.activity?.showRequestUnavailableToast()
            profilePage?.showRequestUnavailableToast()
            profileXPage?.showRequestUnavailableToast()
            postingInProgress = false
        }
    }

    fun deletePost(post: GoalProgress, deleteButton: LinearLayout, disabledButton: LinearLayout,
                   cancelButton: LinearLayout, confirmDeletePostDialog: AlertDialog) {

        fun cancelPosting() {
            confirmDeletePostDialog.setCancelable(true)
            disabledButton.visibility = View.GONE
            deleteButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            profilePage?.showRequestUnavailableToast()
        }

        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        postsTable.child(post.getPostFirebaseKey()).setValue(null)
            .addOnSuccessListener {
                if(post.getBitmapPhotoUrl().isNotEmpty())
                    FirebaseStorage.getInstance()
                        .getReferenceFromUrl(post.getBitmapPhotoUrl()).delete()
                confirmDeletePostDialog.setCancelable(true)
                confirmDeletePostDialog.dismiss()
                profilePage?.updateWithDeletedPost()
            }.addOnFailureListener { cancelPosting() }
    }

    private fun deleteOriginalPost(incompletePost: GoalProgress, originalPost: GoalProgress?,
                                   confirmMarkAsIncompleteDialog: AlertDialog?,
                                   disabledButton: LinearLayout?,
                                   markIncompleteButton: LinearLayout?,
                                   cancelButton: LinearLayout?,
                                   deletePhoto: Boolean, originalPostView: View?) {
        fun cancelPosting() {
            confirmMarkAsIncompleteDialog?.setCancelable(true)
            disabledButton?.visibility = View.GONE
            markIncompleteButton?.visibility = View.VISIBLE
            cancelButton?.visibility = View.VISIBLE
            profilePage?.showRequestUnavailableToast()
        }

        if(originalPost == null) {
            cancelPosting()
            return
        }

        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        postsTable.child(originalPost.getPostFirebaseKey()).setValue(null)
            .addOnSuccessListener {
                if(deletePhoto && originalPost.getBitmapPhotoUrl().isNotEmpty())
                    FirebaseStorage.getInstance()
                        .getReferenceFromUrl(originalPost.getBitmapPhotoUrl()).delete()
                confirmMarkAsIncompleteDialog?.setCancelable(true)
                confirmMarkAsIncompleteDialog?.dismiss()
                profilePage?.updateWithIncompletePost(incompletePost)
                profileXPage?.sendResolvedReportDetailsEmail(incompletePost)
                profileXPage?.removePostView(originalPostView)
            }.addOnFailureListener { cancelPosting() }
    }

    fun updateSeenLikes(selectedLabel: String) {
        val postLikesNotifsPage = postLikesNotifsPage?: return
        val signedInUserId = getSignedInUserId(TinyDB(postLikesNotifsPage))
        for (post in getStaticNewLikesMap().keys.iterator()) {
            if(selectedLabel.isEmpty() || post.getLabels().contains(selectedLabel))
                updateSeenLikesForPost(post.createProgressClone(), signedInUserId)
        }
    }

    private fun updateSeenLikesForPost(post: GoalProgress, signedInUserId: Int) {
        val seenLikes = post.getSeenLikes()
        val postLikes = post.getPostLikes()
        for(userId in seenLikes.keys.iterator()) {
            if(!postLikes.containsKey(userId)) {
                val timeLiked = seenLikes[userId]
                if(timeLiked != null)
                    postLikes[userId] = timeLiked
            }
        }

        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        val likesReference = postReference.child(SEEN_LIKES_PATH)
        likesReference.updateChildren(postLikes.toPostLikesDataMap(signedInUserId))
            .addOnFailureListener {
                postLikesNotifsPage?.showRequestUnavailableToast()
            }
    }

    private fun HashMap<Int, Long>.toPostLikesDataMap(signedInUserId: Int): HashMap<String, Any?> {
        val postLikesDataMap = HashMap<String, Any?>()
        for(userId in this.keys.iterator())
            if(userId != signedInUserId)
                postLikesDataMap[userId.toString()] = this[userId]
        return postLikesDataMap
    }

    fun updatePostLike(post: GoalProgress, userId: Int, liked: Boolean = true) {
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        val likesReference = postReference.child(LIKES_PATH)
        val likeValue = if(liked) getTimeLiked(post, userId) else null
        likesReference.child(userId.toString()).setValue(likeValue)
    }

    private fun getTimeLiked(post: GoalProgress, userId: Int): Long {
        return post.getSeenLikes()[userId]?: getTimeStamp()
    }

    fun updatePostLabels(post: GoalProgress) {
        if(postingInProgress) return
        postingInProgress = true
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        postReference.child(LABELS_PATH).setValue(post.getLabelsAsDataMap()
        ).addOnSuccessListener {
            postingInProgress = false
            selectLabelPage?.confirmLabelsUpdatedSuccess()
        }.addOnFailureListener {
            postingInProgress = false
            selectLabelPage?.notifyUpdateFailure()
        }
    }

    fun updatePostAttachments(post: GoalProgress) {
        if(postingInProgress) return
        val newPostData = post.toAttachmentsDataMap()
        postingInProgress = true
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        postReference.updateChildren(newPostData).addOnSuccessListener {
            postingInProgress = false
            seeAttachmentsPage?.confirmAttachmentsUpdatedSuccess()
        }.addOnFailureListener {
            postingInProgress = false
            seeAttachmentsPage?.notifyUpdateFailure()
        }
    }

    private fun GoalProgress.toAttachmentsDataMap(): HashMap<String, Any?> {
        val progress = this
        val newPostData = HashMap<String, Any?>()
        newPostData[NOTES_PATH] = progress.getNotesAsDataMap()
        newPostData[LINKS_PATH] = progress.getLinksAsDataMap()
        return newPostData
    }

    private fun postProgressData() {
        val progress = createPostPage?.getGoalProgress()
        val newPostData = progress?.toProgressDataMap()?: return
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.push()
        postReference.updateChildren(newPostData).addOnSuccessListener {
            createPostPage?.getGoalProgress()?.setPostFirebaseKey(postReference.key.toString())
            postingInProgress = false
            createPostPage?.confirmProgressPostedSuccess()
        }.addOnFailureListener {
            postingInProgress = false
            createPostPage?.getView()?.notifyPostFailure()
        }
    }

    private fun GoalProgress.toProgressDataMap(resolvedReportMap: HashMap<String, Any?>? = null): HashMap<String, Any?> {
        val progress = this
        val timestamp = getTimeStamp()
        uploader.createPostPage?.getGoalProgress()?.setTimestampPosted(timestamp)
        uploader.goalPage?.getIncompletePost()?.setTimestampPosted(timestamp)
        val newPostData = HashMap<String, Any?>()
        if(progress.isIncomplete())
            newPostData[IS_INCOMPLETE_PATH] = true
        if(progress.isNotRequired())
            newPostData[NOT_REQUIRED_PATH] = true

        newPostData[TIMESTAMP_POSTED_PATH] = timestamp
        newPostData[USER_ID_PATH] = progress.getUserId()
        val date = progress.getDate()
        newPostData[TIMESTAMP_GOAL_DATE_PATH] =
            if(progress.hasDate() && date != null)
                date.timeInMillis else null
        newPostData[REPEATING_DAYS_PATH] = if(progress.isRepeating())
            getRepeatingDaysFirebaseValue(progress.getRepeatingDays()) else null
        newPostData[GOAL_TIME_PATH] =
            getTimeDataString(progress.getHour(), progress.getMinute())
        val amount = progress.getAmount()
        val originalAmount = progress.getOriginalAmount()
        newPostData[AMOUNT_PATH] = if(amount > 0) amount else null
        newPostData[ORIGINAL_AMOUNT_PATH] = if(originalAmount > 0)
            originalAmount else null
        newPostData[TITLE_PATH] = progress.getTitle()
        newPostData[LABELS_PATH] = progress.getLabelsAsDataMap()
        newPostData[NOTES_PATH] = progress.getNotesAsDataMap()
        newPostData[LINKS_PATH] = progress.getLinksAsDataMap()

        newPostData[TIMESTAMP_DUE_DATE_PATH] = progress.getDueDate()?.timeInMillis
        newPostData[GOAL_FIREBASE_KEY_PATH] = progress.getGoalFirebaseKey()
        if(progress.getBitmapPhotoUrl().isNotEmpty())
            newPostData[PHOTO_URL_PATH] = progress.getBitmapPhotoUrl()
        newPostData[CAPTION_PATH] = progress.getCaption()
        val postIsLastOfDueDate = uploader.createPostPage
            ?.postIsLastOfDueDate()?: progress.isLastPostOfDueDate()
        newPostData[POST_IS_LAST_OF_DUE_DATE_PATH] = postIsLastOfDueDate

        if(resolvedReportMap != null)
            for(key in resolvedReportMap.keys.iterator())
                newPostData[key] = resolvedReportMap[key]

        return newPostData
    }

    private fun Goal.toGoalDataMap(isNew: Boolean = false, original: Goal? = null): HashMap<String, Any?>? {
        val editGoalPage = uploader.editGoalPage?: return null
        val goal = this
        val newGoalData = HashMap<String, Any?>()
        val timestamp = getTimeStamp()
        if(isNew) {
            editGoalPage.getGoal().setCreatedDate(timestamp)
            editGoalPage.getGoal().setActiveDate(timestamp)
            newGoalData[TIMESTAMP_ADDED_PATH] = timestamp
            newGoalData[USER_ID_PATH] = goal.getUserId()
        }
        editGoalPage.getGoal().setUpdatedDate(timestamp)
        newGoalData[TIMESTAMP_UPDATED_PATH] = timestamp
        val date = goal.getDate()
        newGoalData[TIMESTAMP_GOAL_DATE_PATH] =
            if(goal.hasDate() && date != null)
                date.timeInMillis else null
        newGoalData[REPEATING_DAYS_PATH] = if(goal.isRepeating())
            getRepeatingDaysFirebaseValue(goal.getRepeatingDays()) else null
        newGoalData[GOAL_TIME_PATH] =
            getTimeDataString(goal.getHour(), goal.getMinute())
        val amount = goal.getAmount()
        newGoalData[AMOUNT_PATH] = if(amount > 0) amount else null
        newGoalData[TITLE_PATH] = goal.getTitle()
        newGoalData[LABELS_PATH] = goal.getLabelsAsDataMap()
        newGoalData[NOTES_PATH] = goal.getNotesAsDataMap()
        newGoalData[LINKS_PATH] = goal.getLinksAsDataMap()
        newGoalData[IS_ACTIVE_PATH] = goal.isActive()
        if(isNew || (original != null && goal.activeStatusChangedFrom(original) && goal.isActive()))
            newGoalData[ACTIVE_START_TIMESTAMP_PATH] = goal.getActiveDate()
        return newGoalData
    }
}