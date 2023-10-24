package com.justanotherdeveloper.totalslite

import java.util.Calendar

class Profile(private val user: TotalsUser, private val label: String) {

    private var followers = HashMap<Int, Boolean>()
    private var totalsStartDate: Calendar? = null

    fun getUser(): TotalsUser {
        return user
    }

    fun getLabel(): String {
        return label
    }

    fun getFollowers(): HashMap<Int, Boolean> {
        return followers
    }

    fun setFollowers(followers: HashMap<Int, Boolean>) {
        this.followers = followers
    }

    fun getProfileKey(): String {
        return "${user.getUserId()}_$label"
    }

    fun setTotalsStartDate(date: Calendar?) {
        totalsStartDate = date
    }

    fun getTotalsStartDate(): Calendar? {
        return totalsStartDate
    }

    fun hasTotalsStartDate(): Boolean {
        return totalsStartDate != null
    }

}