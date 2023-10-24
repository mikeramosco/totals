package com.justanotherdeveloper.totalslite

import android.graphics.Bitmap
import android.view.View
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class TotalsAccountEditor(private val verificationPage: EnterVerificationCodePageActivity? = null,
                          private val usernamePage: CreateUsernamePageActivity? = null,
                          private val editProfilePage: EditProfileInfoPageActivity? = null,
                          private val sharedPage: SharedPageFragment? = null,
                          private val manageBlockedUsersPage: UsersDisplayPageActivity? = null,
                          private val manageBannedUsersPage: UsersDisplayPageActivity? = null,
                          private val profileXPage: ProgressPageXActivity? = null) {

    private var foundUsername = ""
    private var accountManagementInProgress = false

    fun banUser(user: TotalsUser, postView: View, post: GoalProgress, note: String) {
        val fb = FirebaseDatabase.getInstance().reference
        val bannedUsersTable = fb.child(BANNED_PATH)
        bannedUsersTable.child(user.getUserId().toString()).setValue(true)
            .addOnSuccessListener {
                profileXPage?.removeFromReported(postView, post, note)
            }.addOnFailureListener {
                profileXPage?.showRequestUnavailableToast()
            }
    }

    fun unbanUser(user: TotalsUser) {
        val fb = FirebaseDatabase.getInstance().reference
        val bannedUsersTable = fb.child(BANNED_PATH)
        bannedUsersTable.child(user.getUserId().toString()).setValue(null)
            .addOnSuccessListener {
                manageBannedUsersPage?.showToast("User unbanned")
            }.addOnFailureListener {
                manageBannedUsersPage?.showRequestUnavailableToast()
            }
    }

    fun updateBlockedUsers(user: TotalsUser) {
        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(USERS_PATH)
        val userRef = usersTable.child(user.getDatabaseReferenceKey())
        userRef.child(BLOCKED_USERS_PATH).setValue(user.getBlockedUsersDataMap())
            .addOnFailureListener {
                manageBlockedUsersPage?.showRequestUnavailableToast()
                sharedPage?.activity?.showRequestUnavailableToast()
            }
    }

    fun checkIfAccountExists(fullPhoneNumber: String) {
        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(USERS_PATH)
        val usersPhoneNumbers = usersTable.orderByChild(PHONE_NUMBER_PATH).equalTo(fullPhoneNumber)
        usersPhoneNumbers.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                val dataIterator = data.children.iterator()
                if(dataIterator.hasNext()) {
                    val database = verificationPage?.getDatabase()
                    if(database != null) {
                        val totalsUser = dataIterator.next().toTotalsUser()
                        setSignedInTotalsUser(database, totalsUser, true)
                        if(totalsUser.hasProfilePhotoUrl() && verificationPage != null)
                            retrieveBitmapAndSetAsStaticData(verificationPage, totalsUser)
                    }
                    verificationPage?.signInWithPhoneNumber()
                } else verificationPage?.openCreateUsernamePage()
            }

            override fun onCancelled(error: DatabaseError) {
                verificationPage?.getView()?.restartResendCodeTimer()
                verificationPage?.showRequestUnavailableToast()
            }
        })
    }

    fun checkAvailability(usernameToCheck: String) {
        if(accountManagementInProgress) return
        accountManagementInProgress = true

        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(USERS_PATH)
        val usersPhoneNumbers = usersTable.orderByChild(USERNAME_PATH).equalTo(usernameToCheck)
        usersPhoneNumbers.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                if(data.children.iterator().hasNext()) {
                    editProfilePage?.getView()?.setUsernameTakenErrorTextVisible()
                    editProfilePage?.getView()?.setProfileInfoUpdateCancelled(false)
                    usernamePage?.getView()?.setUsernameTakenErrorTextVisible()
                    usernamePage?.getView()?.hideProgressCircle()
                    accountManagementInProgress = false
                } else if(usernamePage != null) {
                    foundUsername = usernameToCheck
                    initCreateAccount()
                } else {
                    editProfilePage?.continueCheckingForNewProfileInfo(usernameToCheck)
                    accountManagementInProgress = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                editProfilePage?.getView()?.setProfileInfoUpdateCancelled()
                usernamePage?.getView()?.hideProgressCircle()
                usernamePage?.showRequestUnavailableToast()
                accountManagementInProgress = false
            }
        })
    }

    private fun initCreateAccount() {
        accountManagementInProgress = true
        usernamePage?.getView()?.showProgressCircle()

        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(USERS_PATH)
        val usersPhoneNumbers = usersTable.orderByChild(USER_ID_PATH)
        usersPhoneNumbers.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                createAccount(createNewUserId(data))
            }

            override fun onCancelled(error: DatabaseError) {
                accountManagementInProgress = false
                usernamePage?.getView()?.hideProgressCircle()
                usernamePage?.showRequestUnavailableToast()
            }
        })
    }

    private fun createAccount(userId: Int) {
        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(USERS_PATH)
        val newUser = usersTable.push()
        val newUserData = HashMap<String, Any>()
        newUserData[DATE_JOINED_PATH] = getTimeStamp()
        newUserData[USER_ID_PATH] = userId
        newUserData[USERNAME_PATH] = foundUsername
        if(usernamePage != null) {
            val name = usernamePage.getName()
            val birthday = usernamePage.getBirthday()
            val fullPhoneNumber = usernamePage.getFullPhoneNumber()?: ""

            newUserData[NAME_PATH] = name
            newUserData[BIRTHDAY_PATH] = birthday
            newUserData[PHONE_NUMBER_PATH] = fullPhoneNumber

            newUser.updateChildren(newUserData).addOnSuccessListener {
                val revealUser = TotalsUser(newUser.key.toString(),
                    userId, foundUsername, name)
                usernamePage.signInWithPhoneNumber(revealUser)
            }.addOnFailureListener {
                accountManagementInProgress = false
                usernamePage.getView().hideProgressCircle()
                usernamePage.showRequestUnavailableToast()
            }
        }
    }

    private fun createNewUserId(data: DataSnapshot): Int {
        var idExists = true
        var userId = generateId()
        while (idExists) {
            idExists = false
            val dataIterator = data.children.iterator()
            while (dataIterator.hasNext()) {
                val nextData = dataIterator.next()
                val id = nextData.child(USER_ID_PATH).value.toString().toInt()
                if (userId == id) {
                    idExists = true
                    break
                }
            }
            if (idExists) userId = generateId()
        }
        return userId
    }

    fun uploadNewProfilePhoto(profilePhoto: Bitmap,
                              currentProfileInfo: TotalsUser,
                              newProfileInfo: TotalsUser) {
        if(accountManagementInProgress) return
        accountManagementInProgress = true
        val storage = FirebaseStorage.getInstance().getReference(PROFILE_PHOTOS_LOCATION)
        val fileReference = storage.child(getTimeStamp().toString())
        val stream = ByteArrayOutputStream()
        profilePhoto.compress(Bitmap.CompressFormat.JPEG, 25, stream)
        val data = stream.toByteArray()
        val uploadTask = fileReference.putBytes(data)
        uploadTask.addOnSuccessListener {
            val result = it.storage.downloadUrl
            result.addOnSuccessListener { uri ->
                accountManagementInProgress = false
                newProfileInfo.setProfilePhotoUrl(uri.toString())
                newProfileInfo.circleCropProfilePhoto()
                updateProfileInfo(currentProfileInfo, newProfileInfo)
            }.addOnFailureListener {
                accountManagementInProgress = false
                editProfilePage?.getView()?.setProfileInfoUpdateCancelled()
            }
        }.addOnFailureListener {
            accountManagementInProgress = false
            editProfilePage?.getView()?.setProfileInfoUpdateCancelled()
        }
    }

    fun updateProfileInfo(currentProfileInfo: TotalsUser,
                          newProfileInfo: TotalsUser) {
        if(accountManagementInProgress) return
        accountManagementInProgress = true
        val fb = FirebaseDatabase.getInstance().reference
        val usersTable = fb.child(USERS_PATH)
        val userRef = usersTable.child(currentProfileInfo.getDatabaseReferenceKey())
        val newUserData = HashMap<String, Any>()
        if(currentProfileInfo.hasNewName(newProfileInfo))
            newUserData[NAME_PATH] = newProfileInfo.getName()
        if(currentProfileInfo.hasNewUsername(newProfileInfo))
            newUserData[USERNAME_PATH] = newProfileInfo.getUsername()
        if(currentProfileInfo.hasNewProfileBio(newProfileInfo))
            newUserData[PROFILE_BIO_PATH] = newProfileInfo.getProfileBio()
        if(currentProfileInfo.hasNewProfilePhoto(newProfileInfo))
            newUserData[PROFILE_PHOTO_URL_PATH] = newProfileInfo.getProfilePhotoUrl()
        userRef.updateChildren(newUserData).addOnSuccessListener {
            if(editProfilePage != null) {
                newProfileInfo.setProfilePhotoBitmap(null)
                setSignedInTotalsUser(editProfilePage.getDatabase(), newProfileInfo)
                if(currentProfileInfo.hasNewProfilePhoto(newProfileInfo))
                    deleteProfilePhotoFromDatabase(currentProfileInfo)
                editProfilePage.returnToPreviousPage(true)
            }
            accountManagementInProgress = false
        }.addOnFailureListener {
            accountManagementInProgress = false
            editProfilePage?.getView()?.setProfileInfoUpdateCancelled()
        }
    }

    private fun deleteProfilePhotoFromDatabase(currentProfileInfo: TotalsUser) {
        val oldUrl = currentProfileInfo.getProfilePhotoUrl()
        if(oldUrl.isNotEmpty()) FirebaseStorage.getInstance()
            .getReferenceFromUrl(oldUrl).delete()
    }

    fun accountManagementInProgress(): Boolean {
        return accountManagementInProgress
    }

}