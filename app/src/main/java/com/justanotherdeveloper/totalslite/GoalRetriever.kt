package com.justanotherdeveloper.totalslite

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GoalRetriever(private val fragment: MePageFragment) {

    fun retrieveUserGoals() {
        val user = getStaticSignedInTotalsUser()?: return
        val fb = FirebaseDatabase.getInstance().reference
        val goalsTable = fb.child(GOALS_PATH)
        val goalsQuery = goalsTable.orderByChild(USER_ID_PATH).equalTo(user.getUserId().toDouble())
        goalsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                setStaticUserGoalsData(data)
                fragment.getUserGoalsData()
            }

            override fun onCancelled(error: DatabaseError) {
                fragment.activity?.showRequestUnavailableToast()
            }
        })
    }

    fun deleteGoal(goal: Goal) {
        val fb = FirebaseDatabase.getInstance().reference
        val goalsTable = fb.child(GOALS_PATH)
        goalsTable.child(goal.getGoalFirebaseKey()).setValue(null)
    }

}