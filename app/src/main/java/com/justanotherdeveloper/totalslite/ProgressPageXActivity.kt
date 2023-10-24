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
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.justanotherdeveloper.totalslite.databinding.ActivityProgressPageXBinding
import java.util.*

class ProgressPageXActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressPageXBinding

    private lateinit var uploader: GoalUploader
    private lateinit var accounts: TotalsAccountEditor

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()
        initBackButton()
        retrievePosts()

        if(forResolvedReports())
            binding.progressPageXTitle.text = "Resolved reports"
        uploader = GoalUploader(profileXPage = this)
        accounts = TotalsAccountEditor(profileXPage = this)
    }

    private fun initBinding() {
        binding = ActivityProgressPageXBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initBackButton() {
        initButtonAnimationListener(binding.backButton)
        binding.backButton.setOnClickListener { finish() }
    }

    private fun forResolvedReports(): Boolean {
        return intent.getBooleanExtra(FOR_RESOLVED_REPORTS_REF, false)
    }

    private fun retrievePosts() {
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val pathForPostsToManage = if(forResolvedReports())
            REPORT_RESOLVED_PATH else REPORTED_PATH
        val postsQuery = postsTable.orderByChild(pathForPostsToManage).equalTo(true)
        postsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                for(post in parseUserPostsData(data)) if(post.isIncomplete())
                    addIncompletePostView(post) else addPostView(post)

            }

            override fun onCancelled(error: DatabaseError) {
                showRequestUnavailableToast()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    fun addIncompletePostView(post: GoalProgress) {
        val postView = layoutInflater.inflate(R.layout.view_post_incomplete, null)
        val postLayout = postView.findViewById<LinearLayout>(R.id.postLayout)
        val goalTitleText = postView.findViewById<TextView>(R.id.goalTitleText)
        val incompleteForDateText = postView.findViewById<TextView>(R.id.incompleteForDateText)
        val captionText = postView.findViewById<TextView>(R.id.captionText)
        val reasonReportedText = postView.findViewById<TextView>(R.id.reasonReportedText)
        val resolvedReportNoteText = postView.findViewById<TextView>(R.id.resolvedReportNoteText)
        val moreButton = postView.findViewById<ImageView>(R.id.moreButton)
        val incompleteIcon = postView.findViewById<ImageView>(R.id.incompleteIcon)

        postLayout.visibility = View.VISIBLE

        val amount = post.getAmount()
        val titleString = if(amount > 0)
            "$amount ${post.getTitle()}"
        else post.getTitle()

        val hour = post.getHour()
        val minute = post.getMinute()
        goalTitleText.text = getString(R.string.titleAndTimeDueString,
            titleString, getTimeText(hour, minute))

        val dueDate = post.getDueDate()
        if(dueDate != null) {
            incompleteForDateText.text = if (post.isNotRequired())
                getString(R.string.notRequiredDetailsString, dueDate.toDateString())
            else getString(R.string.incompleteDetailsString, dueDate.toDateString())
        } else incompleteForDateText.visibility = View.GONE
        if(post.hasCaption()) captionText.text = post.getCaption()
        else captionText.visibility = View.GONE
        reasonReportedText.visibility = View.VISIBLE
        reasonReportedText.text = "REPORT MESSAGE: ${post.getReasonReported()}"
        val resolvedReportNote = "RESOLVED REPORT NOTE: ${post.getResolvedReportNote()}"
        if(resolvedReportNote.isNotEmpty()) {
            resolvedReportNoteText.visibility = View.VISIBLE
            resolvedReportNoteText.text = resolvedReportNote
        }

        if(post.isNotRequired())
            incompleteIcon.setImageResource(R.drawable.ic_cancelled)

        moreButton.setOnClickListener {
            showPostOptionsDialog(postView, post)
        }

        binding.postsContainer.addView(postView, 0)
    }

    @SuppressLint("SetTextI18n")
    private fun addPostView(post: GoalProgress) {
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
        val reasonReportedText = postView.findViewById<TextView>(R.id.reasonReportedText)
        val resolvedReportNoteText = postView.findViewById<TextView>(R.id.resolvedReportNoteText)
        val likeButton = postView.findViewById<ImageView>(R.id.likeButton)
        val moreButton = postView.findViewById<ImageView>(R.id.moreButton)

        likeButton.visibility = View.GONE

        progressPhotoLayout.visibility = View.VISIBLE
        if(post.photoLoaded()) progressPhoto.setImageBitmap(post.getBitmapPhoto())
        else post.retrieveBitmap(this, getStaticHomePage()?.getPhotos(), progressPhoto)
        transparentLayer.visibility = View.VISIBLE
        val amount = post.getAmount()
        if(amount == 0) goalAmountText.visibility = View.GONE
        else goalAmountText.text = amount.toString()
        goalTitleText.text = post.getTitle()
        if(post.hasCaption()) captionText.text = post.getCaption()
        else captionText.visibility = View.GONE
        reasonReportedText.visibility = View.VISIBLE
        reasonReportedText.text = "REPORT MESSAGE: ${post.getReasonReported()}"
        val resolvedReportNote = "RESOLVED REPORT NOTE: ${post.getResolvedReportNote()}"
        if(resolvedReportNote.isNotEmpty()) {
            resolvedReportNoteText.visibility = View.VISIBLE
            resolvedReportNoteText.text = resolvedReportNote
        }
        dateText.text = getDateTextString(post, true, post.getDueDate())
        postDetailsText.text = getPostedTextString(post)

        moreButton.setOnClickListener {
            showPostOptionsDialog(postView, post)
        }

        clickableLayout.setOnClickListener{
            if(post.photoLoaded()) openViewPhotoPage(post)
        }

        binding.postsContainer.addView(postView, 0)
    }

    private fun openViewPhotoPage(post: GoalProgress) {
        val user = getStaticUsersMap()[post.getUserId()]?: return
        setBitmapPhotoToView(post.getBitmapPhoto())
        setStaticTotalsUser(user)
        val intent = Intent(this, ViewPhotoPageActivity::class.java)
        intent.putExtra(PHOTO_DETAILS_REF, post.getPhotoDetails())
        startActivity(intent)
    }

    private fun showPostOptionsDialog(postView: View, post: GoalProgress) {
        val postMoreOptionsDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(
            R.layout.bottomsheet_post_settings_x, null)
        postMoreOptionsDialog.setContentView(view)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(view.findViewById(R.id.removeFromReportedOption))
        dialogOptions.add(view.findViewById(R.id.deletePostOption))
        dialogOptions.add(view.findViewById(R.id.banUserOption))
        dialogOptions.add(view.findViewById(R.id.seeAttachmentsOption))
        initDialogOptions(postMoreOptionsDialog, dialogOptions)

        val user = getStaticUsersMap()[post.getUserId()]

        if(forResolvedReports())
            dialogOptions[0].visibility = View.GONE

        if(post.isFromDeletedReportedPost())
            dialogOptions[1].visibility = View.GONE

        if(user != null && user.isBanned())
            dialogOptions[2].visibility = View.GONE

        if(post.getNotes().isEmpty() && post.getLinks().isEmpty())
            dialogOptions[3].visibility = View.GONE

        if(!dialogOptions[0].isVisible
            && !dialogOptions[1].isVisible
            && !dialogOptions[2].isVisible
            && !dialogOptions[3].isVisible)
            return

        fun optionClicked(option: LinearLayout) {
            option.setOnClickListener {
                when(option) {
                    dialogOptions[0] -> showAccessCodeDialog(
                        postView, post, removeFromReported = true)
                    dialogOptions[1] -> showAccessCodeDialog(
                        postView, post, deletePost = true)
                    dialogOptions[2] -> showAccessCodeDialog(
                        postView, post, banUser = true)
                    dialogOptions[3] -> openAttachmentsPage(post)
                }
                postMoreOptionsDialog.dismiss()
            }
        }

        for(option in dialogOptions)
            optionClicked(option)

        postMoreOptionsDialog.show()
    }

    private fun showAccessCodeDialog(postView: View, post: GoalProgress,
                                     removeFromReported: Boolean = false,
                                     deletePost: Boolean = false, banUser: Boolean = false) {
        val view = layoutInflater.inflate(R.layout.dialog_access_code, null)
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(view)
        val accessCodeDialog = builder.create()
        accessCodeDialog.setCancelable(true)

        accessCodeDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val noteLayout = view.findViewById<LinearLayout>(R.id.noteLayout)
        val noteField = view.findViewById<EditText>(R.id.noteField)
        noteLayout.visibility = View.VISIBLE

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
                val note = noteField.text.toString()
                if(firstPartCompleted && enteredCode == ACCESS_CODE) {
                    when {
                        removeFromReported -> removeFromReported(postView, post, note)
                        deletePost -> deletePost(postView, post, note)
                        banUser -> {
                            val user = getStaticUsersMap()[post.getUserId()]
                            if(user != null) accounts.banUser(user, postView, post, note)
                        }
                    }
                    accessCodeDialog.dismiss()
                } else if(enteredCode == accessCodeFirstPart)
                    extendFieldForSecondPart()
            }
        })

        accessCodeDialog.show()
    }

    private fun openAttachmentsPage(post: GoalProgress) {
        val intent = Intent(this, SeeAttachmentsActivity::class.java)
        setStaticProgress(post)
        startActivity(intent)
    }

    fun removeFromReported(postView: View, post: GoalProgress, note: String) {
        val fb = FirebaseDatabase.getInstance().reference
        val postsTable = fb.child(POSTS_PATH)
        val postReference = postsTable.child(post.getPostFirebaseKey())
        post.setResolvedReportNote(note)
        postReference.updateChildren(getResolvedReportMap(note)).addOnSuccessListener {
            sendResolvedReportDetailsEmail(post)
            removePostView(postView)
        }.addOnFailureListener {
            showRequestUnavailableToast()
        }
    }

    private fun getResolvedReportMap(note: String,
                                     reasonReported: String = "",
                                     deletePost: Boolean = false): HashMap<String, Any?> {
        val resolvedReportMap = HashMap<String, Any?>()
        resolvedReportMap[REPORTED_PATH] = null
        resolvedReportMap[REPORT_RESOLVED_PATH] = true
        resolvedReportMap[RESOLVED_REPORT_NOTE_PATH] = note.ifEmpty { null }
        if(deletePost) {
            resolvedReportMap[REASON_REPORTED_PATH] = reasonReported
            resolvedReportMap[DELETED_POST_NOTICE_SEEN_PATH] = false
        }
        return resolvedReportMap
    }

    private fun deletePost(postView: View, post: GoalProgress, note: String) {
        val incompletePost = post.createProgressClone()
        incompletePost.setAsIncomplete()
        incompletePost.setBitmapPhotoUrl("")
        incompletePost.setCaption("")
        incompletePost.getPostLikes().clear()
        incompletePost.getNotes().clear()
        incompletePost.getLinks().clear()
        incompletePost.setReasonReported(note)
        incompletePost.setAsNotRequired(true,
            post.isLastPostOfDueDate())

        uploader.postIncompleteGoal(incompletePost,
            originalPost = post, originalPostView = postView,
            resolvedReportMap = getResolvedReportMap(note,
                post.getReasonReported(),true),
            deletePhoto = false)
    }

    fun sendResolvedReportDetailsEmail(post: GoalProgress) {
        val user = getStaticUsersMap()[post.getUserId()]?: return

        val amount = post.getAmount()
        val postTitle = if(amount > 0)
            "$amount ${post.getTitle()}" else post.getTitle()
        val subject = "Report Resolved: $postTitle by ${user.getUsername()}"
        val message = "Resolved Report Note:\n${post.getResolvedReportNote()}\n\n" +
                getEmailReportedPostMessage(post, user)
        ReportedPostsDataUploader().uploadReportedPostData(this, subject, message)
//        EmailUtils().sendEmail(subject, message)
    }

    fun removePostView(postView: View?) {
        showToast("Report resolved")
        beginTransition(binding.progressPageXParent)
        binding.postsContainer.removeView(postView)
    }
}