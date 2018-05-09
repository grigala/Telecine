package ch.grigala.telecine

import com.google.android.gms.analytics.Tracker

internal interface Analytics {

    /**
     * @see {@link Tracker.send
     */
    fun send(params: Map<String, String>)

    class GoogleAnalytics(private val tracker: Tracker) : Analytics {

        override fun send(params: Map<String, String>) {
            tracker.send(params)
        }
    }

    companion object {
        const val CATEGORY_SETTINGS = "Settings"
        const val CATEGORY_RECORDING = "Recording"
        const val CATEGORY_SHORTCUT = "Shortcut"
        const val CATEGORY_QUICK_TILE = "Quick Tile"

        const val ACTION_CAPTURE_INTENT_LAUNCH = "Launch Overlay Launch"
        const val ACTION_CAPTURE_INTENT_RESULT = "Launch Overlay Result"
        const val ACTION_CHANGE_VIDEO_SIZE = "Change Video Size"
        const val ACTION_CHANGE_SHOW_COUNTDOWN = "Show Countdown"
        const val ACTION_CHANGE_HIDE_RECENTS = "Hide In Recents"
        const val ACTION_CHANGE_RECORDING_NOTIFICATION = "Recording Notification"
        const val ACTION_CHANGE_SHOW_TOUCHES = "Show Touches"
        const val ACTION_CHANGE_USE_DEMO_MODE = "Use Demo Mode"
        const val ACTION_OVERLAY_SHOW = "Overlay Show"
        const val ACTION_OVERLAY_HIDE = "Overlay Hide"
        const val ACTION_OVERLAY_CANCEL = "Overlay Cancel"
        const val ACTION_RECORDING_START = "Recording Start"
        const val ACTION_RECORDING_STOP = "Recording Stop"
        const val ACTION_SHORTCUT_ADDED = "Shortcut Added"
        const val ACTION_SHORTCUT_LAUNCHED = "Shortcut Launched"
        const val ACTION_QUICK_TILE_ADDED = "Quick Tile Added"
        const val ACTION_QUICK_TILE_LAUNCHED = "Quick Tile Launched"
        const val ACTION_QUICK_TILE_REMOVED = "Quick Tile Removed"

        const val VARIABLE_RECORDING_LENGTH = "Recording Length"
    }
}