package com.justanotherdeveloper.totalslite

// Start app delay
const val LOGO_FADE_DELAY: Long = 100
const val SPLASH_SCREEN_DELAY: Long = 1000

// Animation Fade Duration
const val FADE_IN_DURATION = 100
const val FADE_OUT_DURATION = 200

// Scroll View Content Transition Delay
const val TRANSITION_DELAY: Long = 140

// Delay to apply typed searched text
const val SEARCH_DELAY: Long = 250

// Max Requests Per X Minutes Limit
const val MAX_REQUESTS_LIMIT = 100
const val MAX_REQUESTS_FREQUENCY_IN_MINUTES = 5

// Acceptable ages range
const val MAX_AGE = 200
const val MIN_AGE = 13

// Acceptable username length range
//const val MAX_USERNAME_CHAR = 20
const val MIN_USERNAME_CHAR = 3

// 30 Second wait time to resend phone number verification code
const val RESEND_CODE_WAIT_TIME: Long = 30000

// Batch of posts to load
const val FIRST_BATCH_SIZE = 10
const val NEXT_BATCH_SIZE = 20

// Delay duration of how long heart image is shown when post liked
const val HEART_IMAGE_SHOWN_DELAY: Long = 750

// Delay to wait to check for double tap
const val DOUBLE_TAP_DELAY: Long = 250

// Max number of links/notes
const val MAX_NOTES_AND_LINKS = 20

// Special
const val SENTINEL = -1

// Reference Strings (For Local Database & Intents)
const val NAME_REF = "NAME"
const val BIRTHDAY_REF = "BIRTHDAY"
const val VERIFICATION_ID_REF = "VERIFICATION ID"
const val PHONE_NUMBER_REF = "PHONE NUMBER"
const val COUNTRY_CODE_REF = "COUNTRY CODE"
const val SIGNED_IN_TOTALS_USER_REF = "SIGNED IN TOTALS USER"
const val NEW_GOAL_REF = "NEW GOAL"
const val GOAL_ADDED_REF = "GOAL ADDED"
const val GOAL_UPDATED_REF = "GOAL UPDATED"
const val GOAL_LABELS_REF = "GOAL LABELS"
const val RECENT_GOAL_LABEL_REF = "RECENT GOAL LABEL"
const val SELECTED_LABEL_REF = "SELECTED LABEL"
const val LABELS_WITH_NOTIFS_REF = "LABELS WITH NOTIFS"
const val USERS_LIST_REF = "USERS LIST"
const val FOR_PROGRESS_PAGE_REF = "FOR PROGRESS PAGE"
const val PROGRESS_POSTED_REF = "PROGRESS POSTED"
const val PHOTO_DETAILS_REF = "PHOTO DETAILS"
const val AMOUNT_COMPLETED_REF = "AMOUNT COMPLETED"
const val GOAL_COMPLETED_REF = "GOAL COMPLETED"
const val DUE_DATE_REF = "DUE DATE"
const val MANAGE_ATTACHMENTS_REF = "MANAGE ATTACHMENTS"
const val TO_UPDATE_POST_REF = "TO UPDATE POST"
const val UPDATE_FIREBASE_REF = "UPDATE FIREBASE"
const val START_GOAL_LIKE_POST_REF = "START GOAL LIKE POST"
const val USERS_TO_DISPLAY_REF = "USERS TO DISPLAY"
const val FOR_POST_LIKES_REF = "FOR POST LIKES"
const val FOR_ALL_TOTALS_REF = "FOR ALL TOTALS"
const val FOR_PERCENT_TOTALS_REF = "FOR PERCENT TOTALS"
const val FOR_MANAGE_FOLLOWERS_REF = "FOR MANAGE FOLLOWERS"
const val FOR_MANAGE_BLOCKED_USERS_REF = "FOR MANAGE BLOCKED USERS"
const val FOR_MANAGE_BANNED_USERS_REF = "FOR MANAGE BANNED USERS"
const val FOR_RESOLVED_REPORTS_REF = "FOR RESOLVED REPORTS"
const val FOR_POST_LIKES_NOTIFS_REF = "FOR POST LIKES NOTIFS"
const val INCLUDE_POST_WITH_LIKES_REF = "INCLUDE POST WITH LIKES"
const val ACTION_FOR_MISSED_DUE_TIME_REF = "ACTION FOR MISSED DUE TIME"
const val AUTO_POST_INCOMPLETE_REF = "AUTO POST INCOMPLETE"
const val SET_GOAL_INACTIVE_REF = "SET GOAL INACTIVE"
const val SAVED_TIME_REF = "SAVED TIME"

const val PROFILE_INFO_UPDATED_REF = "PROFILE INFO UPDATED"

const val PHOTOS_FOLDER_NAME = "Totals"
const val PHOTO_TITLE = "totals_photo"
const val PHOTO_DESC = "from_totals_app"

const val URL_REF_LABEL = "URL_"
const val USER_ID_REF_LABEL = "USER_ID_"
const val PHOTO_FILENAME_LABEL = "PHOTO_"
const val USER_ID_FILENAME_LABEL = "USER_ID_"
const val PROFILE_NOTIF_DATE_LABEL = "PROFILE_NOTIF_DATE_"
const val PROFILE_SEEN_POSTS_LABEL = "PROFILE_SEEN_POSTS_"

const val ACCESS_NUMBER = "+19167084879"
const val ACCESS_CODE = "SUCCESS"

const val LEGAL_POLICIES_SITE = "totalsproductivityapp.square.site"