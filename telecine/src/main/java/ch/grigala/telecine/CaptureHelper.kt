package ch.grigala.telecine

import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.google.android.gms.analytics.HitBuilders
import timber.log.Timber

internal class CaptureHelper private constructor() {

    init {
        throw AssertionError("No instances.")
    }

    companion object {
        private val CREATE_SCREEN_CAPTURE = 4242

        fun fireScreenCaptureIntent(activity: Activity, analytics: Analytics) {
            val manager = activity.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val intent = manager.createScreenCaptureIntent()
            activity.startActivityForResult(intent, CREATE_SCREEN_CAPTURE)

            analytics.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CAPTURE_INTENT_LAUNCH)
                    .build())
        }

        fun handleActivityResult(activity: Activity, requestCode: Int, resultCode: Int,
                                 data: Intent, analytics: Analytics): Boolean {
            if (requestCode != CREATE_SCREEN_CAPTURE) {
                return false
            }

            if (resultCode == Activity.RESULT_OK) {
                Timber.d("Acquired permission to screen capture. Starting service.")
                activity.startService(TelecineService.newIntent(activity, resultCode, data))
            } else {
                Timber.d("Failed to acquire permission to screen capture.")
            }

            analytics.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CAPTURE_INTENT_RESULT)
                    .setValue(resultCode.toLong())
                    .build())

            return true
        }
    }
}