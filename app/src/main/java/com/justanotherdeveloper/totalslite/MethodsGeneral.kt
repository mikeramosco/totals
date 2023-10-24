package com.justanotherdeveloper.totalslite

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.ParseException
import java.util.*
import kotlin.collections.HashMap

/**
 * Adds fade in/out animation to views within a layout
 * when layout visibility is changed
 */
fun beginTransition(layout: LinearLayout) {
    TransitionManager.beginDelayedTransition(layout)
}

/** Puts cursor to end of text of a text field */
fun moveCursorTo(editText: EditText) {
    editText.requestFocus()
    editText.post {
        editText.setSelection(
            editText.text.toString().length
        )
    }
}

/** Returns true if date exists */
fun dateExists(date: String): Boolean {
    val dateFormat = "dd-MM-yyyy"

    return try {
        val df = SimpleDateFormat(dateFormat)
        df.isLenient = false
        df.parse(date)
        true
    } catch (e: ParseException) {
        false
    }
}

/** Minimizes app - usually when back pressed on home page */
fun Activity.goToHomeScreen() {
    val startMain = Intent(Intent.ACTION_MAIN)
    startMain.addCategory(Intent.CATEGORY_HOME)
    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(startMain)
}

fun Activity.copyToClipboard(textToCopy: String) {
    val clipboard = getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("note", textToCopy)
    clipboard.setPrimaryClip(clip)
    showToast(getString(R.string.copiedToClipboardMessage))
}

fun String.isFormattedAsWebsite(context: Context): Boolean {
    val httpsString = context.getString(R.string.httpsString)
    val httpString = context.getString(R.string.httpString)

    return startsWith(httpsString, true) ||
            startsWith(httpString, true)
}

fun String.formatAsWebsiteLink(context: Context): String {
    return if(isFormattedAsWebsite(context)) this
    else "${context.getString(R.string.httpsString)}$this"
}

fun Context.getValuesColor(colorCode: Int): Int {
    return ContextCompat.getColor(this, colorCode)
}

fun EditText.toLowerCase() {
    val username = text.toString()
    val lowercaseUsername = username.lowercase(Locale.getDefault())
    if(username == lowercaseUsername) return
    setText(lowercaseUsername)
    setSelection(lowercaseUsername.length)
}

fun generateId(): Int {
    val startRandomID = 100000
    val endRandomID = 999999
    return Random().nextInt((endRandomID + 1) - startRandomID) + startRandomID
}

fun EditText.formatEntry() {
    removeEndSpaces()
    removeNewLines()
}

fun EditText.removeEndSpaces() {
    val str = text.toString()
    if(str.isNotEmpty() && str.last().toString() == " ") {
        setText(str.removeEndSpaces())
        setSelection(length())
    }
}

fun EditText.removeNewLines(newValue: String = "") {
    val str = text.toString()
    if(str.contains("\n")) {
        setText(str.removeNewLines(newValue))
        setSelection(length())
    }
}

fun String.removeEndSpaces(): String {
    return if(isNotEmpty() && last().toString() == " ")
        substring(0, length-1).removeEndSpaces()
    else this
}

fun String.removeNewLines(newValue: String = ""): String {
    return replace("\n", newValue)
}

fun String.removeFrontZeros(): String {
    if(isEmpty() || this == "0") return ""
    return if(this[0] == '0') substring(1, length).removeFrontZeros() else this
}

fun Context.showToast(message: String) {
    val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
    try {
        val view = toast.view?.findViewById(android.R.id.message) as TextView
        view.gravity = Gravity.CENTER
    } catch (e: java.lang.NullPointerException) {
        e.printStackTrace()
    }
    toast.show()
}

fun Context.getBackupProfileImage(): Bitmap {
    val imageView = ImageView(this)
    imageView.setImageResource(R.drawable.ic_person_white)
    return imageView.drawable.toBitmap()
}

fun Context.getBackupPhotoIcon(): Bitmap {
    val imageView = ImageView(this)
    imageView.setImageResource(R.drawable.ic_broken_image)
    return imageView.drawable.toBitmap()
}

fun HashMap<Int, Long>.intLongMapClone(): HashMap<Int, Long> {
    val clone = HashMap<Int, Long>()
    for(key in keys.iterator()) {
        val value = get(key)
        if(value != null) clone[key] = value
    }
    return clone
}

fun ArrayList<Int>.intArrayListClone(): ArrayList<Int> {
    val clone = ArrayList<Int>()
    for(i in this) clone.add(i)
    return clone
}

fun ArrayList<String>.stringArrayListClone(): ArrayList<String> {
    val clone = ArrayList<String>()
    for(i in this) clone.add(i)
    return clone
}

fun Activity.openLink(linkTextString: String) {
    if (linkTextString.isNotEmpty()) {
        val websiteLink = linkTextString.formatAsWebsiteLink(this)
        val openWebsite = Intent(Intent.ACTION_VIEW, Uri.parse(websiteLink))
        openWebsite.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setPackage("com.android.chrome")
        try {
            startActivity(openWebsite)
        } catch (e: ActivityNotFoundException) {
            intent.setPackage(null)
            startActivity(openWebsite)
        }
    }
}

fun Context.showRequestUnavailableToast() {
    showToast(getString(R.string.requestUnavailableError))
}

fun Context.addDebugMsg(message: String = "called", showToast: Boolean = true) {
    if(showToast) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    Log.d("dtag", message)
}

fun addDebugMsg(message: String = "called") {
    Log.d("dtag", message)
}

fun Activity.slideTransitionLeftIfSignedIn(database: TinyDB) {
    if(isSignedIn(database))
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
}

fun getTimeDataString(hour: Int, minute: Int): String {
    return "$hour\t$minute"
}

fun getHourFromTimeDataString(timeString: String): Int {
    return timeString.split("\t")[0].toInt()
}

fun getMinuteFromTimeDataString(timeString: String): Int {
    return timeString.split("\t")[1].toInt()
}

fun saveTimeDataString(local: TinyDB, timeDataString: String) {
    local.putString(SAVED_TIME_REF, timeDataString)
}

fun getSavedTimeDataString(local: TinyDB): String? {
    return local.getString(SAVED_TIME_REF).ifEmpty { null }
}

fun getEmailReportedPostMessage(post: GoalProgress, user: TotalsUser): String {
    val amount = post.getAmount()
    val postTitle = if(amount > 0)
        "$amount ${post.getTitle()}" else post.getTitle()
    return "" +
            "USER DETAILS\n" +
            "Username: ${user.getUsername()}\n" +
            "Name: ${user.getName()}\n" +
            "ID: ${user.getUserId()}\n" +
            "Photo: ${user.getProfilePhotoUrl()}\n" +
            "Phone Number: ${user.getPhoneNumber()}\n" +
            "\n" +
            "POST DETAILS\n" +
            "Title: $postTitle\n" +
            "Caption: ${post.getCaption()}\n" +
            "Photo: ${post.getBitmapPhotoUrl()}\n" +
            "Firebase Key: ${post.getPostFirebaseKey()}\n" +
            "Date Posted: ${getCalendar(post.getTimestampPosted()).toDateString()}\n" +
            "Reason Reported: ${post.getReasonReported()}"
}