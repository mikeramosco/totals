package com.justanotherdeveloper.totalslite

import com.google.firebase.database.DataSnapshot
import java.lang.NullPointerException

fun isSignedIn(database: TinyDB): Boolean {
    return getSignedInUserId(database) != 0
}

fun getSignedInUserId(database: TinyDB): Int {
    return getSignedInTotalsUser(database)?.getUserId()?: 0
}

fun signOutTotalsUser(database: TinyDB) {
    database.remove(SIGNED_IN_TOTALS_USER_REF)
    setGoalLabels(database, ArrayList())
    setMissedDueTimeAction(database)
    setStaticSignedInTotalsUser(null)
    setStaticHomePage(null)
    stopStaticListeners()
    resetStaticMapsData()
}

fun setSignedInTotalsUser(database: TinyDB, totalsUser: TotalsUser, isSigningIn: Boolean = false) {
    if(!isSigningIn && !isSignedIn(database)) return
    setStaticSignedInTotalsUser(totalsUser)
    database.putObject(SIGNED_IN_TOTALS_USER_REF, totalsUser.createCloneWithoutBitmap())
}

fun getSignedInTotalsUser(database: TinyDB): TotalsUser? {
    var totalsUser: TotalsUser? = null
    try { totalsUser = database.getObject(SIGNED_IN_TOTALS_USER_REF, TotalsUser::class.java)
    } catch (_: NullPointerException) { }
    val staticTotalsUser = getStaticSignedInTotalsUser()
    if(staticTotalsUser != null) {
        totalsUser?.setProfilePhotoBitmap(
            staticTotalsUser.getProfilePhotoBitmap())
    }
    return totalsUser
}

fun DataSnapshot.toTotalsUser(): TotalsUser {
    val userId = child(USER_ID_PATH).value.toString().toInt()
    val username = child(USERNAME_PATH).value.toString()
    val name = child(NAME_PATH).value.toString()
    val phoneNumber = child(PHONE_NUMBER_PATH).value.toString()
    val profilePhotoUrlValue = child(PROFILE_PHOTO_URL_PATH).value
    val profileBioValue = child(PROFILE_BIO_PATH).value
    val totalsUser = TotalsUser(key.toString(), userId, username, name)
    totalsUser.setPhoneNumber(phoneNumber)
    if(profilePhotoUrlValue != null)
        totalsUser.setProfilePhotoUrl(profilePhotoUrlValue.toString())
    if(profileBioValue != null)
        totalsUser.setProfileBio(profileBioValue.toString())
    val blockedUsersDataIterator = child(BLOCKED_USERS_PATH).children.iterator()
    while(blockedUsersDataIterator.hasNext())
        totalsUser.getBlockedUsers().add(
            blockedUsersDataIterator.next().key.toString().toInt())
    return totalsUser
}