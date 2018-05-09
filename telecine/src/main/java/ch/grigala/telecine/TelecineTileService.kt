package ch.grigala.telecine

import android.annotation.TargetApi
import android.os.Build.VERSION_CODES.N
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.google.android.gms.analytics.HitBuilders
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

@TargetApi(N) // Only created on N+
class TelecineTileService : TileService() {
    @Inject
    internal var analytics: Analytics? = null

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onClick() {
        startActivity(TelecineShortcutLaunchActivity.createQuickTileIntent(this))
    }

    override fun onStartListening() {
        Timber.i("Quick tile started listening")
        val tile = qsTile
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    override fun onStopListening() {
        Timber.i("Quick tile stopped listening")
    }

    override fun onTileAdded() {
        Timber.i("Quick tile added")
        analytics!!.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_QUICK_TILE)
                .setAction(Analytics.ACTION_QUICK_TILE_ADDED)
                .build())
    }

    override fun onTileRemoved() {
        Timber.i("Quick tile removed")
        analytics!!.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_QUICK_TILE)
                .setAction(Analytics.ACTION_QUICK_TILE_REMOVED)
                .build())
    }
}