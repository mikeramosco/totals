package com.justanotherdeveloper.totalslite

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog

class SharedPageDialogs(private val activity: HomePageActivity,
                        private val fragment: SharedPageFragment) {

    fun showSharedProfileMoreOptionsDialog(profile: Profile, forInvite: Boolean = false) {
        if(fragment.otherProcessStarted()) return
        val sharedProfileMoreOptionsDialog = BottomSheetDialog(activity)
        val view = activity.layoutInflater.inflate(
            R.layout.bottomsheet_shared_profile_more_options, null)
        sharedProfileMoreOptionsDialog.setContentView(view)

        val openProfileOption = view.findViewById<LinearLayout>(R.id.openProfileOption)
        val unfollowProfileOption = view.findViewById<LinearLayout>(R.id.unfollowProfileOption)
        val blockInvitesOption = view.findViewById<LinearLayout>(R.id.blockInvitesOption)

        if(forInvite) {
            openProfileOption.visibility = View.GONE
            unfollowProfileOption.visibility = View.GONE
        }

        val dialogOptions = ArrayList<LinearLayout>()
        dialogOptions.add(openProfileOption)
        dialogOptions.add(unfollowProfileOption)
        dialogOptions.add(blockInvitesOption)
        initDialogOptions(sharedProfileMoreOptionsDialog, dialogOptions)

        openProfileOption.setOnClickListener {
            fragment.setOtherProcessStarted(false)
            fragment.openProgressPage(profile)
            sharedProfileMoreOptionsDialog.cancel()
        }

        unfollowProfileOption.setOnClickListener {
            showConfirmUnfollowProfileDialog(profile)
            sharedProfileMoreOptionsDialog.cancel()
        }

        blockInvitesOption.setOnClickListener {
            showConfirmBlockUserDialog(profile)
            sharedProfileMoreOptionsDialog.cancel()
        }

        sharedProfileMoreOptionsDialog.setOnCancelListener {
            fragment.setOtherProcessStarted(false)
            sharedProfileMoreOptionsDialog.dismiss()
        }

        sharedProfileMoreOptionsDialog.show()
    }

    private fun showConfirmUnfollowProfileDialog(profile: Profile) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_unfollow_profile, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmUnfollowProfileDialog = builder.create()
        confirmUnfollowProfileDialog.setCancelable(true)

        confirmUnfollowProfileDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val unfollowButton = view.findViewById<LinearLayout>(R.id.unfollowButton)
        initButtonAnimationListener(unfollowButton)
        unfollowButton.setOnClickListener {
            fragment.getProfiles().respondToInvite(profile,
                getSignedInUserId(activity.getDatabase()))
            confirmUnfollowProfileDialog.dismiss()
        }

        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmUnfollowProfileDialog.dismiss()
        }

        confirmUnfollowProfileDialog.show()
    }

    private fun showConfirmBlockUserDialog(profile: Profile) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_block_user, null)
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(false)
        builder.setView(view)
        val confirmBlockUserDialog = builder.create()
        confirmBlockUserDialog.setCancelable(true)

        confirmBlockUserDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val blockUserButton = view.findViewById<LinearLayout>(R.id.blockUserButton)
        initButtonAnimationListener(blockUserButton)
        blockUserButton.setOnClickListener {
            fragment.getProfiles().respondToInvite(profile,
                getSignedInUserId(activity.getDatabase()))
            val signedInUser = getStaticSignedInTotalsUser(activity, activity.getDatabase())
            signedInUser?.getBlockedUsers()?.add(profile.getUser().getUserId())
            if(signedInUser != null) {
                setSignedInTotalsUser(activity.getDatabase(), signedInUser)
                fragment.getAccounts().updateBlockedUsers(signedInUser)
            }
            confirmBlockUserDialog.dismiss()
        }

        val cancelButton = view.findViewById<LinearLayout>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            confirmBlockUserDialog.dismiss()
        }

        confirmBlockUserDialog.show()
    }
}