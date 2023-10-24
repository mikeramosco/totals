package com.justanotherdeveloper.totalslite

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit


// tutorial source: https://www.youtube.com/watch?v=BWseVs2MXaI&list=PLDnkoPIBPhqsWYg2S467DKbzO-8TxPIVb&index=1&t=1354s
class PhoneNumberAuthentication(private val enterNumberPage: EnterPhoneNumberPageActivity? = null,
                                private val verificationPage: EnterVerificationCodePageActivity? = null,
                                private val verificationId: String = "") {

    private val auth = FirebaseAuth.getInstance()
    private var authInProgress = false

    fun authInProgress(): Boolean {
        return authInProgress
    }

    fun sendVerificationCode() {
        authInProgress = true
        enterNumberPage?.getView()?.toggleErrorTextVisibility()
        val fullPhoneNumber = enterNumberPage?.getView()?.getFullPhoneNumber()?: return

        // source: https://firebase.google.com/docs/auth/android/phone-auth?authuser=0&hl=en
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(enterNumberPage)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(verificationCode: String) {
        if(verificationId.isEmpty()) return
        val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
        signInByCredentials(credential)
    }

    private fun signInByCredentials(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                val task = it
                if(task.isSuccessful)
                    verificationPage?.checkIfAccountExists()
                else {
                    verificationPage?.getView()?.restartResendCodeTimer()
                    verificationPage?.getView()?.showErrorText()
                }
            }
    }

    fun resendVerificationCode(token: PhoneAuthProvider.ForceResendingToken?) {
        authInProgress = true
        enterNumberPage?.getView()?.toggleErrorTextVisibility()
        val fullPhoneNumber = enterNumberPage?.getView()?.getFullPhoneNumber()?: return
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            fullPhoneNumber,  // Phone number to verify
            60,  // Timeout duration
            TimeUnit.SECONDS,  // Unit of timeout
            enterNumberPage,  // Activity (for callback binding)
            callbacks,  // OnVerificationStateChangedCallbacks
            token) // ForceResendingToken from callbacks
    }

    // source: https://firebase.google.com/docs/auth/android/phone-auth?authuser=0&hl=en
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            // Log.d(TAG, "onVerificationCompleted:$credential")
            // addDebugMsg("onVerificationCompleted:$credential")
            // signInWithPhoneAuthCredential(credential)

            // val verificationCode = credential.smsCode
            // if(verificationCode != null) verifyCode(verificationCode)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            // Log.w(TAG, "onVerificationFailed", e)
            // addDebugMsg("onVerificationFailed:$e")
            authInProgress = false
            addDebugMsg(e.toString())
            enterNumberPage?.getView()?.toggleErrorTextVisibility()
            enterNumberPage?.getView()?.toggleLoadingContinueButton()

            // if (e is FirebaseAuthInvalidCredentialsException) {
            //     // Invalid request
            // } else if (e is FirebaseTooManyRequestsException) {
            //     // The SMS quota for the project has been exceeded
            // }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            // Log.d(TAG, "onCodeSent:$verificationId")
            // addDebugMsg("onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            // storedVerificationId = verificationId
            // resendToken = token

            super.onCodeSent(verificationId, token)
            authInProgress = false
            enterNumberPage?.codeSentSuccessfully(verificationId, token)
            // verificationId = id
        }
    }
}