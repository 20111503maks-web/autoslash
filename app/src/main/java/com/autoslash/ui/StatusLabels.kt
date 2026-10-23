package com.autoslash.ui

import androidx.annotation.StringRes
import com.autoslash.R
import com.autoslash.session.FinishReason
import com.autoslash.session.SessionStatus

/** Shared by the Compose screen and the floating overlay. */
@StringRes
fun SessionStatus.labelRes(): Int = when (this) {
    SessionStatus.IDLE -> R.string.status_idle
    SessionStatus.WAITING_FOR_APP -> R.string.status_waiting_app
    SessionStatus.WAITING_FOR_FEED -> R.string.status_waiting_feed
    SessionStatus.WATCHING -> R.string.status_watching
    SessionStatus.SWIPING -> R.string.status_swiping
    SessionStatus.PAUSED_OUTSIDE_APP -> R.string.status_paused
    SessionStatus.PAUSED_BY_USER -> R.string.status_paused_by_user
    SessionStatus.FINISHED -> R.string.status_finished
    SessionStatus.STOPPED -> R.string.status_stopped
}

@StringRes
fun FinishReason.labelRes(): Int = when (this) {
    FinishReason.TIME_LIMIT -> R.string.finish_time
    FinishReason.VIDEO_LIMIT -> R.string.finish_videos
    FinishReason.USER_STOP -> R.string.finish_user
    FinishReason.SERVICE_DISCONNECTED -> R.string.finish_service
}
