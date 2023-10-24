package com.justanotherdeveloper.totalslite

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.DataSnapshot
import com.justanotherdeveloper.totalslite.databinding.FragmentMePageBinding
import java.util.Calendar

class MePageFragment(private val activity: HomePageActivity) : Fragment() {

    private lateinit var listeners: MePageListeners
    private lateinit var view: MePageView
    private lateinit var dialogs: MePageDialogs
    private lateinit var local: TinyDB

    private lateinit var uploader: GoalUploader
    private lateinit var retriever: GoalRetriever

    private val viewGoalMap = HashMap<View, Goal>()
    private val viewDueNowGoalMap = HashMap<View, Goal>()
    private val dueDateMap = HashMap<Goal, Long>()
    private val goalCompletedAmounts = HashMap<Goal, Int>()
    private val dueNowGoalViewsMap = HashMap<View, View>()

    private val viewsToDeleteFromMap = ArrayList<View>()

    private var selectedLabel = ""
    private var selectedGoalView: View? = null

    private var incompletePost: GoalProgress? = null

    private var amountCompleted = 0
    private var postCounter = 0

    private var postingInProgress = false
    private var isRefreshing = false

    private var otherProcessStarted = false

    private var _binding: FragmentMePageBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMePageBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onActivityCreated(savedInstanceState)",
        "androidx.fragment.app.Fragment"))
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        listeners = MePageListeners(activity, this)
        view = MePageView(activity, this)
        dialogs = MePageDialogs(activity, this)

        uploader = GoalUploader(goalPage = this)
        retriever = GoalRetriever(this)
        retriever.retrieveUserGoals()

        local = TinyDB(activity)

        setSavedSelectedLabel()

        activity.setMeFragmentReady()
    }

    private fun setSavedSelectedLabel() {
        val selectedLabel = getSelectedLabelForAppLaunch(local)
        val labels = getLabels(local)
        if(labels.contains(selectedLabel) && selectedLabel.isNotEmpty())
            setSelectedLabel(selectedLabel, false)
    }

    fun setSelectedLabel(selectedLabel: String = this.selectedLabel, saveLabel: Boolean = true,
                         selectOnlyIfNoneSelected: Boolean = false) {
        if(selectOnlyIfNoneSelected && this.selectedLabel.isNotEmpty()) return
        this.selectedLabel = selectedLabel
        view.filterGoalViews(selectedLabel)
        if(saveLabel) setSelectedLabelForAppLaunch(selectedLabel, local)
    }

    fun getUserGoalsData() {
        val userGoalsData = getStaticUserGoalsData() ?: return
        setStaticUserGoalsData(null)
        val dataIterator = userGoalsData.children.iterator()
        val goals = ArrayList<Goal>()
        val updatedLabels = getPostLabels(getSignedInUserId(local))?: ArrayList()
        while(dataIterator.hasNext()) {
            val goal = dataIterator.next().asGoal()
            if(goal != null) {
                for(label in goal.getLabels())
                    if(!updatedLabels.contains(label))
                        updatedLabels.add(label)
                goals.add(goal)
            }
        }
        beginTransition(binding.mePageParent)
        for(goal in goals) view.addGoalView(goal, false)
        setGoalLabels(local, updatedLabels)
        for(goalView in viewGoalMap.keys.iterator())
            if(isActiveAndDueNow(goalView)) view.addActiveGoalView(goalView)
        setSavedSelectedLabel()
        view.updateGoalsPageNotifications()
    }

    fun isActiveAndDueNow(goalView: View): Boolean {
        val goal = viewGoalMap[goalView]
        val dueDateTimestamp = dueDateMap[goal]
        if(goal != null && dueDateTimestamp != null) {
            if(!goal.isActive()) return false
            val dueDate = getCalendar(dueDateTimestamp)
            return dueDate.comesBefore(getTodaysDate())
        }
        return false
    }

    private fun DataSnapshot.asGoal(): Goal? {
        val goal = Goal()
        return if(goal.parseGoalData(this))
            goal else null
    }

    fun createIncompletePost(goal: Goal, isNotRequired: Boolean = false) {
        incompletePost = goal.createGoalProgress()
        incompletePost?.setAsIncomplete()
        val originalAmount = goal.getAmount()
        val amountCompleted = goalCompletedAmounts[goal]
        if(originalAmount > 0 && amountCompleted != null) {
            val amountRemaining = originalAmount - amountCompleted
            incompletePost?.setOriginalAmount(originalAmount)
            incompletePost?.setAmount(amountRemaining)
        }
        if(isNotRequired) incompletePost?.setAsNotRequired()
    }

    fun clearIncompletePost() {
        incompletePost = null
    }

    fun postIncompleteGoal(confirmMarkAsIncompleteDialog: AlertDialog? = null,
                           disabledButton: LinearLayout? = null,
                           markIncompleteButton: LinearLayout? = null,
                           cancelButton: LinearLayout? = null,
                           goalView: View? = null, goal: Goal? = null) {
        val updatedGoal = goal?.createClone()
        updatedGoal?.setActive()
        uploader.postIncompleteGoal(incompletePost,
            confirmMarkAsIncompleteDialog, disabledButton,
            markIncompleteButton, cancelButton,
            goalView = goalView, updatedGoal = updatedGoal)
    }

    fun updateIncompletePost() {
        val incompletePost = getStaticProgress()?: return
        setStaticProgress(null)
        this.incompletePost = incompletePost
    }

    fun manageAttachmentsForIncompletePost() {
        if(otherProcessStarted()) return
        val intent = Intent(activity, SeeAttachmentsActivity::class.java)
        intent.putExtra(MANAGE_ATTACHMENTS_REF, true)
        setStaticProgress(incompletePost)
        startActivityForResult(intent, 0)
    }

    fun manageLabelsForIncompletePost() {
        if(otherProcessStarted()) return
        val intent = Intent(activity, SelectLabelActivity::class.java)
        intent.putExtra(TO_UPDATE_POST_REF, true)
        setStaticProgress(incompletePost)
        startActivityForResult(intent, 0)
    }

    fun openLikeNotifsPage() {
        if(otherProcessStarted()) return
        val intent = Intent(activity, UsersDisplayPageActivity::class.java)
        intent.putExtra(USERS_TO_DISPLAY_REF, FOR_POST_LIKES_NOTIFS_REF)
        intent.putExtra(SELECTED_LABEL_REF, selectedLabel)
        startActivityForResult(intent, 0)
    }

    fun openProgressPage(selectedLabel: String = this.selectedLabel,
                         selectOnlyIfNoneSelected: Boolean = false) {
        if(selectedLabel.isEmpty())
            openSelectLabelPage(true)
        else {
            if(otherProcessStarted()) return
            val intent = Intent(activity, ProgressPageActivity::class.java)
            val profileToOpen = if(selectOnlyIfNoneSelected)
                this.selectedLabel.ifEmpty { selectedLabel }
            else selectedLabel
            intent.putExtra(SELECTED_LABEL_REF, profileToOpen)
            setStaticProgressPageUser(getSignedInTotalsUser(local))
            startActivityForResult(intent, 0)
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    fun openCreatePostPage(dueDateTimestamp: Long?, amountCompleted: Int?) {
        if(otherProcessStarted()) return
        if(dueDateTimestamp == null) return
        val intent = Intent(activity, CreatePostPageActivity::class.java)
        if(amountCompleted != null)
            intent.putExtra(AMOUNT_COMPLETED_REF, amountCompleted)
        intent.putExtra(DUE_DATE_REF, dueDateTimestamp)
        startActivityForResult(intent, 0)
    }

    fun openSelectLabelPage(forProgressPage: Boolean = false) {
        if(otherProcessStarted()) return
        val intent = Intent(activity, SelectLabelActivity::class.java)
        intent.putExtra(SELECTED_LABEL_REF, selectedLabel)
        intent.putExtra(FOR_PROGRESS_PAGE_REF, forProgressPage)
        val labelsWithNotifs = getLabelsWithNotifs()
        if(labelsWithNotifs.isNotEmpty())
            intent.putStringArrayListExtra(LABELS_WITH_NOTIFS_REF, labelsWithNotifs)
        startActivityForResult(intent, 0)
    }

    private fun getLabelsWithNotifs(): ArrayList<String> {
        val labelsWithNotifs = ArrayList<String>()

        fun addLabelIfNotYetAdded(label: String) {
            if(!labelsWithNotifs.contains(label))
                labelsWithNotifs.add(label)
        }

        for(goal in viewDueNowGoalMap.values.iterator())
            for(label in goal.getLabels())
                addLabelIfNotYetAdded(label)
        return labelsWithNotifs
    }

    fun openAddGoalPage() {
        if(otherProcessStarted()) return
        val intent = Intent(activity, EditActivityPageActivity::class.java)
        intent.putExtra(NEW_GOAL_REF, true)
        startActivityForResult(intent, 0)
    }

    fun openEditGoalPage(amountCompleted: Int?, goalCompleted: Boolean?) {
        val intent = Intent(activity, EditActivityPageActivity::class.java)
        if(amountCompleted != null)
            intent.putExtra(AMOUNT_COMPLETED_REF, amountCompleted)
        if(goalCompleted != null)
            intent.putExtra(GOAL_COMPLETED_REF, goalCompleted)
        startActivityForResult(intent, 0)
    }

    fun startGoalLikePost() {
        val intent = Intent(activity, EditActivityPageActivity::class.java)
        intent.putExtra(START_GOAL_LIKE_POST_REF, true)
        startActivityForResult(intent, 0)
    }

    fun deleteGoal(goalView: View) {
        val goal = viewGoalMap[goalView]
        if(goal != null) retriever.deleteGoal(goal)
        view.deleteGoalView(goalView)
        val activeGoalView = dueNowGoalViewsMap[goalView]
        if(activeGoalView != null) view.removeActiveGoalView(activeGoalView)
        removeFromMaps(goalView, activeGoalView)
        deleteViewsFromMap()
    }

    fun removeFromMaps(goalView: View, activeGoalView: View? = null,
                       forActiveGoalOnly: Boolean = false,
                       deleteGoalView: Boolean = true) {
        if(activeGoalView != null) {
            viewDueNowGoalMap.remove(activeGoalView)
            dueNowGoalViewsMap.remove(goalView)
        }

        if(forActiveGoalOnly) return

        val goal = viewGoalMap[goalView]
        if(deleteGoalView)
            viewsToDeleteFromMap.add(goalView)
        if (goal != null) {
            dueDateMap.remove(goal)
            goalCompletedAmounts.remove(goal)
        }
    }

    fun searchPostsForSingleDateGoal(goal: Goal): Boolean {
        val allUserPosts = getUserPosts(getSignedInUserId(local))?: return false
        amountCompleted = 0
        postCounter = 0
        var lastPostOfDueDateFound = false
        for(post in allUserPosts) {
            if(post.getGoalFirebaseKey() == goal.getGoalFirebaseKey()) {
                postCounter++
                if(!post.isIncomplete())
                    amountCompleted += post.getAmount()
                if(post.isLastPostOfDueDate()) lastPostOfDueDateFound = true
                else if(post.isNotRequired()) postCounter--
            }
        }
        return lastPostOfDueDateFound
    }

    fun performMissedDueTimeAction(goalView: View, goal: Goal, previousDueDate: Calendar) {
        if(goal.isActive()) {
            when(getMissedDueTimeAction(local)) {
                SET_GOAL_INACTIVE_REF -> {
                    if(goal.isRepeating()) {
                        val inactiveGoal = goal.createClone()
                        inactiveGoal.setActive(false)
                        view.updateGoal(goalView, inactiveGoal)
                        refreshGoalProgressDetails()
                        uploader.setGoalActive(inactiveGoal, false)
                    }
                }
                AUTO_POST_INCOMPLETE_REF -> {
                    createIncompletePost(goal)
                    incompletePost?.setDueDate(previousDueDate)
                    val updatedGoal = goal.createClone()
                    updatedGoal.setActive()
                    uploader.postIncompleteGoal(incompletePost,
                        goalView = goalView, updatedGoal = updatedGoal)
                }
            }
        }
    }

    fun calculateNextDueDate(goalView: View, goal: Goal): Calendar {
        val allUserPosts = getUserPosts(getSignedInUserId(local))?: return getNextDate(goal)

        fun searchPostsForDate(dueDate: Calendar): Boolean {
            var isLastOfDueDate = false
            for(post in allUserPosts) {
                if (post.getGoalFirebaseKey() == goal.getGoalFirebaseKey()) {
                    val postDueDate = post.getDueDate()
                    if (postDueDate != null && datesAreTheSame(dueDate, postDueDate)) {
                        postCounter++
                        if(!post.isIncomplete())
                            amountCompleted += post.getAmount()
                        if(post.isLastPostOfDueDate()) isLastOfDueDate = true
                        else if(post.isNotRequired()) postCounter--
                    }
                }
            }
            return isLastOfDueDate
        }

        val previousDueDate = getPreviousDate(goal)
        val activeDate = getCalendar(goal.getActiveDate())
        if(previousDueDate.comesAfter(activeDate)) {
            amountCompleted = 0
            postCounter = 0
            val previousDueDateCompleted = searchPostsForDate(previousDueDate)
            if (!previousDueDateCompleted) {
                if (dueDateTimeIsUp(goal, previousDueDate))
                    performMissedDueTimeAction(goalView, goal, previousDueDate)
                else return previousDueDate
            }
        }

        val currentDueDate = getNextDate(goal)
        amountCompleted = 0
        postCounter = 0
        val currentDueDateCompleted = searchPostsForDate(currentDueDate)
        if(!currentDueDateCompleted) return currentDueDate

        currentDueDate.add(Calendar.DATE, 1)
        return getNextDate(goal, currentDueDate)
    }

    fun refreshGoalProgressDetails(animate: Boolean = false) {
        if(isRefreshing) return
        isRefreshing = true
        if(animate) beginTransition(binding.mePageParent)
        for(goalView in viewGoalMap.keys.iterator()) {
            val goal = viewGoalMap[goalView]
            if(goal != null) view.refreshDetails(goalView, goal)
        }
        deleteViewsFromMap()
        view.updateGoalsPageNotifications()
        isRefreshing = false
    }

    private fun deleteViewsFromMap() {
        for(goalView in viewsToDeleteFromMap)
            viewGoalMap.remove(goalView)
        viewsToDeleteFromMap.clear()
    }

    /** "Get" Methods */

    fun getFragmentBinding(): FragmentMePageBinding {
        return binding
    }

    fun getFragmentView(): MePageView {
        return view
    }

    fun getFragmentListeners(): MePageListeners {
        return listeners
    }

    fun getFragmentDialogs(): MePageDialogs {
        return dialogs
    }

    fun getRetriever(): GoalRetriever {
        return retriever
    }

    fun getViewActiveGoalMap(): HashMap<View, Goal> {
        return viewDueNowGoalMap
    }

    fun getViewGoalMap(): HashMap<View, Goal> {
        return viewGoalMap
    }

    fun getDueDateMap(): HashMap<Goal, Long> {
        return dueDateMap
    }

    fun getActiveGoalViewsMap(): HashMap<View, View> {
        return dueNowGoalViewsMap
    }

    fun getPostCounter(): Int {
        return postCounter
    }

    fun getAmountCompleted(): Int {
        return amountCompleted
    }

    fun getGoalCompletedAmounts(): HashMap<Goal, Int> {
        return goalCompletedAmounts
    }

    fun getSelectedLabel(): String {
        return selectedLabel
    }

    fun setSelectedGoalView(goalView: View?) {
        selectedGoalView = goalView
    }

    fun getSelectedGoalView(): View? {
        return selectedGoalView
    }

    fun getIncompletePost(): GoalProgress? {
        return incompletePost
    }

    fun setPostingFinished() {
        postingInProgress = false
    }

    fun postingInProgress(): Boolean {
        return postingInProgress
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