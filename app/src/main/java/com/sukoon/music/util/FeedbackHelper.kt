package com.sukoon.music.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast

/**
 * Helper for sending feedback via email.
 *
 * Opens the user's email client with pre-filled support email and device info.
 * Falls back to clipboard copy if no email app is installed.
 */
object FeedbackHelper {

    private const val SUPPORT_EMAIL = "support@shkcorp.com"

    /**
     * Open email client with pre-filled feedback template.
     *
     * Includes device info (app version, Android version, device model) to help
     * with debugging user-reported issues.
     *
     * @param context Android context
     * @param appVersion App version string to include in email
     */
    fun sendFeedback(context: Context, appVersion: String) {
        val deviceInfo = """
            App Version: $appVersion
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Device: ${Build.MANUFACTURER} ${Build.MODEL}

            [Please describe your feedback, issue, or suggestion below]


        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(com.sukoon.music.R.string.feedback_email_subject))
            putExtra(Intent.EXTRA_TEXT, deviceInfo)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(com.sukoon.music.R.string.feedback_chooser_title)
                )
            )
        } else {
            // Fallback: Copy email to clipboard
            copyEmailToClipboard(context)
        }
    }

    /**
     * Copy support email to clipboard with a toast notification.
     *
     * Used when no email client is available on the device.
     */
    private fun copyEmailToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            context.getString(com.sukoon.music.R.string.feedback_clipboard_label),
            SUPPORT_EMAIL
        )
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            context.getString(com.sukoon.music.R.string.feedback_email_copied_toast, SUPPORT_EMAIL),
            Toast.LENGTH_LONG
        ).show()
    }
}
