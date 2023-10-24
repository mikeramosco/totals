package com.justanotherdeveloper.totalslite

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

fun getTodaysDate(): Calendar {
    return Calendar.getInstance(TimeZone.getDefault())
}

fun getTomorrowsDate(): Calendar {
    val tomorrowsDate = getTodaysDate()
    tomorrowsDate.add(Calendar.DATE, 1)
    return tomorrowsDate
}

fun getYesterdaysDate(): Calendar {
    val tomorrowsDate = getTodaysDate()
    tomorrowsDate.add(Calendar.DATE, -1)
    return tomorrowsDate
}

fun datesAreTheSame(date1: Calendar, date2: Calendar): Boolean {
    return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
            date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
            date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)
}

fun isToday(date: Calendar): Boolean {
    return datesAreTheSame(date, getTodaysDate())
}

fun isYesterday(date: Calendar): Boolean {
    return datesAreTheSame(date, getYesterdaysDate())
}

fun isTomorrow(date: Calendar): Boolean {
    return datesAreTheSame(date, getTomorrowsDate())
}

fun getTimeStamp(): Long {
    return getTodaysDate().timeInMillis
}

fun getCalendar(timestamp: Long): Calendar {
    val calendar = getTodaysDate()
    calendar.timeInMillis = timestamp
    return calendar
}

fun Calendar.comesAfter(date: Calendar, inclusive: Boolean = false): Boolean {
    return if(inclusive)
        datesAreTheSame(this, date) || timeInMillis >= date.timeInMillis
    else timeInMillis > date.timeInMillis
}

fun Calendar.comesBefore(date: Calendar, inclusive: Boolean = false): Boolean {
    return if(inclusive)
        datesAreTheSame(this, date) || timeInMillis <= date.timeInMillis
    else timeInMillis < date.timeInMillis
}

fun Calendar.dateClone(): Calendar {
    return getCalendar(timeInMillis)
}

fun createCalendar(year: Int, month: Int, day: Int): Calendar {
    val calendar = Calendar.getInstance(TimeZone.getDefault())
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month-1)
    calendar.set(Calendar.DAY_OF_MONTH, day)
    return calendar.resetTimeOfDay()
}

fun Calendar.resetTimeOfDay(): Calendar {
    set(Calendar.MILLISECOND, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.HOUR_OF_DAY, 0)
    return this
}

fun Calendar.toDateString(): String {
    val year = get(Calendar.YEAR)
    val month = get(Calendar.MONTH) + 1
    val day = get(Calendar.DAY_OF_MONTH)
    return "$month/$day/$year"
}

@SuppressLint("SimpleDateFormat")
fun getTimeText(cal: Calendar): String {
    val date = cal.time
    val sdf = SimpleDateFormat("hh:mm aa")
    var timeText = sdf.format(date)
    if(timeText[0] == '0')
        timeText = timeText.substring(1, timeText.length)
    return timeText
}

fun getTimeText(hour: Int, minute: Int): String {
    val cal = getTodaysDate()
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    return getTimeText(cal)
}

fun Activity.getDateText(date: Calendar): String {
    return when {
        datesAreTheSame(getTodaysDate(), date) -> getString(R.string.todayString)
        isTomorrow(date) -> getString(R.string.tomorrowString)
        isYesterday(date) -> getString(R.string.yesterdayString)
        else -> date.toDateString()
    }
}

fun Context.getRepeatingDaysString(repeatingDays: ArrayList<Int>): String {
    var repeatingDaysString = ""

    fun addLetter(letter: String) {
        if(repeatingDaysString.isNotEmpty())
            repeatingDaysString += ", "
        repeatingDaysString += letter
    }

    var sunLetter = getString(R.string.sunLetter)
    var monLetter = getString(R.string.monLetter)
    var tueLetter = getString(R.string.tueLetter)
    var wedLetter = getString(R.string.wedLetter)
    var thuLetter = getString(R.string.thuLetter)
    var friLetter = getString(R.string.friLetter)
    var satLetter = getString(R.string.satLetter)

    if(repeatingDays.size == 1) {
        sunLetter = getString(R.string.sundaysString)
        monLetter = getString(R.string.mondaysString)
        tueLetter = getString(R.string.tuesdaysString)
        wedLetter = getString(R.string.wednesdaysString)
        thuLetter = getString(R.string.thursdaysString)
        friLetter = getString(R.string.fridaysString)
        satLetter = getString(R.string.saturdaysString)
    } else if(repeatingDays.size == 2) {
        sunLetter = getString(R.string.sunString)
        monLetter = getString(R.string.monString)
        tueLetter = getString(R.string.tueString)
        wedLetter = getString(R.string.wedString)
        thuLetter = getString(R.string.thuString)
        friLetter = getString(R.string.friString)
        satLetter = getString(R.string.satString)
    }

    for(i in 0..6) {
        when(i) {
            0 -> if(repeatingDays.contains(Calendar.SUNDAY)) addLetter(sunLetter)
            1 -> if(repeatingDays.contains(Calendar.MONDAY)) addLetter(monLetter)
            2 -> if(repeatingDays.contains(Calendar.TUESDAY)) addLetter(tueLetter)
            3 -> if(repeatingDays.contains(Calendar.WEDNESDAY)) addLetter(wedLetter)
            4 -> if(repeatingDays.contains(Calendar.THURSDAY)) addLetter(thuLetter)
            5 -> if(repeatingDays.contains(Calendar.FRIDAY)) addLetter(friLetter)
            6 -> if(repeatingDays.contains(Calendar.SATURDAY)) addLetter(satLetter)
        }
    }

    when (repeatingDaysString) {
        "$monLetter, $tueLetter, $wedLetter, $thuLetter, $friLetter" ->
            repeatingDaysString = getString(R.string.weekdaysString)
        "$sunLetter, $satLetter" -> repeatingDaysString =
            getString(R.string.weekendsString)
        "$sunLetter, $monLetter, $tueLetter, $wedLetter, $thuLetter, $friLetter, $satLetter" ->
            repeatingDaysString = getString(R.string.everydayString)
    }

    return repeatingDaysString
}

fun getFollowingDate(goal: Goal): Calendar {
    val dateToStartSearch = getNextDate(goal)
    dateToStartSearch.add(Calendar.DATE, 1)
    return getNextDate(goal, dateToStartSearch)
}

fun getNextDate(goal: Goal, dateCursor: Calendar = getTodaysDate()): Calendar {
    var dayOfWeek = dateCursor.get(Calendar.DAY_OF_WEEK)
    while(true) {
        if (goal.getRepeatingDays().contains(dayOfWeek)) {
            dateCursor.set(Calendar.MILLISECOND, 0)
            dateCursor.set(Calendar.SECOND, 0)
            dateCursor.set(Calendar.HOUR_OF_DAY, goal.getHour())
            dateCursor.set(Calendar.MINUTE, goal.getMinute())
            return dateCursor
        }
        dateCursor.add(Calendar.DATE, 1)
        dayOfWeek = dateCursor.get(Calendar.DAY_OF_WEEK)
    }
}

fun getPreviousDate(goal: Goal): Calendar {
    val dateCursor: Calendar = getYesterdaysDate()
    var dayOfWeek = dateCursor.get(Calendar.DAY_OF_WEEK)
    while(true) {
        if (goal.getRepeatingDays().contains(dayOfWeek)) {
            dateCursor.set(Calendar.MILLISECOND, 0)
            dateCursor.set(Calendar.SECOND, 0)
            dateCursor.set(Calendar.HOUR_OF_DAY, goal.getHour())
            dateCursor.set(Calendar.MINUTE, goal.getMinute())
            return dateCursor
        }
        dateCursor.add(Calendar.DATE, -1)
        dayOfWeek = dateCursor.get(Calendar.DAY_OF_WEEK)
    }
}

fun daysBetween(startDate: Calendar, endDate: Calendar): Int {
    startDate.resetTimeOfDay()
    endDate.resetTimeOfDay()
    val end = endDate.timeInMillis
    val start = startDate.timeInMillis
    return TimeUnit.MILLISECONDS.toDays(abs(end - start)).toInt()
}

private fun getDaysInWeeks(nDays: Int): Int {
    return (nDays.toDouble() / 7.0).toInt()
}

private fun getDaysInMonths(nDays: Int): Int {
    return (nDays.toDouble() / 30.0).toInt()
}

private fun getDaysInYears(nDays: Int): Int {
    return (nDays.toDouble() / 365.0).toInt()
}

fun Activity.getRecencyText(date: Calendar): String {
    val todaysDate = getTodaysDate()
    return when {
        datesAreTheSame(todaysDate, date) -> getString(R.string.todayString)
        isTomorrow(date) -> getString(R.string.tomorrowString)
        isYesterday(date) -> getString(R.string.yesterdayString)
        todaysDate.comesAfter(date) -> getXDaysAgoText(todaysDate, date)
        else -> getInXDaysText(todaysDate, date)
    }
}

fun Activity.getRecencyText(timestamp: Long): String {
    val currentTime = getTimeStamp()
    val secondsPassed = (currentTime - timestamp) / 1000.0
    val hoursPassed = secondsPassed / 3600.0
    return when {
        hoursPassed < 1 -> {
            val minutesPassed = 60 * hoursPassed
            if(minutesPassed < 1) getString(R.string.secondsAgoString)
            else if(minutesPassed < 2) getString(R.string.minuteAgoString)
            else getString(R.string.minutesAgoString, minutesPassed.toInt().toString())
        }
        hoursPassed < 24 -> {
            val hoursPassedInt = hoursPassed.roundToInt()
            if(hoursPassedInt == 1) getString(R.string.hourAgoString)
            else getString(R.string.hoursAgoString, hoursPassedInt.toString())
        }
        else -> getRecencyText(getCalendar(timestamp))
    }
}

private fun Activity.getXDaysAgoText(todaysDate: Calendar, date: Calendar): String {
    val daysBetween = daysBetween(todaysDate, date.dateClone())
    return when {
        daysBetween < 14 -> getString(R.string.xDaysAgoString,
            daysBetween.toString())
        daysBetween < 60 -> getString(R.string.xWeeksAgoString,
            getDaysInWeeks(daysBetween).toString())
        daysBetween < 730 -> getString(R.string.xMonthsAgoString,
            getDaysInMonths(daysBetween).toString())
        else -> getString(R.string.xYearsAgoString,
            getDaysInYears(daysBetween).toString())
    }
}

private fun Activity.getInXDaysText(todaysDate: Calendar, date: Calendar): String {
    val daysBetween = daysBetween(todaysDate, date.dateClone())
    return when {
        daysBetween < 14 -> getString(R.string.inXDaysString,
            daysBetween.toString())
        daysBetween < 60 -> getString(R.string.inXWeeksString,
            getDaysInWeeks(daysBetween).toString())
        daysBetween < 730 -> getString(R.string.inXMonthsString,
            getDaysInMonths(daysBetween).toString())
        else -> getString(R.string.inXYearsString,
            getDaysInYears(daysBetween).toString())
    }
}