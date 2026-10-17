package com.example.util

import android.content.Context
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return BiometricPromptHelper.isAvailable(context)
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock with Biometrics",
        subtitle: String = "Scan your fingerprint or face to continue",
        negativeButtonText: String = "Use PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val helper = BiometricPromptHelper(activity)
        helper.authenticateOnStartup(
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            onSuccess = onSuccess,
            onError = { _, errString -> onError(errString) }
        )
    }
}
