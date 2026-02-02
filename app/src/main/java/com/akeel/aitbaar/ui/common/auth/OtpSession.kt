package com.akeel.aitbaar.ui.common.auth

import com.google.firebase.auth.PhoneAuthProvider

object OtpSession {
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null
}
