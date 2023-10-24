package com.justanotherdeveloper.totalslite

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.justanotherdeveloper.totalslite.databinding.FragmentSharedPageBinding

class SharedPageFragment(private val activity: HomePageActivity) : Fragment() {

    private lateinit var view: SharedPageView
    private lateinit var listeners: SharedPageListeners
    private lateinit var dialogs: SharedPageDialogs

    private lateinit var profiles: ProfileSender

    private lateinit var accounts: TotalsAccountEditor

    private var profileOpened = false

    private var otherProcessStarted = false

    private var _binding: FragmentSharedPageBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedPageBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityCreated(savedInstanceState)",
        "androidx.fragment.app.Fragment"))
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view = SharedPageView(activity, this)
        listeners = SharedPageListeners(activity, this)
        dialogs = SharedPageDialogs(activity, this)

        profiles = ProfileSender()
        accounts = TotalsAccountEditor()

        activity.initSharedProfilesListener(this)

        activity.setSharedFragmentReady()
    }

    fun updateNotificationsIfProfileOpened() {
        if(profileOpened) view.updateProfileNotifications()
        profileOpened = false
    }

    fun openProgressPage(profile: Profile) {
        if(otherProcessStarted()) return
        profileOpened = true
        view.updateSeenPosts(profile)
        val intent = Intent(activity, ProgressPageActivity::class.java)
        intent.putExtra(SELECTED_LABEL_REF, profile.getLabel())
        setStaticProgressPageUser(profile.getUser())
        startActivityForResult(intent, 0)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    fun getFragmentBinding(): FragmentSharedPageBinding {
        return binding
    }

    fun getFragmentView(): SharedPageView {
        return view
    }

    fun getFragmentListeners(): SharedPageListeners {
        return listeners
    }

    fun getFragmentDialogs(): SharedPageDialogs {
        return dialogs
    }

    fun getProfiles(): ProfileSender {
        return profiles
    }

    fun getAccounts(): TotalsAccountEditor {
        return accounts
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