package ch.grigala.telecine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Spinner
import android.widget.Switch
import butterknife.*
import ch.grigala.telecine.annotations.*
import com.google.android.gms.analytics.HitBuilders
import com.jakewharton.telecine.R
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

class TelecineActivity : AppCompatActivity() {
    @BindView(R.id.spinner_video_size_percentage) internal var videoSizePercentageView: Spinner? = null
    @BindView(R.id.switch_show_countdown) internal var showCountdownView: Switch? = null
    @BindView(R.id.switch_hide_from_recents) internal var hideFromRecentsView: Switch? = null
    @BindView(R.id.switch_recording_notification)internal var recordingNotificationView: Switch? = null
    @BindView(R.id.switch_show_touches) internal var showTouchesView: Switch? = null
    @BindView(R.id.container_use_demo_mode) internal var useDemoModeContainerView: View? = null
    @BindView(R.id.switch_use_demo_mode) internal var useDemoModeView: Switch? = null
    @BindView(R.id.launch) internal var launchView: View? = null

    @BindString(R.string.app_name) internal var appName: String? = null
    @BindColor(R.color.primary_normal) internal var primaryNormal: Int = 0

    @Inject
    @VideoSizePercentage
    internal var videoSizePreference: IntPreference? = null
    @Inject
    @ShowCountdown
    internal var showCountdownPreference: BooleanPreference? = null
    @Inject
    @HideFromRecents
    internal var hideFromRecentsPreference: BooleanPreference? = null
    @Inject
    @RecordingNotification
    internal var recordingNotificationPreference: BooleanPreference? = null
    @Inject
    @ShowTouches
    internal var showTouchesPreference: BooleanPreference? = null
    @Inject
    @UseDemoMode
    internal var useDemoModePreference: BooleanPreference? = null

    @Inject
    internal var analytics: Analytics? = null

    private var videoSizePercentageAdapter: VideoSizePercentageAdapter? = null
    private var showDemoModeSetting: DemoModeHelper.ShowDemoModeSetting? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        if ("true" == intent.getStringExtra("crash")) {
            throw RuntimeException("Crash! Bang! Pow! This is only a test...")
        }

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        CheatSheet.setup(launchView!!)

        setTaskDescription(ActivityManager.TaskDescription(appName, rasterizeTaskIcon(), primaryNormal))

        videoSizePercentageAdapter = VideoSizePercentageAdapter(this)

        videoSizePercentageView!!.adapter = videoSizePercentageAdapter
        videoSizePercentageView!!.setSelection(
                VideoSizePercentageAdapter.getSelectedPosition(videoSizePreference!!.get()))

        showCountdownView!!.isChecked = showCountdownPreference!!.get()
        hideFromRecentsView!!.isChecked = hideFromRecentsPreference!!.get()
        recordingNotificationView!!.isChecked = recordingNotificationPreference!!.get()
        showTouchesView!!.isChecked = showTouchesPreference!!.get()
        useDemoModeView!!.isChecked = useDemoModePreference!!.get()
        showDemoModeSetting = object : DemoModeHelper.ShowDemoModeSetting {
            override fun show() {
                useDemoModeContainerView!!.visibility = VISIBLE
            }

            override fun hide() {
                useDemoModeView!!.isChecked = false
                useDemoModeContainerView!!.visibility = GONE
            }
        }
        DemoModeHelper.showDemoModeSetting(this, showDemoModeSetting as DemoModeHelper.ShowDemoModeSetting)
    }

    private fun rasterizeTaskIcon(): Bitmap {
        val drawable = resources.getDrawable(R.drawable.ic_videocam_white_24dp, theme)

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val size = am.launcherLargeIconSize
        val icon = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(icon)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        return icon
    }

    @OnClick(R.id.launch)
    internal fun onLaunchClicked() {
        Timber.d("Attempting to acquire permission to screen capture.")
        CaptureHelper.fireScreenCaptureIntent(this, analytics!!)

    }

    @OnItemSelected(R.id.spinner_video_size_percentage)
    internal fun onVideoSizePercentageSelected(
            position: Int) {
        val newValue = videoSizePercentageAdapter!!.getItem(position)
        val oldValue = videoSizePreference!!.get()
        if (newValue != oldValue) {
            Timber.d("Video size percentage changing to %s%%", newValue)
            videoSizePreference!!.set(newValue)

            analytics!!.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CHANGE_VIDEO_SIZE)
                    .setValue(newValue.toLong())
                    .build())
        }
    }

    @OnCheckedChanged(R.id.switch_show_countdown)
    internal fun onShowCountdownChanged() {
        val newValue = showCountdownView!!.isChecked
        val oldValue = showCountdownPreference!!.get()
        if (newValue != oldValue) {
            Timber.d("Hide show countdown changing to %s", newValue)
            showCountdownPreference!!.set(newValue)

            analytics!!.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CHANGE_SHOW_COUNTDOWN)
                    .setValue((if (newValue) 1 else 0).toLong())
                    .build())
        }
    }

    @OnCheckedChanged(R.id.switch_hide_from_recents)
    internal fun onHideFromRecentsChanged() {
        val newValue = hideFromRecentsView!!.isChecked
        val oldValue = hideFromRecentsPreference!!.get()
        if (newValue != oldValue) {
            Timber.d("Hide from recents preference changing to %s", newValue)
            hideFromRecentsPreference!!.set(newValue)

            analytics!!.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CHANGE_HIDE_RECENTS)
                    .setValue((if (newValue) 1 else 0).toLong())
                    .build())
        }
    }

    @OnCheckedChanged(R.id.switch_recording_notification)
    internal fun onRecordingNotificationChanged() {
        val newValue = recordingNotificationView!!.isChecked
        val oldValue = recordingNotificationPreference!!.get()
        if (newValue != oldValue) {
            Timber.d("Recording notification preference changing to %s", newValue)
            recordingNotificationPreference!!.set(newValue)

            analytics!!.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CHANGE_RECORDING_NOTIFICATION)
                    .setValue((if (newValue) 1 else 0).toLong())
                    .build())
        }
    }

    @OnCheckedChanged(R.id.switch_show_touches)
    internal fun onShowTouchesChanged() {
        val newValue = showTouchesView!!.isChecked
        val oldValue = showTouchesPreference!!.get()
        if (newValue != oldValue) {
            Timber.d("Show touches preference changing to %s", newValue)
            showTouchesPreference!!.set(newValue)

            analytics!!.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CHANGE_SHOW_TOUCHES)
                    .setValue((if (newValue) 1 else 0).toLong())
                    .build())
        }
    }

    @OnCheckedChanged(R.id.switch_use_demo_mode)
    internal fun onUseDemoModeChanged() {
        val newValue = useDemoModeView!!.isChecked
        val oldValue = useDemoModePreference!!.get()
        if (newValue != oldValue) {
            Timber.d("Use demo mode preference changing to %s", newValue)
            useDemoModePreference!!.set(newValue)

            analytics!!.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_SETTINGS)
                    .setAction(Analytics.ACTION_CHANGE_USE_DEMO_MODE)
                    .setValue((if (newValue) 1 else 0).toLong())
                    .build())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (!CaptureHelper.handleActivityResult(this, requestCode, resultCode, data, analytics!!) && !DemoModeHelper.handleActivityResult(this, requestCode, showDemoModeSetting!!)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStop() {
        super.onStop()
        if (hideFromRecentsPreference!!.get() && !isChangingConfigurations) {
            Timber.d("Removing task because hide from recents preference was enabled.")
            finishAndRemoveTask()
        }
    }
}
