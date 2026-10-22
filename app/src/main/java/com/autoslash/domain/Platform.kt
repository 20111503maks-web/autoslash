package com.autoslash.domain

/**
 * A short-video app the session can scroll through.
 *
 * Everything the session engine, timer, stats and swipe code need is shared; a platform only
 * describes what really differs between apps: which packages it ships as, how to open it and
 * whether the engine must first confirm the right screen (the Shorts feed) is visible.
 */
enum class Platform(
    val displayName: String,
    /** Official package names of the app; the first one is preferred when launching. */
    val packageNames: List<String>,
    /** Optional deep link that opens the short-video feed directly in the official app. */
    val feedDeepLink: String?,
    /** True when the app has other screens where vertical swipes would do the wrong thing. */
    val requiresFeedConfirmation: Boolean,
) {
    TIKTOK(
        displayName = "TikTok",
        packageNames = listOf(
            "com.zhiliaoapp.musically", // TikTok (global)
            "com.ss.android.ugc.trill", // TikTok (some regions)
        ),
        feedDeepLink = null,
        requiresFeedConfirmation = false,
    ),
    YOUTUBE_SHORTS(
        displayName = "YouTube Shorts",
        packageNames = listOf("com.google.android.youtube"),
        feedDeepLink = "https://www.youtube.com/shorts",
        requiresFeedConfirmation = true,
    );

    fun owns(packageName: CharSequence?): Boolean =
        packageName != null && packageNames.any { it.contentEquals(packageName) }

    companion object {
        val DEFAULT = TIKTOK

        /** Every package the accessibility service may act in. */
        val allPackages: Set<String> = entries.flatMap { it.packageNames }.toSet()

        fun fromName(name: String?): Platform = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
