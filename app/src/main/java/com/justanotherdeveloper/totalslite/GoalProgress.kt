package com.justanotherdeveloper.totalslite

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.google.firebase.database.DataSnapshot
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.Calendar

class GoalProgress: Goal() {
    private var originalAmount = 0
    private var isIncomplete = false
    private var notRequired = false
    private var isLastPostOfDueDate = false
    private var caption = ""
    private var photoBitmapUrl = ""
    private var photoBitmap: Bitmap? = null
    private var dueDate: Calendar? = null
    private var timestampPosted: Long = 0
    private var postFirebaseKey = ""
    private var postLikesMap = HashMap<Int, Long>()
    private var seenLikesMap = HashMap<Int, Long>()
    private var deletedPostNoticeSeen: Boolean? = null
    private var resolvedReportNote = ""
    private var reasonReported = ""

    fun createProgressClone(): GoalProgress {
        val clone = createClone().createGoalProgress()
        clone.originalAmount = originalAmount
        clone.isIncomplete = isIncomplete
        clone.notRequired = notRequired
        clone.isLastPostOfDueDate = isLastPostOfDueDate
        clone.caption = caption
        clone.photoBitmapUrl = photoBitmapUrl
        clone.dueDate = dueDate?.dateClone()
        clone.timestampPosted = timestampPosted
        clone.postFirebaseKey = postFirebaseKey
        clone.postLikesMap = postLikesMap.intLongMapClone()
        clone.seenLikesMap = seenLikesMap.intLongMapClone()
        clone.deletedPostNoticeSeen = deletedPostNoticeSeen
        clone.resolvedReportNote = resolvedReportNote
        clone.reasonReported = reasonReported
        return clone
    }

    fun setOriginalAmount(amount: Int) {
        originalAmount = amount
    }

    fun getOriginalAmount(): Int {
        return originalAmount
    }

    fun getSeenLikes(): HashMap<Int, Long> {
        return seenLikesMap
    }

    fun getPostLikes(): HashMap<Int, Long> {
        return postLikesMap
    }

    fun postLiked(userId: Int): Boolean {
        return postLikesMap.containsKey(userId)
    }

    fun likePost(userId: Int) {
        if(!postLikesMap.containsKey(userId))
            postLikesMap[userId] = getTimeStamp()
    }

    fun unlikePost(userId: Int) {
        postLikesMap.remove(userId)
    }

    fun setAsIncomplete() {
        isIncomplete = true
        isLastPostOfDueDate = true
    }

    fun isIncomplete(): Boolean {
        return isIncomplete
    }

    fun setAsNotRequired(notRequired: Boolean = true,
                         isLastPostOfDueDate: Boolean = true) {
        this.notRequired = notRequired
        this.isLastPostOfDueDate = isLastPostOfDueDate
    }

    fun isNotRequired(): Boolean {
        return notRequired
    }

    fun isLastPostOfDueDate(): Boolean {
        return isLastPostOfDueDate
    }

    fun isFromDeletedReportedPost(): Boolean {
        return deletedPostNoticeSeen != null
    }

    fun deletedPostNoticeSeen(): Boolean? {
        return deletedPostNoticeSeen
    }

    fun setPostFirebaseKey(firebaseKey: String) {
        postFirebaseKey = firebaseKey
    }

    fun getPostFirebaseKey(): String {
        return postFirebaseKey
    }

    fun setCaption(caption: String) {
        this.caption = caption
    }

    fun getCaption(): String {
        return caption
    }

    fun hasCaption(): Boolean {
        return caption.isNotEmpty()
    }

    fun setResolvedReportNote(resolvedReportNote: String) {
        this.resolvedReportNote = resolvedReportNote
    }

    fun getResolvedReportNote(): String {
        return resolvedReportNote
    }

    fun setReasonReported(reasonReported: String) {
        this.reasonReported = reasonReported
    }

    fun getReasonReported(): String {
        return reasonReported
    }

    fun setTimestampPosted(timestamp: Long) {
        timestampPosted = timestamp
    }

    fun getTimestampPosted(): Long {
        return timestampPosted
    }

    fun setDueDate(date: Calendar) {
        dueDate = date
    }

    fun getDueDate(): Calendar? {
        return dueDate
    }

    fun setBitmapPhotoUrl(photoBitmapUrl: String) {
        this.photoBitmapUrl = photoBitmapUrl
    }

    fun getBitmapPhotoUrl(): String {
        return photoBitmapUrl
    }

    fun setBitmapPhoto(photoBitmap: Bitmap) {
        this.photoBitmap = photoBitmap
    }

    fun getBitmapPhoto(): Bitmap? {
        return photoBitmap
    }

    fun photoLoaded(): Boolean {
        return photoBitmap != null
    }

    fun getPhotoDetails(): String {
        if(hasCaption()) return caption
        return if(getAmount() == 0) getTitle()
        else "${getAmount()} ${getTitle()}"
    }

    fun parsePostData(postData: DataSnapshot): Boolean {
        postFirebaseKey = postData.key.toString()
        val timestampPostedValue = postData.child(TIMESTAMP_POSTED_PATH).value
        if(timestampPostedValue == null) {
            // TODO
            return false
        }

        parseGoalData(postData, true)
        setGoalFirebaseKey(postData.child(GOAL_FIREBASE_KEY_PATH).value.toString())
        val isIncompleteValue = postData.child(IS_INCOMPLETE_PATH).value
        if(isIncompleteValue != null) isIncomplete = true
        val notRequiredValue = postData.child(NOT_REQUIRED_PATH).value
        if(notRequiredValue != null) notRequired = true

        val likesDataIterator = postData.child(LIKES_PATH).children.iterator()
        while(likesDataIterator.hasNext()) {
            val postLikeData = likesDataIterator.next()
            val userId = postLikeData.key.toString().toInt()
            val timeLiked = postLikeData.value.toString().toLong()
            postLikesMap[userId] = timeLiked
        }

        val seenLikesDataIterator = postData.child(SEEN_LIKES_PATH).children.iterator()
        while(seenLikesDataIterator.hasNext()) {
            val seenLikeData = seenLikesDataIterator.next()
            val userId = seenLikeData.key.toString().toInt()
            val timeLiked = seenLikeData.value.toString().toLong()
            seenLikesMap[userId] = timeLiked
        }

        val originalAmountValue = postData.child(ORIGINAL_AMOUNT_PATH).value
        if(originalAmountValue != null) originalAmount = originalAmountValue.toString().toInt()
        timestampPosted = timestampPostedValue.toString().toLong()
        caption = postData.child(CAPTION_PATH).value.toString()
        dueDate = getCalendar(postData.child(TIMESTAMP_DUE_DATE_PATH).value.toString().toLong())
        val photoBitmapUrlValue = postData.child(PHOTO_URL_PATH).value
        if(photoBitmapUrlValue != null) photoBitmapUrl = photoBitmapUrlValue.toString()
        isLastPostOfDueDate = postData.child(POST_IS_LAST_OF_DUE_DATE_PATH)
            .value.toString().toBoolean()

        val reasonReportedValue = postData.child(REASON_REPORTED_PATH).value
        if(reasonReportedValue != null)
            reasonReported = reasonReportedValue.toString()

        val resolvedReportNoteValue = postData.child(RESOLVED_REPORT_NOTE_PATH).value
        if(resolvedReportNoteValue != null)
            resolvedReportNote = resolvedReportNoteValue.toString()

        val deletedPostNoticeSeenValue = postData.child(DELETED_POST_NOTICE_SEEN_PATH).value
        if(deletedPostNoticeSeenValue != null)
            deletedPostNoticeSeen = deletedPostNoticeSeenValue.toString().toBoolean()

        return true
    }

    fun retrieveBitmap(context: Context, photos: PhotoDatabase?, toDisplay: ImageView? = null) {
        if(photoBitmapUrl.isEmpty()) return
        val filename = getPhotoFilename(photoBitmapUrl)
        val photo = if(filename != null) photos?.getPhoto(filename) else null
        if(photo != null) {
            photoBitmap = photo
            toDisplay?.setImageBitmap(photoBitmap)
        } else {
            val imageView = ImageView(context)
            Picasso.get().load(photoBitmapUrl)
                .into(imageView, object : Callback {
                    override fun onSuccess() {
                        val bitmap = imageView.drawable.toBitmap()
                        photoBitmap = bitmap
                        toDisplay?.setImageBitmap(photoBitmap)
                        photos?.addPhoto(createPhotoFilename(photoBitmapUrl), bitmap)
                    }
                    override fun onError(e: Exception) {
                        photoBitmap = context.getBackupPhotoIcon()
                        toDisplay?.setImageBitmap(photoBitmap)
                    }
                })
        }
    }
}