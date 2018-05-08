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
        val CATEGORY_SETTINGS = "Settings"
        val CATEGORY_RECORDING = "Recording"
        val CATEGORY_SHORTCUT = "Shortcut"
        val CATEGORY_QUICK_TILE = "Quick Tile"

        val ACTION_CAPTURE_INTENT_LAUNCH = "Launch Overlay Launch"
        val ACTION_CAPTURE_INTENT_RESULT = "Launch Overlay Result"
        val ACTION_CHANGE_VIDEO_SIZE = "Change Video Size"
        val ACTION_CHANGE_SHOW_COUNTDOWN = "Show Countdown"
        val ACTION_CHANGE_HIDE_RECENTS = "Hide In Recents"
        val ACTION_CHANGE_RECORDING_NOTIFICATION = "Recording Notification"
        val ACTION_CHANGE_SHOW_TOUCHES = "Show Touches"
        val ACTION_CHANGE_USE_DEMO_MODE = "Use Demo Mode"
        val ACTION_OVERLAY_SHOW = "Overlay Show"
        val ACTION_OVERLAY_HIDE = "Overlay Hide"
        val ACTION_OVERLAY_CANCEL = "Overlay Cancel"
        val ACTION_RECORDING_START = "Recording Start"
        val ACTION_RECORDING_STOP = "Recording Stop"
        val ACTION_SHORTCUT_ADDED = "Shortcut Added"
        val ACTION_SHORTCUT_LAUNCHED = "Shortcut Launched"
        val ACTION_QUICK_TILE_ADDED = "Quick Tile Added"
        val ACTION_QUICK_TILE_LAUNCHED = "Quick Tile Launched"
        val ACTION_QUICK_TILE_REMOVED = "Quick Tile Removed"

        val VARIABLE_RECORDING_LENGTH = "Recording Length"
    }
}