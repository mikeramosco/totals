package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.os.Handler
import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MePageView(private val activity: HomePageActivity,
                 private val fragment: MePageFragment) {

    private val binding = fragment.getFragmentBinding()
    private val handler = Handler()

    fun filterGoalViews(selectedLabel: String) {
        val allGoalsOption = activity.getString(R.string.allGoalsFilterOption)
        binding.mePageParent.post {
            binding.selectedGoalText.text = selectedLabel.ifEmpty { allGoalsOption }
         }

        var activeViewsShown = false
        var viewsShown = false

        if(selectedLabel.isEmpty()) {
            for(goalView in binding.goalsContainer.iterator()) {
                viewsShown = true
                goalView.visibility = View.VISIBLE
            }
            for(activeGoalView in binding.activeGoalsContainer.iterator()) {
                activeViewsShown = true
                activeGoalView.visibility = View.VISIBLE
            }
        } else {
            val viewGoalMap = fragment.getViewGoalMap()
            for (goalView in binding.goalsContainer.iterator()) {
                val goal = viewGoalMap[goalView]
                if (goal != null) {
                    val goalHasLabel = goal.getLabels().contains(selectedLabel)
                    goalView.visibility =
                        if (goalHasLabel) {
                            viewsShown = true
                            View.VISIBLE
                        } else View.GONE

                    val activeGoalView = fragment.getActiveGoalViewsMap()[goalView]
                    if(activeGoalView != null) {
                        activeGoalView.visibility =
                            if (goalHasLabel) {
                                activeViewsShown = true
                                View.VISIBLE
                            } else View.GONE
                    }
                }
            }
        }

        if(activeViewsShown) {
            binding.activeSectionTitleLayout.visibility = View.VISIBLE
            binding.activeGoalsContainer.visibility = View.VISIBLE
        } else {
            binding.activeSectionTitleLayout.visibility = View.GONE
            binding.activeGoalsContainer.visibility = View.GONE
        }

        if(viewsShown) {
            binding.scheduleSectionTitle.visibility = View.VISIBLE
            binding.goalsContainer.visibility = View.VISIBLE
            binding.goalsContainer.post {
                binding.goalPageScrollView.fullScroll(ScrollView.FOCUS_UP)
            }
        } else {
            binding.scheduleSectionTitle.visibility = View.GONE
            binding.goalsContainer.visibility = View.GONE
        }

        updateEmptyPageMessageVisibility()
    }

    private fun updateEmptyPageMessageVisibility() {
        binding.emptyGoalPageMessageLayout.visibility =
            if(binding.goalsContainer.isVisible
                || binding.activeGoalsContainer.isVisible)
                View.GONE else View.VISIBLE
    }

    private fun addGoalViewInSortedOrder(goalView: View, isActiveGoalView: Boolean = false) {
        val container = if(isActiveGoalView)
            binding.activeGoalsContainer else binding.goalsContainer
        val viewGoalMap = if(isActiveGoalView)
            fragment.getViewActiveGoalMap() else fragment.getViewGoalMap()
        val goal = viewGoalMap[goalView]?: return
        val goalDateTimestamp = fragment.getDueDateMap()[goal]
            ?: getTodaysDate().timeInMillis
        val goalDate = getCalendar(goalDateTimestamp)
        var goalAdded = false
        for((i, addedGoalView) in container.iterator().withIndex()) {
            val addedGoal = viewGoalMap[addedGoalView]
            if(addedGoal != null) {
                val addedDateTimestamp = fragment.getDueDateMap()[addedGoal]
                    ?: getTodaysDate().timeInMillis
                val addedGoalDate = getCalendar(addedDateTimestamp)
                if(goalDate.comesBefore(addedGoalDate)) {
                    container.addView(goalView, i)
                    goalAdded = true
                    break
                }
            }
        }
        if(!goalAdded) container.addView(goalView)
    }

    private fun updateGoalCompletedAmounts(goal: Goal) {
        val amountCompleted = fragment.getAmountCompleted()
        fragment.getGoalCompletedAmounts()[goal] = amountCompleted
    }

    private fun updateProgressText(goal: Goal, goalView: View, goalCompleted: Boolean) {
        if(!goal.isActive()) return
        val progressText = goalView.findViewById<TextView>(R.id.progressText)
        val nPosts = fragment.getPostCounter()
        if(nPosts == 0) progressText.visibility = View.GONE
        else {
            val amountCompleted = fragment.getAmountCompleted()
            val goalReachedString = activity.getString(R.string.goalPostedString)
            var progressTextString = if(goalCompleted) goalReachedString
            else activity.getString(R.string.goalViewProgressTextString,
                amountCompleted.toString())
            if(nPosts > 1) progressTextString +=
                " ${activity.getString(R.string.postCountString, nPosts.toString())}"
            progressText.text = progressTextString
            progressText.visibility = View.VISIBLE
        }
    }

    private fun updatePostButtonVisibility(goalView: View,
                                           isVisible: Boolean) {
        val postButton = goalView.findViewById<LinearLayout>(R.id.postButton)
        val postButtonDisabled = goalView.findViewById<LinearLayout>(R.id.postButtonDisabled)

        if(isVisible) {
            postButton.visibility = View.VISIBLE
            postButtonDisabled.visibility = View.GONE
        } else {
            postButton.visibility = View.GONE
            postButtonDisabled.visibility = View.VISIBLE
        }
    }

    private fun goalCompletedToRemove(goalView: View, goal: Goal): Boolean {
        if(goal.hasDate()) {
            val goalCompleted = fragment.searchPostsForSingleDateGoal(goal)
            if(goalCompleted) return true
            updateGoalCompletedAmounts(goal)
            updateProgressText(goal, goalView, false)
        }
        return false
    }

    private fun missedSingleDateGoalPosted(goalView: View, goal: Goal): Boolean {
        val goalDate = goal.getDate()?: return false
        if(dueDateTimeIsUp(goal, goalDate)) {
            fragment.performMissedDueTimeAction(goalView, goal, goalDate)
            return true
        }
        return false
    }

    private fun goalCompleted(goal: Goal, dueDate: Calendar): Boolean {
        if(!goal.isRepeating()) return false
        val followingDueDate = getFollowingDate(goal)
        return datesAreTheSame(followingDueDate, dueDate)
    }

    private fun updateNextDateText(goalView: View, goal: Goal) {
        val nextDateText = goalView.findViewById<TextView>(R.id.nextDateText)
        val nextDate = fragment.calculateNextDueDate(goalView, goal)
        val goalCompleted = goalCompleted(goal, nextDate)
        val postButtonIsVisible = !goalCompleted && goal.isActive()
        updateGoalCompletedAmounts(goal)
        updateProgressText(goal, goalView, goalCompleted)
        updatePostButtonVisibility(goalView, postButtonIsVisible)
        fragment.getDueDateMap()[goal] = nextDate.timeInMillis
        nextDateText.text = activity.getString(
            R.string.goalViewNextDateTextString,
            activity.getRecencyText(nextDate))
    }

    fun refreshDetails(goalView: View, goal: Goal) {
        if(goalCompletedToRemove(goalView, goal)
            || missedSingleDateGoalPosted(goalView, goal)) {
            fragment.getRetriever().deleteGoal(goal)
            deleteGoalView(goalView, false)
            val activeGoalView = fragment.getActiveGoalViewsMap()[goalView]
            if(activeGoalView != null) removeActiveGoalView(activeGoalView)
            fragment.removeFromMaps(goalView, activeGoalView)
            return
        }
        if(goal.hasDate()) {
            val dateTextString = activity.getDateTextString(goal)
            val dateText = goalView.findViewById<TextView>(R.id.dateText)
            dateText.text = activity.getString(
                R.string.goalViewSingleDateTextString, dateTextString)
        }
        if(goal.isRepeating())
            updateNextDateText(goalView, goal)
        addOrRemoveToDueNow(goalView)
        if(fragment.isActiveAndDueNow(goalView))
            updateActiveGoalViewTitle(goalView, goal)
        binding.goalsContainer.removeView(goalView)
        addGoalViewInSortedOrder(goalView)
    }

    private fun addOrRemoveToDueNow(goalView: View) {
        val activeGoalView = fragment.getActiveGoalViewsMap()[goalView]
        if(fragment.isActiveAndDueNow(goalView)) {
            if (activeGoalView == null) addActiveGoalView(goalView)
        } else if(activeGoalView != null) {
            removeActiveGoalView(activeGoalView)
            fragment.removeFromMaps(goalView, activeGoalView, true)
        }
    }

    private fun setGoalViewToInactiveState(goalView: View) {
        val dateText = goalView.findViewById<TextView>(R.id.dateText)
        val nextDateText = goalView.findViewById<TextView>(R.id.nextDateText)
        val progressText = goalView.findViewById<TextView>(R.id.progressText)
        val inactiveText = goalView.findViewById<TextView>(R.id.inactiveText)
        updatePostButtonVisibility(goalView, false)
        dateText.visibility = View.GONE
        nextDateText.visibility = View.GONE
        progressText.visibility = View.GONE
        inactiveText.visibility = View.VISIBLE
    }

    fun updateGoal(selectedGoalView: View? = null, selectedGoal: Goal? = null) {
        val goalView = selectedGoalView?: fragment.getSelectedGoalView()?: return
        fragment.setSelectedGoalView(null)
        val goal = selectedGoal?: getStaticGoal()?: return
        setStaticGoal(null)
        val original = fragment.getViewGoalMap()[goalView]?: return
        fragment.removeFromMaps(goalView, deleteGoalView = false)
        fragment.getViewGoalMap()[goalView] = goal
        val activeGoalView = fragment.getActiveGoalViewsMap()[goalView]
        if(activeGoalView != null)
            fragment.getViewActiveGoalMap()[activeGoalView] = goal
        if(goal.hasDate()) {
            val dueDateTimestamp = goal.getDate()?.timeInMillis
            if(dueDateTimestamp != null) fragment.getDueDateMap()[goal] = dueDateTimestamp
            if(goal.isActive()) updatePostButtonVisibility(goalView, true)
        }

        if(goal.amountChangedFrom(original)) {
            val amountText = goalView.findViewById<TextView>(R.id.amountText)
            amountText.text = goal.getAmount().toString()
        }

        if(goal.titleChangedFrom(original)) {
            val titleText = goalView.findViewById<TextView>(R.id.titleText)
            titleText.text = goal.getTitle()
        }

        val dateText = goalView.findViewById<TextView>(R.id.dateText)
        val nextDateText = goalView.findViewById<TextView>(R.id.nextDateText)

        if(goal.isRepeating()) {
            if(goal.repeatingDaysChangedFrom(original)) {
                if(goal.isActive()) nextDateText.visibility = View.VISIBLE
                updateNextDateText(goalView, goal)
            }
        } else nextDateText.visibility = View.GONE

        if(goal.activeStatusChangedFrom(original)) {
            val inactiveText = goalView.findViewById<TextView>(R.id.inactiveText)
            if(goal.isActive()) {
                inactiveText.visibility = View.GONE
                dateText.visibility = View.VISIBLE
                if(goal.isRepeating())
                    nextDateText.visibility = View.VISIBLE
            } else setGoalViewToInactiveState(goalView)
        }

        /*
        if(goal.dateChangedFrom(original)
            || goal.repeatingDaysChangedFrom(original)
            || goal.timeChangedFrom(original)) {
            if(goal.dateChangedFrom(original)) {
                val goalDate = goal.getDate()
                if(goalDate != null)
                    fragment.getDueDateMap()[goal] = goalDate.timeInMillis
            }
            dateText.text = activity.getDateTextString(goal)
            beginTransition(binding.mePageParent)
            binding.goalsContainer.removeView(goalView)
            addGoalViewInSortedOrder(goalView)
        }
         */
    }

    fun postGoalProgressPressed(goalView: View) {
        if(fragment.getSelectedGoalView() != null) return
        fragment.setSelectedGoalView(goalView)
        val goal = fragment.getViewGoalMap()[goalView]?: return
        setStaticGoal(goal)
        fragment.openCreatePostPage(fragment.getDueDateMap()[goal],
            fragment.getGoalCompletedAmounts()[goal])
    }

    fun editGoalViewPressed(goalView: View) {
        if(fragment.getSelectedGoalView() != null) return
        val goal = fragment.getViewGoalMap()[goalView]?: return
        val dueDateTimestamp = fragment.getDueDateMap()[goal]?: return
        if(fragment.otherProcessStarted()) return
        fragment.setSelectedGoalView(goalView)
        setStaticGoal(fragment.getViewGoalMap()[goalView])
        val goalCompleted = if(goal.isRepeating())
            goalCompleted(goal, getCalendar(dueDateTimestamp)) else null
        fragment.openEditGoalPage(fragment.getGoalCompletedAmounts()[goal], goalCompleted)
    }

    private fun getActiveGoalViewTitleString(goal: Goal, amountCompleted: Int?): String {
        val amountRemaining = if(amountCompleted != null && goal.getAmount() != 0)
            goal.getAmount() - amountCompleted else 0
        return if(amountRemaining != 0)
            "$amountRemaining ${goal.getTitle()}" else goal.getTitle()
    }

    @SuppressLint("SetTextI18n")
    private fun updateActiveGoalViewTitle(goalView: View, goal: Goal) {
        val activeGoalView = fragment.getActiveGoalViewsMap()[goalView]?: return
        val titleText = activeGoalView.findViewById<TextView>(R.id.titleText)
        titleText.text = getActiveGoalViewTitleString(goal,
            fragment.getGoalCompletedAmounts()[goal])
    }

    private fun openGoalSettingsPressed(goalView: View) {
        val goal = fragment.getViewGoalMap()[goalView]?: return
        val dueDateTimestamp = fragment.getDueDateMap()[goal]?: return
        val dueDate = getCalendar(dueDateTimestamp)
        val postButton = goalView.findViewById<LinearLayout>(R.id.postButton)
        fragment.getFragmentDialogs().openGoalSettingsDialog(
            goalView, goal, dueDate, postButton.isVisible)
    }

    fun addActiveGoalView(goalView: View) {
        val goal = fragment.getViewGoalMap()[goalView]?: return
        val dueDateTimestamp = fragment.getDueDateMap()[goal]?: return
        val dueDate = getCalendar(dueDateTimestamp)
        val amountCompleted = fragment.getGoalCompletedAmounts()[goal]

        val selectedLabel = fragment.getSelectedLabel()
        val activeGoalView = activity.layoutInflater.inflate(R.layout.view_goal_active, null)
        if(selectedLabel.isEmpty() || goal.getLabels().contains(selectedLabel)) {
            binding.activeSectionTitleLayout.visibility = View.VISIBLE
            binding.activeGoalsContainer.visibility = View.VISIBLE
            updateEmptyPageMessageVisibility()
        } else activeGoalView.visibility = View.GONE
        val activeGoalButton = activeGoalView.findViewById<LinearLayout>(R.id.activeGoalButton)
        val moreButton = activeGoalView.findViewById<LinearLayout>(R.id.moreButton)
        val titleText = activeGoalView.findViewById<TextView>(R.id.titleText)
        val dayDueText = activeGoalView.findViewById<TextView>(R.id.dayDueText)
        val timeDueText = activeGoalView.findViewById<TextView>(R.id.timeDueText)
        val timeRemainingText = activeGoalView.findViewById<TextView>(R.id.timeRemainingText)

        fragment.getViewActiveGoalMap()[activeGoalView] = goal
        fragment.getActiveGoalViewsMap()[goalView] = activeGoalView

        fragment.getFragmentListeners().getScrollViewButtonAnimation().addButton(activeGoalButton)
        activeGoalButton.setOnClickListener {
            postGoalProgressPressed(goalView)
        }

        activeGoalButton.setOnLongClickListener {
            openGoalSettingsPressed(goalView)
            true
        }

        fragment.getFragmentListeners().getScrollViewButtonAnimation().addButton(moreButton)
        moreButton.setOnClickListener {
            openGoalSettingsPressed(goalView)
        }

        titleText.text = getActiveGoalViewTitleString(goal, amountCompleted)

        dayDueText.text = activity.getString(R.string.dayDueString,
            activity.getDateText(dueDate))
        timeDueText.text = activity.getString(R.string.timeDueString,
            getTimeText(goal.getHour(), goal.getMinute()))

        updateTimeRemainingTimer(timeRemainingText, dueDate, goalView, activeGoalView)

        addGoalViewInSortedOrder(activeGoalView, true)
    }

    private fun updateTimeRemainingTimer(timeRemainingText: TextView, dueDate: Calendar,
                                         goalView: View, activeGoalView: View) {
        val timeDue = dueDate.dateClone()
        timeDue.add(Calendar.DATE, 1)
        val millisRemaining = timeDue.timeInMillis - getTimeStamp()
        val secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisRemaining)
        val timeRemaining = DateUtils.formatElapsedTime(secondsRemaining)
        timeRemainingText.text = activity.getString(R.string.timeRemainingString, timeRemaining)
        handler.postDelayed({
            if(binding.activeGoalsContainer.contains(activeGoalView)) {
                if(secondsRemaining.toInt() == 0) {
                    beginTransition(binding.mePageParent)
                    removeActiveGoalView(activeGoalView)
                    fragment.removeFromMaps(goalView, activeGoalView, true)
                    fragment.refreshGoalProgressDetails()
                } else updateTimeRemainingTimer(timeRemainingText, dueDate, goalView, activeGoalView)
            }
        }, 1000)
    }

    fun addGoalView(goal: Goal, animate: Boolean = true): View? {
        val goalView = activity.layoutInflater.inflate(R.layout.view_goal_scheduled, null)
        if(goalCompletedToRemove(goalView, goal) || missedSingleDateGoalPosted(goalView, goal)) {
            fragment.getRetriever().deleteGoal(goal)
            return null
        }
        if(animate) beginTransition(binding.mePageParent)
        binding.scheduleSectionTitle.visibility = View.VISIBLE
        binding.goalsContainer.visibility = View.VISIBLE
        updateEmptyPageMessageVisibility()
        val itemButton = goalView.findViewById<LinearLayout>(R.id.itemButton)
        val amountText = goalView.findViewById<TextView>(R.id.amountText)
        val titleText = goalView.findViewById<TextView>(R.id.titleText)
        val dateText = goalView.findViewById<TextView>(R.id.dateText)
        val nextDateText = goalView.findViewById<TextView>(R.id.nextDateText)
        val settingsButton = goalView.findViewById<LinearLayout>(R.id.settingsButton)
        val postButton = goalView.findViewById<LinearLayout>(R.id.postButton)

        fragment.getViewGoalMap()[goalView] = goal

        val buttonAnimation = fragment.getFragmentListeners().getScrollViewButtonAnimation()
        buttonAnimation.addButton(itemButton)
        buttonAnimation.addButton(settingsButton)
        buttonAnimation.addButton(postButton)
        itemButton.setOnClickListener {
            editGoalViewPressed(goalView)
        }

        itemButton.setOnLongClickListener {
            openGoalSettingsPressed(goalView)
            true
        }

        settingsButton.setOnClickListener {
            openGoalSettingsPressed(goalView)
        }

        postButton.setOnClickListener {
            postGoalProgressPressed(goalView)
        }

        if(goal.getAmount() > 0) amountText.text = goal.getAmount().toString()
        else amountText.visibility = View.GONE
        titleText.text = goal.getTitle()
        val dateTextString = activity.getDateTextString(goal)
        if(goal.isRepeating()) {
            dateText.text = dateTextString
            updateNextDateText(goalView, goal)
        } else {
            dateText.text = activity.getString(
                R.string.goalViewSingleDateTextString, dateTextString)
            dateText.setTextColor(activity.getValuesColor(R.color.color_theme))
            nextDateText.visibility = View.GONE
            val dueDate = goal.getDate()
            if(dueDate != null)
                fragment.getDueDateMap()[goal] = dueDate.timeInMillis
        }

        if(!goal.isActive())
            setGoalViewToInactiveState(goalView)

        addGoalViewInSortedOrder(goalView)

        return goalView
    }

    fun deleteGoalView(goalView: View, animate: Boolean = true) {
        if(animate) beginTransition(binding.mePageParent)
        binding.goalsContainer.removeView(goalView)
        if(binding.goalsContainer.showingNoViews()) {
            binding.scheduleSectionTitle.visibility = View.GONE
            binding.goalsContainer.visibility = View.GONE
            updateEmptyPageMessageVisibility()
        }
    }

    fun removeActiveGoalView(activeGoalView: View) {
        binding.activeGoalsContainer.removeView(activeGoalView)
        if(binding.activeGoalsContainer.showingNoViews()) {
            binding.activeSectionTitleLayout.visibility = View.GONE
            binding.activeGoalsContainer.visibility = View.GONE
            updateEmptyPageMessageVisibility()
        }
        updateGoalsNotifIcon()
    }

    private fun getLabelsWithPostLikesNotifs(): ArrayList<String> {
        val labelsWithPostLikesNotifs = ArrayList<String>()
        for(post in getStaticNewLikesMap().keys.iterator())
            for(label in post.getLabels())
                if(!labelsWithPostLikesNotifs.contains(label))
                    labelsWithPostLikesNotifs.add(label)
        return labelsWithPostLikesNotifs
    }

    private fun getLabelsWithDueNowNotifs(): ArrayList<String> {
        val labelsWithDueNowNotifs = ArrayList<String>()
        for(goal in fragment.getViewActiveGoalMap().values.iterator())
            for(label in goal.getLabels())
                if(!labelsWithDueNowNotifs.contains(label))
                    labelsWithDueNowNotifs.add(label)
        return labelsWithDueNowNotifs
    }

    fun updateGoalsPageNotifications() {
        val labelsWithPostLikesNotifs = getLabelsWithPostLikesNotifs()
        val labelsWithDueNowNotifs = getLabelsWithDueNowNotifs()

        val labelsWithNotifs = ArrayList<String>()
        for(label in labelsWithPostLikesNotifs)
            labelsWithNotifs.add(label)
        for(label in labelsWithDueNowNotifs)
            if(!labelsWithNotifs.contains(label))
                labelsWithNotifs.add(label)

        updatePostLikesNotifIconVisibility(labelsWithPostLikesNotifs)

        val selectedLabel = fragment.getSelectedLabel()
        binding.goalsFilterNotifIcon.visibility =
            if(labelsWithNotifs.isEmpty()) View.GONE
            else if(labelsWithNotifs.size > 1) {
                if(selectedLabel.isEmpty()) View.GONE else View.VISIBLE
            } else if(selectedLabel.isEmpty() || labelsWithNotifs[0] == selectedLabel)
                View.GONE
            else View.VISIBLE

        updateGoalsNotifIcon()

        if(!binding.totalsButton.isVisible) {
            beginTransition(binding.mePageParent)
            binding.likeNotifsButtonLayout.visibility = View.VISIBLE
            binding.totalsButton.visibility = View.VISIBLE
        }
    }

    private fun updatePostLikesNotifIconVisibility(labelsWithPostLikesNotifs: ArrayList<String>) {
        val selectedLabel = fragment.getSelectedLabel()
        if(labelsWithPostLikesNotifs.isEmpty())
            binding.postLikesNotifIcon.visibility = View.GONE
        else if(selectedLabel.isEmpty() || labelsWithPostLikesNotifs.contains(selectedLabel))
            binding.postLikesNotifIcon.visibility = View.VISIBLE
        else binding.postLikesNotifIcon.visibility = View.GONE
    }

    private fun updateGoalsNotifIcon() {
        activity.getBinding().goalsNotifIcon.visibility =
            if(binding.activeGoalsContainer.isNotEmpty()
                || getStaticNewLikesMap().isNotEmpty())
                View.VISIBLE else View.GONE
    }

    private fun LinearLayout.showingNoViews(): Boolean {
        if(size == 0) return true
        for(view in iterator())
            if(view.isVisible) return false
        return true
    }

    fun addNewlyAddedGoal() {
        val goal = getStaticGoal()?: return
        setStaticGoal(null)
        val goalView = addGoalView(goal)
        if(goalView != null && fragment.isActiveAndDueNow(goalView))
            addActiveGoalView(goalView)
    }
}