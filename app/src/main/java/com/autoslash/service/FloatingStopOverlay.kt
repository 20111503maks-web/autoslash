package com.autoslash.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.autoslash.R
import com.autoslash.domain.Platform
import com.autoslash.session.SessionState
import com.autoslash.session.SessionStatus
import com.autoslash.session.formatClock
import com.autoslash.ui.labelRes
import kotlin.math.abs

/**
 * Small draggable panel drawn above TikTok / YouTube with a big STOP button and live stats.
 * Uses TYPE_ACCESSIBILITY_OVERLAY, so no "draw over other apps" permission is required.
 */
class FloatingStopOverlay(
    private val context: AccessibilityService,
    private val onStop: () -> Unit,
    private val onConfirmFeed: () -> Unit,
    /** Returns true when the like gesture was dispatched. */
    private val onLike: () -> Boolean,
    private val onTogglePause: () -> Unit,
) {
    private val windowManager: WindowManager = requireNotNull(context.getSystemService(WindowManager::class.java))
    private val density = context.resources.displayMetrics.density
    private var root: LinearLayout? = null
    private lateinit var dot: View
    private lateinit var title: TextView
    private lateinit var stats: TextView
    private lateinit var confirmButton: TextView
    private lateinit var likeButton: TextView
    private lateinit var pauseButton: TextView

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = dp(12)
        y = dp(96)
    }

    fun show(state: SessionState) {
        val view = root ?: createView().also {
            windowManager.addView(it, params)
            root = it
        }
        bind(state)
        view.visibility = View.VISIBLE
    }

    fun hide() {
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
    }

    private fun bind(state: SessionState) {
        val platform = state.platform ?: Platform.DEFAULT
        title.text = platform.displayName
        stats.text = context.getString(
            R.string.overlay_stats,
            formatClock(state.elapsedMillis),
            state.videosViewed,
            context.getString(state.status.labelRes()),
        )
        (dot.background as GradientDrawable).setColor(statusColor(state.status))
        likeButton.text = context.getString(R.string.overlay_like, state.likes)
        pauseButton.text = context.getString(
            if (state.status == SessionStatus.PAUSED_BY_USER) R.string.overlay_resume else R.string.overlay_pause,
        )
        confirmButton.visibility = if (state.status == SessionStatus.WAITING_FOR_FEED) View.VISIBLE else View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createView(): LinearLayout {
        dot = View(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(8) }
        }
        title = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        stats = TextView(context).apply {
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 11f
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(stats)
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(dot)
            addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val stopButton = pillButton(context.getString(R.string.overlay_stop), STOP_RED).apply {
            setOnClickListener { onStop() }
        }
        confirmButton = pillButton(context.getString(R.string.overlay_confirm_shorts), CONFIRM_BLUE).apply {
            visibility = View.GONE
            setOnClickListener {
                onConfirmFeed()
                visibility = View.GONE
            }
        }
        likeButton = pillButton(context.getString(R.string.overlay_like, 0), LIKE_PINK).apply {
            setOnClickListener { view -> if (onLike()) pop(view) else nudge(view) }
        }
        pauseButton = pillButton(context.getString(R.string.overlay_pause), PAUSE_GREY).apply {
            setOnClickListener { onTogglePause() }
        }
        val actions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(likeButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(
                pauseButton,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) },
            )
        }
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            minimumWidth = dp(168)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xE6161622.toInt())
                setStroke(dp(1), 0x33FFFFFF)
            }
            elevation = dp(8).toFloat()
            addView(header)
            addView(confirmButton, buttonParams())
            addView(actions, buttonParams())
            addView(stopButton, buttonParams())
        }
        panel.setOnTouchListener(DragListener())
        return panel
    }

    private fun pillButton(label: String, color: Int) = TextView(context).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(color)
        }
        isClickable = true
    }

    /** Heart "pop" when the like went through. */
    private fun pop(view: View) {
        view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(110).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
        }.start()
    }

    /** Small shake when liking is not possible right now (not in the feed, mid-swipe). */
    private fun nudge(view: View) {
        view.animate().translationX(dp(6).toFloat()).setDuration(50).withEndAction {
            view.animate().translationX(-dp(6).toFloat()).setDuration(80).withEndAction {
                view.animate().translationX(0f).setDuration(50).start()
            }.start()
        }.start()
    }

    private fun buttonParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(8) }

    /** Lets the user move the panel so it never covers something important. */
    private inner class DragListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (abs(dx) > 4 || abs(dy) > 4) {
                        // Gravity is END, so moving right means a smaller x offset.
                        params.x = (startX - dx).toInt().coerceAtLeast(0)
                        params.y = (startY + dy).toInt().coerceAtLeast(0)
                        root?.let { windowManager.updateViewLayout(it, params) }
                    }
                }
            }
            return true
        }
    }

    private fun dp(value: Int) = (value * density).toInt()

    companion object {
        private const val STOP_RED = 0xFFE53950.toInt()
        private const val CONFIRM_BLUE = 0xFF3D5AFE.toInt()
        private const val LIKE_PINK = 0xFFFE2C55.toInt()
        private const val PAUSE_GREY = 0xFF34344A.toInt()

        private fun statusColor(status: SessionStatus): Int = when (status) {
            SessionStatus.WATCHING -> 0xFF2EE59D.toInt()
            SessionStatus.SWIPING -> 0xFF25F4EE.toInt()
            SessionStatus.WAITING_FOR_APP, SessionStatus.WAITING_FOR_FEED -> 0xFFFFC542.toInt()
            SessionStatus.PAUSED_OUTSIDE_APP, SessionStatus.PAUSED_BY_USER -> 0xFFFF8A3D.toInt()
            else -> 0xFF9E9EAE.toInt()
        }
    }
}
