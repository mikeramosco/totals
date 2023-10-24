package com.justanotherdeveloper.totalslite

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.justanotherdeveloper.totalslite.databinding.ActivitySendProfileBinding

class SendProfileActivity : AppCompatActivity() {

    private lateinit var view: SendProfileView
    private lateinit var listeners: SendProfileListeners

    private lateinit var binding: ActivitySendProfileBinding

    private lateinit var local: TinyDB

    private lateinit var profiles: ProfileSender
    private lateinit var profile: Profile

    private val selectedUsers = ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)

        val staticProfile = getStaticProfile()
        if(staticProfile == null) { finish(); return }
        setStaticProfile(null)

        profiles = ProfileSender(sendProfilePage = this)
        profile = staticProfile

        listeners = SendProfileListeners(this)
        view = SendProfileView(this)

        binding.sendProfileTitleText.text = getString(R.string.sendProfileToText, profile.getLabel())

        binding.sendProfilePageParent.post {
            moveCursorTo(binding.searchField)
        }
    }

    private fun initBinding() {
        binding = ActivitySendProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun sendButtonClicked() {
        view.showSendingState()
        profiles.sendProfile(profile, selectedUsers)
    }

    fun confirmProfileSentSuccess() {
        showToast(getString(R.string.profileSentMessage))
        for(userId in selectedUsers) {
            if(!profile.getFollowers().keys.contains(userId))
                profile.getFollowers()[userId] = false
        }
        finish()
    }

    override fun onBackPressed() {
        if(profiles.profileSending()) goToHomeScreen()
        else super.onBackPressed()
    }

    fun getBinding(): ActivitySendProfileBinding {
        return binding
    }

    fun getView(): SendProfileView {
        return view
    }

    fun getListeners(): SendProfileListeners {
        return listeners
    }

    fun getDatabase(): TinyDB {
        return local
    }

    fun getSelectedUsers(): ArrayList<Int> {
        return selectedUsers
    }

    fun getProfile(): Profile {
        return profile
    }
}