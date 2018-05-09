package ch.grigala.telecine

import android.app.Application
import android.content.ContentResolver
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import ch.grigala.telecine.annotations.*
import com.google.android.gms.analytics.GoogleAnalytics
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import timber.log.Timber
import javax.inject.Singleton

@Module
internal abstract class TelecineModule {

    @ContributesAndroidInjector
    internal abstract fun contributeTelecineActivity(): TelecineActivity

    @ContributesAndroidInjector
    internal abstract fun contributeTelecineShortcutConfigureActivity(): TelecineShortcutConfigureActivity

    @ContributesAndroidInjector
    internal abstract fun contributeTelecineShortcutLaunchActivity(): TelecineShortcutLaunchActivity

    @ContributesAndroidInjector
    internal abstract fun contributeTelecineService(): TelecineService

    @ContributesAndroidInjector
    internal abstract fun contributeTelecineTileService(): TelecineTileService

    companion object {
        private const val PREFERENCES_NAME = "telecine"
        private const val DEFAULT_SHOW_COUNTDOWN = true
        private const val DEFAULT_HIDE_FROM_RECENTS = false
        private const val DEFAULT_SHOW_TOUCHES = false
        private const val DEFAULT_USE_DEMO_MODE = false
        private const val DEFAULT_RECORDING_NOTIFICATION = false
        private const val DEFAULT_VIDEO_SIZE_PERCENTAGE = 100

        @Provides
        @Singleton
        fun provideAnalytics(app: Application): Analytics {
            if (BuildConfig.DEBUG) {
                return object : Analytics {
                    override fun send(params: Map<String, String>) {
                        Timber.tag("Analytics").d(params.toString())
                    }
                }
            }

            val googleAnalytics = GoogleAnalytics.getInstance(app)
            val tracker = googleAnalytics.newTracker(BuildConfig.ANALYTICS_KEY)
            tracker.setSessionTimeout(300) // ms? s? better be s.
            return Analytics.GoogleAnalytics(tracker)
        }

        @Provides
        @Singleton
        fun provideContentResolver(app: Application): ContentResolver {
            return app.contentResolver
        }

        @Provides
        @Singleton
        fun provideSharedPreferences(app: Application): SharedPreferences {
            return app.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        }

        @Provides
        @Singleton
        @ShowCountdown
        fun provideShowCountdownPreference(prefs: SharedPreferences): BooleanPreference {
            return BooleanPreference(prefs, "show-countdown", DEFAULT_SHOW_COUNTDOWN)
        }

        @Provides
        @ShowCountdown
        fun provideShowCountdown(@ShowCountdown pref: BooleanPreference): Boolean {
            return pref.get()
        }

        @Provides
        @Singleton
        @RecordingNotification
        fun provideRecordingNotificationPreference(prefs: SharedPreferences): BooleanPreference {
            return BooleanPreference(prefs, "recording-notification", DEFAULT_RECORDING_NOTIFICATION)
        }

        @Provides
        @RecordingNotification
        fun provideRecordingNotification(@RecordingNotification pref: BooleanPreference): Boolean {
            return pref.get()
        }

        @Provides
        @Singleton
        @HideFromRecents
        fun provideHideFromRecentsPreference(prefs: SharedPreferences): BooleanPreference {
            return BooleanPreference(prefs, "hide-from-recents", DEFAULT_HIDE_FROM_RECENTS)
        }

        @Provides
        @Singleton
        @ShowTouches
        fun provideShowTouchesPreference(prefs: SharedPreferences): BooleanPreference {
            return BooleanPreference(prefs, "show-touches", DEFAULT_SHOW_TOUCHES)
        }

        @Provides
        @ShowTouches
        fun provideShowTouches(@ShowTouches pref: BooleanPreference): Boolean {
            return pref.get()
        }

        @Provides
        @Singleton
        @UseDemoMode
        fun provideUseDemoModePreference(prefs: SharedPreferences): BooleanPreference {
            return BooleanPreference(prefs, "use-demo-mode", DEFAULT_USE_DEMO_MODE)
        }

        @Provides
        @UseDemoMode
        fun provideUseDemoMode(@UseDemoMode pref: BooleanPreference): Boolean {
            return pref.get()
        }

        @Provides
        @Singleton
        @VideoSizePercentage
        fun provideVideoSizePercentagePreference(prefs: SharedPreferences): IntPreference {
            return IntPreference(prefs, "video-size", DEFAULT_VIDEO_SIZE_PERCENTAGE)
        }

        @Provides
        @VideoSizePercentage
        fun provideVideoSizePercentage(@VideoSizePercentage pref: IntPreference): Int {
            return pref.get()
        }
    }
}