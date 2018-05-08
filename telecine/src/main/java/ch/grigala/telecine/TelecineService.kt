package ch.grigala.telecine

import android.app.Notification
import android.app.Notification.PRIORITY_MIN
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.support.v4.content.ContextCompat
import ch.grigala.telecine.annotations.*
import com.jakewharton.telecine.R
import com.jakewharton.telecine.TelecineService
import com.nightlynexus.demomode.*
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class TelecineService : Service() {

    @Inject
    @ShowCountdown
    internal var showCountdownProvider: Provider<Boolean>? = null

    @Inject
    @VideoSizePercentage
    internal var videoSizePercentageProvider: Provider<Int>? = null
    @Inject
    @RecordingNotification
    internal var recordingNotificationProvider: Provider<Boolean>? = null
    @Inject
    @ShowTouches
    internal var showTouchesProvider: Provider<Boolean>? = null
    @Inject
    @UseDemoMode
    internal var useDemoModeProvider: Provider<Boolean>? = null

    @Inject
    internal var analytics: Analytics? = null
    @Inject
    internal var contentResolver: ContentResolver? = null

    private var running: Boolean = false
    private var recordingSession: RecordingSession? = null

    private val listener = object : RecordingSession.Listener {
        private var showTouches: Boolean = false
        private var useDemoMode: Boolean = false

        override fun onPrepare() {
            showTouches = showTouchesProvider!!.get()
            useDemoMode = useDemoModeProvider!!.get()
            if (useDemoMode) {
                sendBroadcast(BarsBuilder().mode(BarsBuilder.BarsMode.TRANSPARENT).build())
                sendBroadcast(BatteryBuilder().level(100).plugged(false).build())
                sendBroadcast(ClockBuilder().setTimeInHoursAndMinutes("1200").build())
                sendBroadcast(NetworkBuilder().airplane(false)
                        .carrierNetworkChange(false)
                        .mobile(true, NetworkBuilder.Datatype.LTE, 0, 4)
                        .nosim(true)
                        .build())
                sendBroadcast(NotificationsBuilder().visible(false).build())
                sendBroadcast(SystemIconsBuilder().alarm(false)
                        .bluetooth(SystemIconsBuilder.BluetoothMode.HIDE)
                        .cast(false)
                        .hotspot(false)
                        .location(false)
                        .mute(false)
                        .speakerphone(false)
                        .tty(false)
                        .vibrate(false)
                        .zen(SystemIconsBuilder.ZenMode.HIDE)
                        .build())
                sendBroadcast(WifiBuilder().fully(true).wifi(true, 4).build())
            }
        }

        override fun onStart() {
            if (showTouches) {
                Settings.System.putInt(contentResolver, SHOW_TOUCHES, 1)
            }

            if (!recordingNotificationProvider!!.get()) {
                return  // No running notification was requested.
            }

            val context = applicationContext
            val title = context.getString(R.string.notification_recording_title)
            val subtitle = context.getString(R.string.notification_recording_subtitle)
            val notification = Notification.Builder(context) //
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setSmallIcon(R.drawable.ic_videocam_white_24dp)
                    .setColor(ContextCompat.getColor(context, R.color.primary_normal))
                    .setAutoCancel(true)
                    .setPriority(PRIORITY_MIN)
                    .build()

            Timber.d("Moving service into the foreground with recording notification.")
            startForeground(NOTIFICATION_ID, notification)
        }

        override fun onStop() {
            if (showTouches) {
                Settings.System.putInt(contentResolver, SHOW_TOUCHES, 0)
            }
            if (useDemoMode) {
                sendBroadcast(DemoMode.buildExit())
            }
        }

        override fun onEnd() {
            Timber.d("Shutting down.")
            stopSelf()
        }
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (running) {
            Timber.d("Already running! Ignoring...")
            return Service.START_NOT_STICKY
        }
        Timber.d("Starting up!")
        running = true

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
        if (resultCode == 0 || data == null) {
            throw IllegalStateException("Result code or data missing.")
        }

        recordingSession = RecordingSession(
                this,
                listener,
                resultCode,
                data,
                analytics!!,
                showCountdownProvider!!,
                videoSizePercentageProvider!!)

        recordingSession!!.showOverlay()

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        recordingSession!!.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        throw AssertionError("Not supported.")
    }

    companion object {
        private val EXTRA_RESULT_CODE = "result-code"
        private val EXTRA_DATA = "data"
        private val NOTIFICATION_ID = 99118822
        private val SHOW_TOUCHES = "show_touches"

        internal fun newIntent(context: Context, resultCode: Int, data: Intent): Intent {
            val intent = Intent(context, TelecineService::class.java)
            intent.putExtra(EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(EXTRA_DATA, data)
            return intent
        }
    }
}
