package ch.grigala.telecine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.analytics.HitBuilders
import dagger.android.AndroidInjection
import javax.inject.Inject

class TelecineShortcutLaunchActivity : Activity() {

    @Inject
    internal var analytics: Analytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        var launchAction: String? = intent.getStringExtra(KEY_ACTION)
        if (launchAction == null) {
            launchAction = Analytics.ACTION_SHORTCUT_LAUNCHED
        }

        analytics!!.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_SHORTCUT)
                .setAction(launchAction)
                .build())

        CaptureHelper.fireScreenCaptureIntent(this, analytics!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (!CaptureHelper.handleActivityResult(this, requestCode, resultCode, data, analytics!!)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
        finish()
    }

    override fun onStop() {
        if (!isFinishing) {
            finish()
        }
        super.onStop()
    }

    companion object {
        private val KEY_ACTION = "launch-action"

        internal fun createQuickTileIntent(context: Context): Intent {
            val intent = Intent(context, TelecineShortcutLaunchActivity::class.java)
            intent.putExtra(KEY_ACTION, Analytics.ACTION_QUICK_TILE_LAUNCHED)
            return intent
        }
    }
}