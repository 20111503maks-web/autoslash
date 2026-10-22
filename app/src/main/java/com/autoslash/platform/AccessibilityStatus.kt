package com.autoslash.platform

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.autoslash.service.AutoScrollService

object AccessibilityStatus {

    /** True when the user has enabled our service in system Accessibility settings. */
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, AutoScrollService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
