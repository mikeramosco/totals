package com.justanotherdeveloper.totalslite

import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class ProfileSender(private val sendProfilePage: SendProfileActivity? = null,
                    private val sharedPage: SharedPageFragment? = null,
                    private val profilePage: ProgressPageActivity? = null,
                    private val usersDisplayPage: UsersDisplayPageActivity? = null) {

    private var profileSending = false

    fun profileSending(): Boolean {
        return profileSending
    }

    fun respondToInvite(profile: Profile, invitedUserId: Int,
                        acceptInvite: Boolean? = null) {
        val fb = FirebaseDatabase.getInstance().reference
        val profilesTable = fb.child(PROFILES_PATH)
        val profileReference = profilesTable.child(profile.getProfileKey())
        profileReference.child(invitedUserId.toString()).setValue(acceptInvite)
            .addOnFailureListener {
                usersDisplayPage?.showRequestUnavailableToast()
                sharedPage?.activity?.showRequestUnavailableToast()
            }
    }

    fun resetTotals(totalsStartDate: Calendar?, disabledButton: LinearLayout,
                    resetTotalsButton: LinearLayout, cancelButton: LinearLayout,
                    confirmSetTotalsDialog: AlertDialog) {
        val profile = profilePage?.getProfile()?: return
        val newProfileData = profile.toProfileDataMap(
            updateTotalsStartDate = true,
            totalsStartDate = totalsStartDate)
        val fb = FirebaseDatabase.getInstance().reference
        val profilesTable = fb.child(PROFILES_PATH)
        val profileReference = profilesTable.child(profile.getProfileKey())
        profileReference.updateChildren(newProfileData).addOnSuccessListener {
            profilePage.confirmResetTotalsSuccess(totalsStartDate)
            confirmSetTotalsDialog.setCancelable(true)
            confirmSetTotalsDialog.dismiss()
        }.addOnFailureListener {
            disabledButton.visibility = View.GONE
            resetTotalsButton.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            confirmSetTotalsDialog.setCancelable(true)
            profilePage.showRequestUnavailableToast()
        }
    }

    fun sendProfile(profile: Profile, selectedUsers: ArrayList<Int>) {
        if(profileSending) return
        profileSending = true
        val newProfileData = profile.toProfileDataMap(selectedUsers)
        val fb = FirebaseDatabase.getInstance().reference
        val profilesTable = fb.child(PROFILES_PATH)
        val profileReference = profilesTable.child(profile.getProfileKey())
        profileReference.updateChildren(newProfileData).addOnSuccessListener {
            sendProfilePage?.confirmProfileSentSuccess()
            profileSending = false
        }.addOnFailureListener {
            sendProfilePage?.getView()?.notifyPostFailure()
            profileSending = false
        }
    }

    private fun Profile.toProfileDataMap(selectedUsers: ArrayList<Int>? = null,
                                         updateTotalsStartDate: Boolean = false,
                                         totalsStartDate: Calendar? = null): HashMap<String, Any?> {
        val profileDataMap = HashMap<String, Any?>()
        if(selectedUsers != null)
            for (selectedUserId in selectedUsers)
                profileDataMap[selectedUserId.toString()] = false
        else if(updateTotalsStartDate)
            profileDataMap[TOTALS_START_DATE_PATH] =
                totalsStartDate?.timeInMillis
        profileDataMap[USER_ID_PATH] = getUser().getUserId()
        profileDataMap[PROFILE_LABEL_PATH] = getLabel()
        return profileDataMap
    }

}