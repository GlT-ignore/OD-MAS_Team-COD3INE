package com.example.odmas.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    fun authenticate(
        context: Context,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        confirmationRequired: Boolean = false,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
        onCancel: () -> Unit
    ): Boolean {
        val activity = (context.findActivity() as? FragmentActivity) ?: return false
        val can = BiometricManager.from(context).canAuthenticate()
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onCancel()
                } else onFailure()
            }
            override fun onAuthenticationFailed() { onFailure() }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setConfirmationRequired(confirmationRequired)
            .setDeviceCredentialAllowed(true)
        subtitle?.let { infoBuilder.setSubtitle(it) }
        description?.let { infoBuilder.setDescription(it) }
        val promptInfo = infoBuilder.build()
        prompt.authenticate(promptInfo)
        return true
    }
}

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}


