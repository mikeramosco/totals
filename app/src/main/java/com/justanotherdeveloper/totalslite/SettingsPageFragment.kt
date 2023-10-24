package com.justanotherdeveloper.totalslite

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.justanotherdeveloper.totalslite.databinding.FragmentSettingsPageBinding
import java.util.ArrayList

class SettingsPageFragment(private val activity: HomePageActivity) : Fragment() {

    private lateinit var local: TinyDB

    private lateinit var listeners: SettingsPageListeners

    private var otherProcessStarted = false

    private var _binding: FragmentSettingsPageBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    /** Main */
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityCreated(savedInstanceState)",
        "androidx.fragment.app.Fragment"))
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        local = TinyDB(activity)
        listeners = SettingsPageListeners(activity, this)

        binding.profilePhotoLayout.visibility = View.VISIBLE
        displayProfileInfo()

        displaySelectedMissedDueTimeAction()

        val phoneNumber = getStaticSignedInTotalsUser(
            activity, activity.getDatabase())?.getPhoneNumber()
        if(phoneNumber == ACCESS_NUMBER)
            binding.adminButtonsLayout.visibility = View.VISIBLE
    }

    fun displayProfileInfo() {
        getStaticSignedInTotalsUser(activity, local)?.displayOnView(activity,
            binding.profilePhotoImage, binding.profilePhotoLetter,
            binding.usernameText, binding.nameText)
    }

    @Suppress("DEPRECATION")
    fun openEditProfilePage() {
        if(otherProcessStarted()) return
        val intent = Intent(activity, EditProfileInfoPageActivity::class.java)
        startActivityForResult(intent, 0)
    }

    fun openManageBlockedUsersPage() {
        if(otherProcessStarted()) return
        val signedInUser = getStaticSignedInTotalsUser(activity, local)?: return
        if(signedInUser.getBlockedUsers().isNotEmpty()) {
            val intent = Intent(activity, UsersDisplayPageActivity::class.java)
            intent.putExtra(USERS_TO_DISPLAY_REF, FOR_MANAGE_BLOCKED_USERS_REF)
            startActivityForResult(intent, 0)
        } else {
            setOtherProcessStarted(false)
            activity.showToast(activity.getString(R.string.noBlockedUsersMessage))
        }
    }

    fun openManageBannedUsersPage() {
        val intent = Intent(activity, UsersDisplayPageActivity::class.java)
        intent.putExtra(USERS_TO_DISPLAY_REF, FOR_MANAGE_BANNED_USERS_REF)
        startActivityForResult(intent, 0)
    }

    fun openManageReportedPostsPage(forResolvedReports: Boolean = false) {
        val intent = Intent(activity, ProgressPageXActivity::class.java)
        intent.putExtra(FOR_RESOLVED_REPORTS_REF, forResolvedReports)
        startActivity(intent)
    }

    private fun displaySelectedMissedDueTimeAction() {
        when(getMissedDueTimeAction(local)) {
            SET_GOAL_INACTIVE_REF -> {
                binding.selectedMissedDueTimeActionIcon.setImageResource(R.drawable.ic_cancelled)
                binding.selectedMissedDueTimeActionText.text =
                    getString(R.string.missedDueDateSetInactiveSelectedDetails)
            }
            AUTO_POST_INCOMPLETE_REF -> {
                binding.selectedMissedDueTimeActionIcon.setImageResource(R.drawable.ic_missed)
                binding.selectedMissedDueTimeActionText.text =
                    getString(R.string.missedDueDateSetAutoPostsAsIncompleteDetails)
            }
        }
    }

    fun showChooseMissedDueTimeActionDialog() {
        if(otherProcessStarted()) return
        val chooseMissedDueTimeActionDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_set_action_for_missed_due_time, null)
        chooseMissedDueTimeActionDialog.setContentView(view)

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(view.findViewById(R.id.setInactiveOption))
        dialogOptions.add(view.findViewById(R.id.postAsIncompleteOption))
        initDialogOptions(chooseMissedDueTimeActionDialog, dialogOptions)

        fun clickOption(option: LinearLayout) {
            when(option) {
                dialogOptions[0] -> setMissedDueTimeAction(local, SET_GOAL_INACTIVE_REF)
                dialogOptions[1] -> setMissedDueTimeAction(local, AUTO_POST_INCOMPLETE_REF)
            }

            displaySelectedMissedDueTimeAction()
            chooseMissedDueTimeActionDialog.cancel()
        }

        for(option in dialogOptions)
            option.setOnClickListener { clickOption(option) }

        chooseMissedDueTimeActionDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            chooseMissedDueTimeActionDialog.dismiss()
        }

        chooseMissedDueTimeActionDialog.show()
    }

    fun showConfirmLogoutDialog() {
        if(otherProcessStarted()) return
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_logout, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmCloseDialog = builder.create()
        confirmCloseDialog.setCancelable(true)

        confirmCloseDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val keepEditingButton = view.findViewById<LinearLayout>(R.id.stayLoggedInButton)
        initButtonAnimationListener(keepEditingButton)
        keepEditingButton.setOnClickListener {
            confirmCloseDialog.cancel()
        }

        val closeButton = view.findViewById<LinearLayout>(R.id.logoutButton)
        closeButton.setOnClickListener {
            confirmCloseDialog.cancel()
            logout()
        }

        confirmCloseDialog.setOnCancelListener {
            setOtherProcessStarted(false)
            confirmCloseDialog.dismiss()
        }

        confirmCloseDialog.show()
    }

    fun showAccessCodeDialog(manageBannedUsers: Boolean = false,
                             manageReportedPosts: Boolean = false,
                             manageResolvedReports: Boolean = false) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_access_code, null)
        val builder = AlertDialog.Builder(activity)
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
                    when {
                        manageBannedUsers -> openManageBannedUsersPage()
                        manageReportedPosts -> openManageReportedPostsPage()
                        manageResolvedReports -> openManageReportedPostsPage(true)
                    }
                    accessCodeDialog.dismiss()
                } else if (enteredCode == accessCodeFirstPart)
                    extendFieldForSecondPart()
            }
        })

        accessCodeField.requestFocus()
        accessCodeDialog.show()
    }

    fun aboutButtonClicked() {
        activity.openLink(LEGAL_POLICIES_SITE)
    }

    private fun logout() {
        signOutTotalsUser(local)
        activity.finish()
    }

    /** "Get" Methods */

    fun getFragmentBinding(): FragmentSettingsPageBinding {
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