package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.content.Context
import com.google.firebase.database.DataSnapshot
import java.util.ArrayList
import java.util.Calendar

fun dueDateTimeIsUp(goal: Goal, dueDate: Calendar): Boolean {
    val currentTimeDate = getTodaysDate()
    val timeDue = dueDate.dateClone()
    timeDue.add(Calendar.DATE, 1)
    timeDue.set(Calendar.MILLISECOND, 0)
    timeDue.set(Calendar.SECOND, 0)
    timeDue.set(Calendar.HOUR_OF_DAY, goal.getHour())
    timeDue.set(Calendar.MINUTE, goal.getMinute())
    return currentTimeDate.comesAfter(timeDue)
}

fun setGoalLabels(local: TinyDB, labels: ArrayList<String>) {
    local.putListString(GOAL_LABELS_REF, labels)
}

fun updateGoalLabels(goal: Goal, local: TinyDB) {
    val goalLabels = local.getListString(GOAL_LABELS_REF)?: ArrayList()
    var labelAdded = false
    for(label in goal.getLabels())
        if(!goalLabels.contains(label)) {
            goalLabels.add(label)
            labelAdded = true
        }
    if(labelAdded) local.putListString(GOAL_LABELS_REF, goalLabels)
}

fun setMissedDueTimeAction(local: TinyDB, actionString: String = SET_GOAL_INACTIVE_REF) {
    local.putString(ACTION_FOR_MISSED_DUE_TIME_REF, actionString)
}

fun getMissedDueTimeAction(local: TinyDB): String {
    return local.getString(ACTION_FOR_MISSED_DUE_TIME_REF).ifEmpty { SET_GOAL_INACTIVE_REF }
}

fun Profile.toProfileSeenPostsRefKey(): String {
    return "$PROFILE_SEEN_POSTS_LABEL${getProfileKey()}"
}

fun getProfileSeenPosts(profile: Profile, local: TinyDB): ArrayList<String>? {
    return local.getListString(profile.toProfileSeenPostsRefKey()).ifEmpty { null }
}

fun setProfileSeenPosts(profile: Profile, local: TinyDB, seenPosts: ArrayList<String>) {
    updateProfileNotifDate(profile, local)
    local.putListString(profile.toProfileSeenPostsRefKey(), seenPosts)
}

fun Profile.toProfileNotifDateRefKey(): String {
    return "$PROFILE_NOTIF_DATE_LABEL${getProfileKey()}"
}

fun getProfileNotifDate(profile: Profile, local: TinyDB): Calendar? {
    val timestamp = local.getLong(profile.toProfileNotifDateRefKey())
    if(timestamp == (0).toLong()) return null
    return getCalendar(timestamp)
}

fun updateProfileNotifDate(profile: Profile, local: TinyDB) {
    local.putLong(profile.toProfileNotifDateRefKey(), getTimeStamp())
}

fun setSelectedLabelForAppLaunch(label: String, local: TinyDB) {
    local.putString(SELECTED_LABEL_REF, label)
    if(label.isNotEmpty())
        local.putString(RECENT_GOAL_LABEL_REF, label)
}

fun getSelectedLabelForAppLaunch(local: TinyDB): String {
    return local.getString(SELECTED_LABEL_REF)
}

@Suppress("UNCHECKED_CAST")
fun getSavedUsersMap(local: TinyDB): HashMap<Int, TotalsUser> {
    val usersList = local.getListObject(USERS_LIST_REF, TotalsUser::class.java) as ArrayList<TotalsUser>
    val usersMap = HashMap<Int, TotalsUser>()
    for(user in usersList) usersMap[user.getUserId()] = user
    return usersMap
}

@Suppress("UNCHECKED_CAST")
fun saveUsersMap(local: TinyDB, usersMap: HashMap<Int, TotalsUser>) {
    val usersList = ArrayList<TotalsUser>()
    for(user in usersMap.values.iterator()) usersList.add(user)
    local.putListObject(USERS_LIST_REF, usersList as ArrayList<Any>)
}

fun getLabels(local: TinyDB): ArrayList<String> {
    val labels = local.getListString(GOAL_LABELS_REF)
    return labels?: ArrayList()
}

fun parseUserPostsData(data: DataSnapshot): ArrayList<GoalProgress> {
    val userPosts = ArrayList<GoalProgress>()
    val dataIterator = data.children.iterator()
    while(dataIterator.hasNext()) {
        val postData = dataIterator.next()
        val progress = postData.asProgress()
        if(progress != null) userPosts.add(progress)
    }
    return userPosts
}

fun DataSnapshot.asProgress(): GoalProgress? {
    val progress = GoalProgress()
    return if(progress.parsePostData(this))
        progress else null
}

fun createLabeledPostsMap(userPosts: ArrayList<GoalProgress>):
        HashMap<String, ArrayList<GoalProgress>> {
    val labeledPostsMap = HashMap<String, ArrayList<GoalProgress>>()
    for(post in userPosts) {
        for(label in post.getLabels()) {
            if(!labeledPostsMap.containsKey(label))
                labeledPostsMap[label] = ArrayList()
            labeledPostsMap[label]?.add(post)
        }
    }
    return labeledPostsMap
}

fun Activity.getDateTextString(goal: Goal, forPost: Boolean = false,
                               dueDate: Calendar? = null): String {
    val date = goal.getDate()
    val dateString = if(forPost && dueDate != null) {
        getGoalDateString(dueDate, true)
    } else {
        if (goal.hasDate() && date != null)
            getGoalDateString(date, forPost)
        else goal.getRepeatingDaysString(this)
    }
    val timeString = getTimeText(goal.getHour(), goal.getMinute())
    val stringCode =
        if(forPost) R.string.postViewDateTextString
        else R.string.goalViewDateTextString
    return getString(stringCode, dateString, timeString)
}

private fun Activity.getGoalDateString(date: Calendar, forPost: Boolean): String {
    return if(forPost) getDateText(date) else getRecencyText(date)
}

fun Activity.getPostedTextString(post: GoalProgress,
                                 timestamp: Long = post.getTimestampPosted()): String {
    val postedDate = getCalendar(timestamp)
    val hour = postedDate.get(Calendar.HOUR_OF_DAY)
    val minute = postedDate.get(Calendar.MINUTE)
    val recencyText = getRecencyText(postedDate)
    val timeString = getTimeText(hour, minute)
    return getString(R.string.postViewPostedTextString, recencyText, timeString)
}