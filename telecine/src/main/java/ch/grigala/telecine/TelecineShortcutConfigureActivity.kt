package ch.grigala.telecine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.analytics.HitBuilders
import dagger.android.AndroidInjection
import javax.inject.Inject

class TelecineShortcutConfigureActivity : Activity() {
    @Inject
    internal var analytics: Analytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        analytics!!.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_SHORTCUT) //
                .setAction(Analytics.ACTION_SHORTCUT_ADDED) //
                .build())

        val launchIntent = Intent(this, TelecineShortcutLaunchActivity::class.java)
        val icon = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher)

        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.shortcut_name))
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon)
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)

        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}