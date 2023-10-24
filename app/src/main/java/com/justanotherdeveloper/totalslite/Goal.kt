package com.justanotherdeveloper.totalslite

import android.content.Context
import com.google.firebase.database.DataSnapshot
import java.util.Calendar

open class Goal {

    private var amount = 0
    private var title = ""
    private var hour = SENTINEL
    private var minute = SENTINEL
    private var date: Calendar? = null
    private var repeatingDays = ArrayList<Int>()
    private var isActive = true
    private var notes = ArrayList<String>()
    private var links = ArrayList<String>()
    private var labels = ArrayList<String>()
    private var goalFirebaseKey = ""
    private var createdDate: Long = 0
    private var updatedDate: Long = 0
    private var activeDate: Long = 0
    private var userId = 0

    fun createGoalProgress(): GoalProgress {
        val progress = GoalProgress()
        progress.setAmount(amount)
        progress.setTitle(title)
        progress.setTime(hour, minute)
        progress.setDate(date?.dateClone())
        progress.setRepeatingDays(repeatingDays.intArrayListClone())
        progress.setNotes(notes.stringArrayListClone())
        progress.setLinks(links.stringArrayListClone())
        progress.setLabels(labels.stringArrayListClone())
        progress.setGoalFirebaseKey(goalFirebaseKey)
        progress.setCreatedDate(createdDate)
        progress.setUpdatedDate(updatedDate)
        progress.setUserId(userId)
        return progress
    }

    fun createClone(): Goal {
        val clone = Goal()
        clone.amount = amount
        clone.title = title
        clone.hour = hour
        clone.minute = minute
        clone.date = date?.dateClone()
        clone.repeatingDays = repeatingDays.intArrayListClone()
        clone.notes = notes.stringArrayListClone()
        clone.links = links.stringArrayListClone()
        clone.labels = labels.stringArrayListClone()
        clone.goalFirebaseKey = goalFirebaseKey
        clone.createdDate = createdDate
        clone.updatedDate = updatedDate
        clone.activeDate = activeDate
        clone.isActive = isActive
        clone.userId = userId
        return clone
    }

    fun setActive(active: Boolean = true) {
        isActive = active
        activeDate = getTimeStamp()
    }

    fun isActive(): Boolean{
         return isActive
    }

    fun addLabel(label: String) {
        if(!labels.contains(label))
            labels.add(label)
    }

    fun setLabels(labels: ArrayList<String>) {
        this.labels = labels
    }

    fun getLabels(): ArrayList<String> {
        return labels
    }

    fun setLinks(links: ArrayList<String>) {
        this.links = links
    }

    fun getLinks(): ArrayList<String> {
        return links
    }

    fun setNotes(notes: ArrayList<String>) {
        this.notes = notes
    }

    fun getNotes(): ArrayList<String> {
        return notes
    }

    fun hasTime(): Boolean {
        return hour != SENTINEL
    }

    fun setTime(hour: Int, minute: Int) {
        this.hour = hour
        this.minute = minute
        if(hasDate()) setDateWithTime()
    }

    private fun setDateWithTime() {
        date?.set(Calendar.MILLISECOND, 0)
        date?.set(Calendar.SECOND, 0)
        date?.set(Calendar.MINUTE, minute)
        date?.set(Calendar.HOUR_OF_DAY, hour)
    }

    fun getHour(): Int {
        return hour
    }

    fun getMinute(): Int {
        return minute
    }

    fun hasDate(): Boolean {
        return date != null
    }

    fun setDate(date: Calendar?) {
        if(date != null) clearRepeatingDays()
        this.date = date
        if(hasTime()) setDateWithTime()
        else this.date?.resetTimeOfDay()
    }

    fun getDate(): Calendar? {
        return date
    }

    fun clearRepeatingDays() {
        repeatingDays.clear()
    }

    fun setRepeatingDays(repeatingDays: ArrayList<Int>) {
        if(repeatingDays.isNotEmpty()) date = null
        this.repeatingDays = repeatingDays
    }

    fun getRepeatingDays(): ArrayList<Int> {
        return repeatingDays
    }

    fun getRepeatingDaysString(context: Context): String {
        return context.getRepeatingDaysString(repeatingDays)
    }

    fun isRepeating(): Boolean {
        return repeatingDays.isNotEmpty()
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun getTitle(): String {
        return title
    }

    fun setAmount(amount: Int) {
        this.amount = amount
    }

    fun getAmount(): Int {
        return amount
    }

    fun getLabelsAsDataMap(): HashMap<String, Any> {
        val dataMap = HashMap<String, Any>()
        for((i, label) in labels.withIndex())
            dataMap[i.toString()] = label
        return dataMap
    }

    fun getNotesAsDataMap(): HashMap<String, Any>? {
        if(notes.isEmpty()) return null
        val dataMap = HashMap<String, Any>()
        for((i, note) in notes.withIndex())
            dataMap[i.toString()] = note
        return dataMap
    }

    fun getLinksAsDataMap(): HashMap<String, Any>? {
        if(links.isEmpty()) return null
        val dataMap = HashMap<String, Any>()
        for((i, link) in links.withIndex())
            dataMap[i.toString()] = link
        return dataMap
    }

    fun setGoalFirebaseKey(firebaseKey: String) {
        goalFirebaseKey = firebaseKey
    }

    fun getGoalFirebaseKey(): String {
        return goalFirebaseKey
    }

    fun activeStatusChangedFrom(original: Goal): Boolean {
        return isActive != original.isActive
    }

    fun labelsChangedFrom(original: Goal): Boolean {
        return labels.toString() != original.labels.toString()
    }

    fun amountChangedFrom(original: Goal): Boolean {
        return amount != original.amount
    }

    fun titleChangedFrom(original: Goal): Boolean {
        return title != original.title
    }

    fun dateChangedFrom(original: Goal): Boolean {
        if(hasDate() != original.hasDate()) return true
        val date = date
        val originalDate = original.date
        if(date != null && originalDate != null)
            return !datesAreTheSame(date, originalDate)
        return false
    }

    fun repeatingDaysChangedFrom(original: Goal): Boolean {
        return repeatingDays.toString() != original.repeatingDays.toString()
    }

    fun timeChangedFrom(original: Goal): Boolean {
        return hour != original.hour || minute != original.minute
    }

    fun notesChangedFrom(original: Goal): Boolean {
        return notes.toString() != original.notes.toString()
    }

    fun linksChangedFrom(original: Goal): Boolean {
        return links.toString() != original.links.toString()
    }

    fun parseGoalData(data: DataSnapshot, isPost: Boolean = false): Boolean {
        if(!isPost) {
            goalFirebaseKey = data.key.toString()
            val timestampAddedValue = data.child(TIMESTAMP_ADDED_PATH).value
            if (timestampAddedValue == null) {
                // TODO
                return false
            }

            createdDate = timestampAddedValue.toString().toLong()
            updatedDate = data.child(TIMESTAMP_UPDATED_PATH).value.toString().toLong()
            val isActiveValue = data.child(IS_ACTIVE_PATH).value
            if(isActiveValue != null) isActive = isActiveValue.toString().toBoolean()
            val activeDateValue = data.child(ACTIVE_START_TIMESTAMP_PATH).value
            activeDate = activeDateValue?.toString()?.toLong()
                ?: timestampAddedValue.toString().toLong()
        }

        val amountValue = data.child(AMOUNT_PATH).value
        if(amountValue != null) amount = amountValue.toString().toInt()
        title = data.child(TITLE_PATH).value.toString()
        val timeString = data.child(GOAL_TIME_PATH).value.toString()
        hour = getHourFromTimeDataString(timeString)
        minute = getMinuteFromTimeDataString(timeString)
        val goalDateTimestampValue = data.child(TIMESTAMP_GOAL_DATE_PATH).value
        if(goalDateTimestampValue != null) {
            val goalDateTimestamp = goalDateTimestampValue.toString().toLong()
            date = getCalendar(goalDateTimestamp)
        }
        val repeatingDaysValue = data.child(REPEATING_DAYS_PATH).value
        if(repeatingDaysValue != null) {
            val repeatingDaysString = repeatingDaysValue.toString()
            repeatingDays = getRepeatingDaysFromFirebaseValue(repeatingDaysString)
        }
        val notesDataIterator = data.child(NOTES_PATH).children.iterator()
        while(notesDataIterator.hasNext())
            notes.add(notesDataIterator.next().value.toString())
        val linksDataIterator = data.child(LINKS_PATH).children.iterator()
        while(linksDataIterator.hasNext())
            links.add(linksDataIterator.next().value.toString())
        val labelDataIterator = data.child(LABELS_PATH).children.iterator()
        while(labelDataIterator.hasNext())
            labels.add(labelDataIterator.next().value.toString())
        userId = data.child(USER_ID_PATH).value.toString().toInt()

        return true
    }

    fun setActiveDate(timestamp: Long) {
        activeDate = timestamp
    }

    fun getActiveDate(): Long {
        return activeDate
    }

    fun setCreatedDate(timestamp: Long) {
        createdDate = timestamp
    }

    fun getCreatedDate(): Long {
        return createdDate
    }

    fun setUpdatedDate(timestamp: Long) {
        updatedDate = timestamp
    }

    fun getUpdatedDate(): Long {
        return updatedDate
    }

    fun setUserId(userId: Int) {
        this.userId = userId
    }

    fun getUserId(): Int {
        return userId
    }

    fun infoAdded(): Boolean {
        return amount != 0 || title.isNotEmpty()
                || hasDate() || isRepeating() || hasTime()
                || notes.isNotEmpty() || links.isNotEmpty()
    }

    fun changedFrom(original: Goal): Boolean {
        return amountChangedFrom(original) || titleChangedFrom(original)
                || dateChangedFrom(original) || repeatingDaysChangedFrom(original)
                || timeChangedFrom(original) || notesChangedFrom(original)
                || linksChangedFrom(original) || labelsChangedFrom(original)
                || activeStatusChangedFrom(original)
    }
}