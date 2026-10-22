package com.autoslash.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.autoslash.domain.Platform

sealed interface LaunchResult {
    /** The app was opened. [feedOpenedDirectly] is true when a deep link to the feed was used. */
    data class Opened(val packageName: String, val feedOpenedDirectly: Boolean) : LaunchResult
    data object NotInstalled : LaunchResult
}

/** Opens the official app of a [Platform] using only public, supported intents. */
class PlatformLauncher(private val context: Context) {

    fun installedPackage(platform: Platform): String? =
        platform.packageNames.firstOrNull { isInstalled(it) }

    fun launch(platform: Platform): LaunchResult {
        val pkg = installedPackage(platform) ?: return LaunchResult.NotInstalled

        platform.feedDeepLink?.let { link ->
            val viewFeed = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                .setPackage(pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(viewFeed)
                return LaunchResult.Opened(pkg, feedOpenedDirectly = true)
            } catch (_: ActivityNotFoundException) {
                // Fall back to the regular launcher entry below.
            }
        }

        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return LaunchResult.NotInstalled
        return try {
            context.startActivity(launch)
            LaunchResult.Opened(pkg, feedOpenedDirectly = false)
        } catch (_: ActivityNotFoundException) {
            LaunchResult.NotInstalled
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
