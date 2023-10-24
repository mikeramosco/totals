package com.justanotherdeveloper.totalslite

import android.content.Context
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

fun stopStaticListeners() {
    stopAllStaticPostsListeners()
    stopStaticSharedProfilesListener()
    stopStaticProfileFollowersListener()
    stopStaticBannedUsersListener()
}

private var staticPostsQueries = HashMap<Int, Query>()
private var staticPostsListeners = HashMap<Int, ValueEventListener>()
private var staticUsersListeningList = ArrayList<Int>()

fun getStaticUsersListeningList(): ArrayList<Int> {
    return staticUsersListeningList
}

fun stopAllStaticPostsListeners() {
    for(userId in staticPostsQueries.keys.iterator())
        stopStaticPostsListener(userId)
    staticPostsQueries.clear()
    staticPostsListeners.clear()
    staticUsersListeningList.clear()
}

fun stopStaticPostsListener(userId: Int, deleteReferences: Boolean = false) {
    val postsQuery = staticPostsQueries[userId]
    val listener = staticPostsListeners[userId]
    if(listener != null) postsQuery?.removeEventListener(listener)
    if(deleteReferences) {
        staticPostsQueries.remove(userId)
        staticPostsListeners.remove(userId)
    }
}

fun Context.addStaticPostsListener(userId: Int, postsQuery: Query, listener: ValueEventListener) {
    if(!isSignedIn(TinyDB(this))) return
    postsQuery.addValueEventListener(listener)
    staticPostsQueries[userId] = postsQuery
    staticPostsListeners[userId] = listener
    staticUsersListeningList.add(userId)
}

private var staticBannedUsersReference: DatabaseReference? = null
private var staticBannedUsersListener: ValueEventListener? = null

fun stopStaticBannedUsersListener() {
    val listener = staticBannedUsersListener
    if(listener != null) staticBannedUsersReference?.removeEventListener(listener)
    staticBannedUsersReference = null
    staticBannedUsersListener = null
}

fun Context.setStaticBannedUsersListener(bannedUsersReference: DatabaseReference,
                                         listener: ValueEventListener
) {
    if(!isSignedIn(TinyDB(this))) return
    bannedUsersReference.addValueEventListener(listener)
    staticBannedUsersReference = bannedUsersReference
    staticBannedUsersListener = listener
}

private var staticProfileFollowersQuery: Query? = null
private var staticProfileFollowersListener: ValueEventListener? = null

fun stopStaticProfileFollowersListener() {
    val listener = staticProfileFollowersListener
    if(listener != null) staticProfileFollowersQuery?.removeEventListener(listener)
    staticProfileFollowersQuery = null
    staticProfileFollowersListener = null
}

fun Context.setStaticProfileFollowersListener(followersQuery: Query, listener: ValueEventListener) {
    if(!isSignedIn(TinyDB(this))) return
    followersQuery.addValueEventListener(listener)
    staticProfileFollowersQuery = followersQuery
    staticProfileFollowersListener = listener
}

private var staticSharedProfilesQuery: Query? = null
private var staticSharedProfilesListener: ValueEventListener? = null

fun stopStaticSharedProfilesListener() {
    val listener = staticSharedProfilesListener
    if(listener != null) staticSharedProfilesQuery?.removeEventListener(listener)
    staticSharedProfilesQuery = null
    staticSharedProfilesListener = null
}

fun Context.setStaticSharedProfilesListener(profilesQuery: Query, listener: ValueEventListener) {
    if(!isSignedIn(TinyDB(this))) return
    profilesQuery.addValueEventListener(listener)
    staticSharedProfilesQuery = profilesQuery
    staticSharedProfilesListener = listener
}