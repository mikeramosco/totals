package com.justanotherdeveloper.totalslite

import android.app.Activity
import com.google.firebase.database.DataSnapshot
import java.util.*
import kotlin.collections.HashMap

fun resetStaticMapsData() {
    staticPhotoFilenames.clear()
    staticProfilePhotoUrls.clear()
    staticNextFilenameIndex = 100000
    staticProfilesUpdating = false
    staticProfilesUpdatingQueue.clear()
    staticTotalsStartDates.clear()
    staticBannedUsers.clear()
    staticProfileFollowers.clear()
    staticUsersMap.clear()
    staticUsersAllPostsMap.clear()
    staticUsersPostsMap.clear()
    staticNewLikesMap.clear()
    staticSeenLikesMap.clear()
}

private var staticLastSavedRequestTimestamp: Long = 0
private var staticNRequestsSinceLastTimestamp = 0

fun Activity.withinRequestsLimit(): Boolean {
    val currentTimestamp = getTimeStamp()
    val secondsSinceLastTimestamp = (currentTimestamp - staticLastSavedRequestTimestamp) / 1000.0
    val minutesSinceLastTimestamp = secondsSinceLastTimestamp / 60.0
    if(minutesSinceLastTimestamp > MAX_REQUESTS_FREQUENCY_IN_MINUTES) {
        staticLastSavedRequestTimestamp = currentTimestamp
        staticNRequestsSinceLastTimestamp = 0
        return true
    }
    if(staticNRequestsSinceLastTimestamp < MAX_REQUESTS_LIMIT){
        staticNRequestsSinceLastTimestamp++
        return true
    }
    showToast(getString(R.string.tooManyRequestsError))
    return false
}

private var staticPhotoFilenames = HashMap<String, String>()
private var staticProfilePhotoUrls = HashMap<String, String>()

fun addStaticPhotoFilename(ref: String, filename: String) {
    staticPhotoFilenames[ref] = filename
}

fun getStaticPhotoFilename(ref: String): String? {
    return staticPhotoFilenames[ref]
}

fun addStaticProfilePhotoUrl(ref: String, url: String) {
    staticProfilePhotoUrls[ref] = url
}

fun getStaticProfilePhotoUrl(ref: String): String? {
    return staticProfilePhotoUrls[ref]
}

private var staticNextFilenameIndex = 100000

fun getNextFilenameIndex(): Int {
    return staticNextFilenameIndex++
}

private val staticTotalsStartDates = HashMap<Int, HashMap<String, Calendar?>>()

fun updateStaticTotalsStartDate(userId: Int, label: String, totalsStartDate: Calendar?) {
    val totalsStartDates = staticTotalsStartDates[userId]?: HashMap()
    totalsStartDates[label] = totalsStartDate
    staticTotalsStartDates[userId] = totalsStartDates
}

fun getStaticTotalsStartDate(userId: Int, label: String): Calendar? {
    return staticTotalsStartDates[userId]?.get(label)
}

private val staticProfileFollowers = HashMap<String, HashMap<Int, Boolean>>()

fun updateStaticProfileFollowers(data: DataSnapshot) {
    val dataIterator = data.children.iterator()
    while(dataIterator.hasNext()) {
        val followers = HashMap<Int, Boolean>()
        val profileData = dataIterator.next()
        val profileDataIterator = profileData.children.iterator()
        while(profileDataIterator.hasNext()) {
            val profileItemData = profileDataIterator.next()
            val key = profileItemData.key
            if(key != null && key != PROFILE_LABEL_PATH
                && key != USER_ID_PATH && key != TOTALS_START_DATE_PATH)
                followers[key.toInt()] = profileItemData.value.toString().toBoolean()
        }
        val label = profileData.child(PROFILE_LABEL_PATH).value.toString()
        staticProfileFollowers[label] = followers
    }
}

fun getStaticProfileFollowers(label: String): HashMap<Int, Boolean>? {
    return staticProfileFollowers[label]
}

private var staticUsersMap = HashMap<Int, TotalsUser>()

fun setStaticUsersMap(usersMap: HashMap<Int, TotalsUser>) {
    staticUsersMap = usersMap
}

fun getStaticUsersMap(): HashMap<Int, TotalsUser> {
    return staticUsersMap
}

private var staticUsersAllPostsMap = HashMap<Int, ArrayList<GoalProgress>>()
private var staticUsersPostsMap = HashMap<Int, HashMap<String, ArrayList<GoalProgress>>>()

fun updateUserPosts(userId: Int, userPosts: ArrayList<GoalProgress>) {
    staticUsersAllPostsMap[userId] = userPosts
}

fun getUserPosts(userId: Int): ArrayList<GoalProgress>? {
    return staticUsersAllPostsMap[userId]
}

fun updateLabeledPosts(userId: Int, labeledPostsMap: HashMap<String, ArrayList<GoalProgress>>) {
    staticUsersPostsMap[userId] = labeledPostsMap
}

fun getPostLabels(userId: Int): ArrayList<String>? {
    val labeledPostsMap = staticUsersPostsMap[userId]?: return null
    val postLabels = ArrayList<String>()
    for(label in labeledPostsMap.keys.iterator())
        postLabels.add(label)
    return postLabels
}

fun getLabeledPosts(profile: Profile): ArrayList<GoalProgress>? {
    return getLabeledPosts(profile.getUser().getUserId(), profile.getLabel())
}

fun getLabeledPosts(userId: Int, label: String): ArrayList<GoalProgress>? {
    val labeledPostsMap = staticUsersPostsMap[userId]
    return labeledPostsMap?.get(label)
}

private var staticNewLikesMap = HashMap<GoalProgress, HashMap<Int, Long>>()
private val staticSeenLikesMap = HashMap<GoalProgress, HashMap<Int, Long>>()

fun getStaticNewLikesMap(): HashMap<GoalProgress, HashMap<Int, Long>> {
    return staticNewLikesMap
}

fun getStaticSeenLikesMap(): HashMap<GoalProgress, HashMap<Int, Long>> {
    return staticSeenLikesMap
}

fun updatePostLikesMaps(signedInUserId: Int) {
    staticNewLikesMap.clear()
    staticSeenLikesMap.clear()

    val posts = getUserPosts(signedInUserId)?: return

    for(post in posts) {
        val postLikes = post.getPostLikes()
        for(userId in postLikes.keys.iterator()) {
            val timestamp = postLikes[userId]
            if(timestamp != null && userId != signedInUserId) {
                if(post.getSeenLikes().contains(userId)) {
                    val addedPostLikes = staticSeenLikesMap[post]?: HashMap()
                    addedPostLikes[userId] = timestamp
                    staticSeenLikesMap[post] = addedPostLikes
                } else {
                    val addedPostLikes = staticNewLikesMap[post]?: HashMap()
                    addedPostLikes[userId] = timestamp
                    staticNewLikesMap[post] = addedPostLikes
                }
            }
        }
    }
}

private var staticProfilesUpdating = false
private var staticProfilesUpdatingQueue = ArrayList<DataSnapshot>()

fun addToStaticSharedProfilesUpdatingQueue(sharedPage: SharedPageFragment, data: DataSnapshot) {
    staticProfilesUpdatingQueue.add(data)
    if(!staticProfilesUpdating) {
        staticProfilesUpdating = true
        while(staticProfilesUpdatingQueue.size > 0) {
            val nextData = staticProfilesUpdatingQueue[0]
            staticProfilesUpdatingQueue.removeAt(0)
            sharedPage.getFragmentView().updateSharedProfiles(nextData)
        }
        staticProfilesUpdating = false
    }
}

private var staticBannedUsers = ArrayList<Int>()

fun updateStaticBannedUsers(data: DataSnapshot) {
    val updatedBannedUsers = ArrayList<Int>()
    val dataIterator = data.children.iterator()
    while(dataIterator.hasNext())
        updatedBannedUsers.add(
            dataIterator.next().key.toString().toInt())
    staticBannedUsers = updatedBannedUsers
}

fun TotalsUser.isBanned(): Boolean {
    return staticBannedUsers.contains(getUserId())
}

fun getStaticBannedUsers(): ArrayList<Int> {
    return staticBannedUsers
}