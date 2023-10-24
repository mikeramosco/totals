package com.justanotherdeveloper.totalslite

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.justanotherdeveloper.totalslite.databinding.ActivityHomePageBinding

class HomePageActivity : AppCompatActivity() {

    /** fragments of home pages */
    private lateinit var settingsPageFragment: SettingsPageFragment
    private lateinit var sharedPageFragment: SharedPageFragment
    private lateinit var mePageFragment: MePageFragment

    /** 'binding' allows reference calls to activity views */
    private lateinit var binding: ActivityHomePageBinding

    private lateinit var local: TinyDB

    private lateinit var view: HomePageView
    private lateinit var listeners: HomePageListeners

    private var photos: PhotoDatabase? = null

    private var activityPaused = false

    private var sharedFragmentReady = false
    private var meFragmentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()

        local = TinyDB(this)

        if(!isSignedIn(local)) { finish(); return }

        view = HomePageView(this)
        listeners = HomePageListeners(this)

        setStaticHomePage(this)
        initBannedUsersListener()
        initFollowersListener()
        initPostsListener(getSignedInUserId(local), true)
        initUsersMap()

        binding.profilePhotoLayout.visibility = View.VISIBLE

        photos = PhotoDatabase(this)
        view.initProfileIcon()

        mePageFragment = MePageFragment(this)
        settingsPageFragment = SettingsPageFragment(this)
        sharedPageFragment = SharedPageFragment(this)

        setupFragments()
    }

    private fun initBinding() {
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun displayFragmentContainer(displaySettings: Boolean = false,
                                 displayShared: Boolean = false) {
        var settingsVisibility = View.GONE
        var sharedVisibility = View.GONE
        var meVisibility = View.GONE

        var settingsRes = R.drawable.ic_settings_gray
        var sharedRes = R.drawable.ic_shared_gray

        when {
            displaySettings -> {
                settingsVisibility = View.VISIBLE
                settingsRes = R.drawable.ic_settings_selected
            }
            displayShared -> {
                sharedVisibility = View.VISIBLE
                sharedRes = R.drawable.ic_shared_selected
            }
            else -> meVisibility = View.VISIBLE
        }

        binding.settingsFragmentContainer.visibility = settingsVisibility
        binding.sharedFragmentContainer.visibility = sharedVisibility
        binding.meFragmentContainer.visibility = meVisibility

        binding.settingsIcon.setImageResource(settingsRes)
        binding.sharedIcon.setImageResource(sharedRes)
        binding.profileNavBackground.visibility = meVisibility

        if(meFragmentReady)
            if(binding.meFragmentContainer.isVisible)
                mePageFragment.refreshGoalProgressDetails()
    }

    fun setSharedFragmentReady() {
        sharedFragmentReady = true
    }

    fun setMeFragmentReady() {
        meFragmentReady = true
    }

    fun logoutBannedUser() {
        signOutTotalsUser(local)
        setSignedOutFromBan()
        finish()
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction().replace(
            R.id.settingsFragmentContainer, settingsPageFragment).commit()
        supportFragmentManager.beginTransaction().replace(
            R.id.sharedFragmentContainer, sharedPageFragment).commit()
        supportFragmentManager.beginTransaction().replace(
            R.id.meFragmentContainer, mePageFragment).commit()
    }

    private fun setOtherProcessesEnded() {
        mePageFragment.setOtherProcessStarted(false)
        sharedPageFragment.setOtherProcessStarted(false)
        settingsPageFragment.setOtherProcessStarted(false)
    }

    /** Override Methods */

    /** minimizes app when back pressed */
    @Deprecated("Deprecated in Java", ReplaceWith("goToHomeScreen()"))
    override fun onBackPressed() {
        goToHomeScreen()
    }

    override fun onResume() {
        super.onResume()
        if(!isSignedIn(local)) finish()
        else if(activityPaused) {
            activityPaused = false
            setOtherProcessesEnded()
            mePageFragment.refreshGoalProgressDetails()
        }
    }

    override fun onPause() {
        super.onPause()
        activityPaused = true
    }

    fun refreshGoalsPage() {
        if(meFragmentReady)
            mePageFragment.refreshGoalProgressDetails()
    }

    fun updateSharedProfiles() {
        if(sharedFragmentReady)
            sharedPageFragment.getFragmentView().updateSharedProfiles()
    }

    fun updateSharedPageNotifications() {
        if(sharedFragmentReady)
            sharedPageFragment.getFragmentView().updateProfileNotifications()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(!isSignedIn(local)) return
        activityPaused = false
        setOtherProcessesEnded()
        sharedPageFragment.updateNotificationsIfProfileOpened()
        val profileInfoUpdated = data?.getBooleanExtra(
            PROFILE_INFO_UPDATED_REF, false) ?: false
        if (profileInfoUpdated) {
            settingsPageFragment.displayProfileInfo()
            view.initProfileIcon()
        }
        val goalAdded = data?.getBooleanExtra(
            GOAL_ADDED_REF, false) ?: false
        if (goalAdded) {
            mePageFragment.getFragmentView().addNewlyAddedGoal()
            mePageFragment.setSelectedLabel()
        }
        val goalUpdated = data?.getBooleanExtra(
            GOAL_UPDATED_REF, false) ?: false
        if (goalUpdated) {
            mePageFragment.getFragmentView().updateGoal()
            mePageFragment.setSelectedLabel()
        } else {
            setStaticGoal(null)
            mePageFragment.setSelectedGoalView(null)
        }
        val progressPosted = data?.getBooleanExtra(
            PROGRESS_POSTED_REF, false)?: false
        val selectedLabel = data?.getStringExtra(SELECTED_LABEL_REF)
        if(selectedLabel != null) {
            mePageFragment.setSelectedLabel(selectedLabel,
                selectOnlyIfNoneSelected = progressPosted)
            val forProgressPage = data.getBooleanExtra(FOR_PROGRESS_PAGE_REF, false)
            if(forProgressPage) mePageFragment.openProgressPage()
            else if(progressPosted) mePageFragment.openProgressPage(
                selectedLabel, true)
        }
        val toUpdatePost = data?.getBooleanExtra(TO_UPDATE_POST_REF, false)?: false
        if(toUpdatePost) mePageFragment.updateIncompletePost()
        val startGoalLikePost = data?.getBooleanExtra(START_GOAL_LIKE_POST_REF, false)?: false
        if(startGoalLikePost) {
            displayFragmentContainer()
            mePageFragment.startGoalLikePost()
        }
        mePageFragment.refreshGoalProgressDetails()
    }

    /** "Get" Methods */

    fun getBinding(): ActivityHomePageBinding {
        return binding
    }

    fun getDatabase(): TinyDB {
        return local
    }

    fun getPhotos(): PhotoDatabase? {
        return photos
    }

}