package com.justanotherdeveloper.totalslite

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.HashMap

class TotalsUser(private val refKey: String, private val userId: Int,
                 private var username: String, private var name: String) {

    private var profileBio = ""
    private var profilePhotoUrl = ""
    private var profilePhotoBitmap: Bitmap? = null
    private var blockedUsers = ArrayList<Int>()
    private var phoneNumber = ""

    fun createCloneWithoutBitmap(): TotalsUser {
        val clone = TotalsUser(refKey, userId, username, name)
        clone.profileBio = profileBio
        clone.profilePhotoUrl = profilePhotoUrl
        clone.phoneNumber = phoneNumber
        return clone
    }

    fun displayOnView(context: Context, profilePhotoImage: ImageView, profilePhotoLetter: TextView,
                      usernameText: TextView? = null, nameText: TextView? = null, bioText: TextView? = null) {

        fun displayProfilePhotoLetter() {
            profilePhotoImage.visibility = View.GONE
            profilePhotoLetter.visibility = View.VISIBLE
            profilePhotoLetter.text = username[0].toString().uppercase(Locale.getDefault())
        }

        if (hasProfilePhotoUrl()) {
            if(hasProfilePhotoBitmap())
                displayProfilePhotoImage(profilePhotoImage, profilePhotoLetter)
            else {
                displayProfilePhotoLetter()
                retrieveBitmap(context, profilePhotoImage, profilePhotoLetter)
            }
        } else displayProfilePhotoLetter()
        usernameText?.text = username
        nameText?.text = name
        bioText?.text = profileBio
        bioText?.visibility = if(profileBio.isEmpty())
            View.GONE else View.VISIBLE
    }

    fun displayProfilePhotoImage(profilePhotoImage: ImageView, profilePhotoLetter: TextView) {
        profilePhotoLetter.visibility = View.GONE
        profilePhotoImage.visibility = View.GONE
        profilePhotoImage.setImageBitmap(profilePhotoBitmap)
        profilePhotoImage.visibility = View.VISIBLE
    }

    private fun retrieveBitmap(context: Context,
                               profilePhotoImage: ImageView,
                               profilePhotoLetter: TextView) {
        if(!hasProfilePhotoUrl()) return

        context.getProfilePhoto(this, TinyDB(context),
            profilePhotoImage, profilePhotoLetter)
    }

    fun hasNewData(revealUser: TotalsUser): Boolean {
        return hasNewName(revealUser) || hasNewUsername(revealUser)
                || hasNewProfileBio(revealUser) || hasNewProfilePhoto(revealUser)
    }

    fun hasNewName(revealUser: TotalsUser): Boolean {
        return name != revealUser.name
    }

    fun hasNewUsername(revealUser: TotalsUser): Boolean {
        return username != revealUser.username
    }

    fun hasNewProfileBio(revealUser: TotalsUser): Boolean {
        return profileBio != revealUser.profileBio
    }

    fun hasNewProfilePhoto(revealUser: TotalsUser): Boolean {
        return profilePhotoUrl != revealUser.profilePhotoUrl
    }

    fun getDatabaseReferenceKey(): String {
        return refKey
    }

    fun getUserId(): Int {
        return userId
    }

    fun setUsername(username: String) {
        this.username = username
    }

    fun getUsername(): String {
        return username
    }

    fun setName(name: String) {
        this.name = name
    }

    fun getName(): String {
        return name
    }

    fun setProfileBio(profileBio: String) {
        this.profileBio = profileBio
    }

    fun getProfileBio(): String {
        return profileBio
    }

    fun hasProfileBio(): Boolean {
        return profileBio.isNotEmpty()
    }

    fun setProfilePhotoUrl(profilePhotoUrl: String) {
        this.profilePhotoUrl = profilePhotoUrl
    }

    fun getProfilePhotoUrl(): String {
        return profilePhotoUrl
    }

    fun hasProfilePhotoUrl(): Boolean {
        return profilePhotoUrl.isNotEmpty()
    }

    fun setProfilePhotoBitmap(profilePhotoBitmap: Bitmap?, asCircleBitmap: Boolean = true) {
        this.profilePhotoBitmap = if(asCircleBitmap)
            profilePhotoBitmap?.toCircleBitmap()
        else profilePhotoBitmap
    }

    fun getProfilePhotoBitmap(): Bitmap? {
        return profilePhotoBitmap
    }

    fun circleCropProfilePhoto() {
        profilePhotoBitmap = profilePhotoBitmap?.toCircleBitmap()
    }

    fun hasProfilePhotoBitmap(): Boolean {
        return profilePhotoBitmap != null
    }

    fun removeProfilePhoto() {
        profilePhotoUrl = ""
        profilePhotoBitmap = null
    }

    fun getBlockedUsers(): ArrayList<Int> {
        return blockedUsers
    }

    fun getBlockedUsersDataMap(): HashMap<String, Any> {
        val dataMap = HashMap<String, Any>()
        for(userId in blockedUsers)
            dataMap[userId.toString()] = true
        return dataMap
    }

    fun setPhoneNumber(phoneNumber: String) {
        this.phoneNumber = phoneNumber
    }

    fun getPhoneNumber(): String {
        return phoneNumber
    }
}