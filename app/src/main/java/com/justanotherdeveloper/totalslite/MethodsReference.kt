package com.justanotherdeveloper.totalslite

import android.content.Context
import android.graphics.Bitmap
import com.google.firebase.database.DataSnapshot

private var staticCreatePostPageForPostedProgress: CreatePostPageActivity? = null

fun setStaticCreatePostPageForPostedProgress(createPostPage: CreatePostPageActivity?) {
    staticCreatePostPageForPostedProgress = createPostPage
}

fun getStaticCreatePostPageForPostedProgress(): CreatePostPageActivity? {
    return staticCreatePostPageForPostedProgress
}

private var staticUsersPostsUpdated = true

fun staticUsersPostsUpdated(): Boolean {
    return staticUsersPostsUpdated
}

fun setStaticUsersPostsUpdated(updated: Boolean = true) {
    staticUsersPostsUpdated = updated
}

private var staticProgressPageUser: TotalsUser? = null

fun setStaticProgressPageUser(user: TotalsUser?) {
    staticProgressPageUser = user
}

fun getStaticProgressPageUser(): TotalsUser? {
    return staticProgressPageUser
}

private var staticTotalsUser: TotalsUser? = null

fun setStaticTotalsUser(totalsUser: TotalsUser?) {
    staticTotalsUser = totalsUser
}

fun getStaticTotalsUser(): TotalsUser? {
    return staticTotalsUser
}

private var bitmapPhotoToView: Bitmap? = null

fun setBitmapPhotoToView(bitmapPhoto: Bitmap?) {
    bitmapPhotoToView = bitmapPhoto
}

fun getBitmapPhotoToView(): Bitmap? {
    return bitmapPhotoToView
}

private var userGoalsData: DataSnapshot? = null

fun setStaticUserGoalsData(data: DataSnapshot?) {
    userGoalsData = data
}

fun getStaticUserGoalsData(): DataSnapshot? {
    return userGoalsData
}

private var staticProfile: Profile? = null

fun setStaticProfile(profile: Profile?) {
    staticProfile = profile
}

fun getStaticProfile(): Profile? {
    return staticProfile
}

private var staticTotals: HashMap<String, Int>? = null

fun setStaticTotals(totals: HashMap<String, Int>?) {
    staticTotals = totals
}

fun getStaticTotals(): HashMap<String, Int>? {
    return staticTotals
}

private var staticProgress: GoalProgress? = null

fun setStaticProgress(progress: GoalProgress?) {
    staticProgress = progress
}

fun getStaticProgress(): GoalProgress? {
    return staticProgress
}

private var staticGoal: Goal? = null

fun setStaticGoal(goal: Goal?) {
    staticGoal = goal
}

fun getStaticGoal(): Goal? {
    return staticGoal
}

private var phoneNumberPage: EnterPhoneNumberPageActivity? = null

fun setPhoneNumberPage(activity: EnterPhoneNumberPageActivity?) {
    phoneNumberPage = activity
}

fun getPhoneNumberPage(): EnterPhoneNumberPageActivity? {
    return phoneNumberPage
}

private var staticSignedInUser: TotalsUser? = null

fun setStaticSignedInTotalsUser(totalsUser: TotalsUser?) {
    staticSignedInUser = totalsUser
}

fun getStaticSignedInTotalsUser(): TotalsUser? {
    return staticSignedInUser
}

fun getStaticSignedInTotalsUser(context: Context, database: TinyDB): TotalsUser? {
    var totalsUser: TotalsUser? = null
    if(staticSignedInUser == null) {
        totalsUser = getSignedInTotalsUser(database)?: return null
        if(totalsUser.hasProfilePhotoUrl())
            retrieveBitmapAndSetAsStaticData(context, totalsUser, database)
        else setStaticSignedInTotalsUser(totalsUser)
    }
    return staticSignedInUser?: totalsUser
}

fun retrieveBitmapAndSetAsStaticData(context: Context,
                                     totalsUser: TotalsUser,
                                     database: TinyDB = TinyDB(context)) {
    if(!totalsUser.hasProfilePhotoUrl()) return

    context.getProfilePhoto(totalsUser, database)
}

var staticSignedOutFromBan = false

fun setSignedOutFromBan() {
    staticSignedOutFromBan = true
}

fun signedOutFromBan(): Boolean {
    val signedOutFromBan = staticSignedOutFromBan
    staticSignedOutFromBan = false
    return signedOutFromBan
}

var staticHome: HomePageActivity? = null

fun setStaticHomePage(homePage: HomePageActivity?) {
    staticHome = homePage
}

fun getStaticHomePage(): HomePageActivity? {
    return staticHome
}