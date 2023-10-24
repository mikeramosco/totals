package com.justanotherdeveloper.totalslite

import android.content.Context
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.ArrayList

fun Context.initUsersMap() {
    val local = TinyDB(this)
    val savedUsersMap = getSavedUsersMap(local)
    val savedUsersMapExists = savedUsersMap.isNotEmpty()
    if(savedUsersMapExists) setStaticUsersMap(savedUsersMap)
    val signedInUserId = getSignedInUserId(local)
    val fb = FirebaseDatabase.getInstance().reference
    val usersTable = fb.child(USERS_PATH)
    usersTable.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(data: DataSnapshot) {
            val dataIterator = data.children.iterator()
            val usersMap = HashMap<Int, TotalsUser>()
            while(dataIterator.hasNext()) {
                val user = dataIterator.next().toTotalsUser()
                usersMap[user.getUserId()] = user
                if(user.getUserId() == signedInUserId)
                    setSignedInTotalsUser(local, user)
            }
            saveUsersMap(local, usersMap)
            setStaticUsersMap(usersMap)
        }

        override fun onCancelled(error: DatabaseError) {
            showRequestUnavailableToast()
        }
    })
}

fun Context.initBannedUsersListener() {
    val fb = FirebaseDatabase.getInstance().reference
    val bannedUsersTable = fb.child(BANNED_PATH)
    val context = this

    val listener = object : ValueEventListener {
        override fun onDataChange(data: DataSnapshot) {
            updateStaticBannedUsers(data)
            val signedInUser = getStaticSignedInTotalsUser(context, TinyDB(context))
            if(signedInUser != null && signedInUser.isBanned()) {
                getStaticHomePage()?.logoutBannedUser()
            } else getStaticHomePage()?.updateSharedProfiles()
        }

        override fun onCancelled(error: DatabaseError) {
            showRequestUnavailableToast()
        }
    }
    setStaticBannedUsersListener(bannedUsersTable, listener)
}

fun Context.initFollowersListener() {
    val userId = getSignedInUserId(TinyDB(this))
    val fb = FirebaseDatabase.getInstance().reference
    val profilesTable = fb.child(PROFILES_PATH)
    val followersQuery = profilesTable.orderByChild(USER_ID_PATH).equalTo(userId.toDouble())

    val listener = object : ValueEventListener {
        override fun onDataChange(data: DataSnapshot) {
            updateStaticProfileFollowers(data)
            val dataIterator = data.children.iterator()
            while(dataIterator.hasNext()) {
                val profileData = dataIterator.next()
                val totalsStartDateTimestampValue = profileData.child(TOTALS_START_DATE_PATH).value
                val label = profileData.child(PROFILE_LABEL_PATH).value.toString()
                val totalsStartDate = if(totalsStartDateTimestampValue != null)
                    getCalendar(totalsStartDateTimestampValue.toString().toLong()) else null
                updateStaticTotalsStartDate(userId, label, totalsStartDate)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            showRequestUnavailableToast()
        }
    }
    setStaticProfileFollowersListener(followersQuery, listener)
}

fun Context.initSharedProfilesListener(sharedPage: SharedPageFragment) {
    val userId = getSignedInUserId(TinyDB(this))
    val fb = FirebaseDatabase.getInstance().reference
    val profilesTable = fb.child(PROFILES_PATH)
    val profilesQuery = profilesTable.orderByChild(userId.toString())

    val listener = object : ValueEventListener {
        override fun onDataChange(data: DataSnapshot) {
            addToStaticSharedProfilesUpdatingQueue(sharedPage, data)
        }

        override fun onCancelled(error: DatabaseError) {
            showRequestUnavailableToast()
        }
    }
    setStaticSharedProfilesListener(profilesQuery, listener)
}

fun Context.initPostsListener(userId: Int, isSignedUser: Boolean = false) {
    val fb = FirebaseDatabase.getInstance().reference
    val postsTable = fb.child(POSTS_PATH)
    val postsQuery = postsTable.orderByChild(USER_ID_PATH).equalTo(userId.toDouble())
    val local = TinyDB(this)

    fun confirmProgressPostedIfNewDataForSignedInUser() {
        if(isSignedUser && !staticUsersPostsUpdated()) {
            setStaticUsersPostsUpdated()
            getStaticCreatePostPageForPostedProgress()
                ?.confirmProgressPostedSuccess()
            setStaticCreatePostPageForPostedProgress(null)
        }
    }

    val listener = object : ValueEventListener {
        override fun onDataChange(data: DataSnapshot) {
            val userPosts = parseUserPostsData(data)
            if(isSignedUser) {
                updateUserPosts(userId, userPosts)
//                for(post in userPosts)
//                    updateGoalLabels(post, local)
                updatePostLikesMaps(userId)
                getStaticHomePage()?.refreshGoalsPage()
            }
            val labeledPostsMap = createLabeledPostsMap(userPosts)
            updateLabeledPosts(userId, labeledPostsMap)
            confirmProgressPostedIfNewDataForSignedInUser()
            getStaticHomePage()?.updateSharedPageNotifications()
        }

        override fun onCancelled(error: DatabaseError) {
            showRequestUnavailableToast()
            confirmProgressPostedIfNewDataForSignedInUser()
        }
    }

    addStaticPostsListener(userId, postsQuery, listener)
}

fun getRepeatingDaysFirebaseValue(repeatingDays: ArrayList<Int>): String {
    var repeatingDaysString = ""
    var firstAdded = false
    for(day in repeatingDays) {
        if(firstAdded) repeatingDaysString += "\t"
        else firstAdded = true
        repeatingDaysString += day.toString()
    }
    return repeatingDaysString
}

fun getRepeatingDaysFromFirebaseValue(repeatingDaysString: String): ArrayList<Int> {
    val repeatingDays = ArrayList<Int>()
    val repeatingDaysContents = repeatingDaysString.split("\t")
    for(day in repeatingDaysContents) repeatingDays.add(day.toInt())
    return repeatingDays
}