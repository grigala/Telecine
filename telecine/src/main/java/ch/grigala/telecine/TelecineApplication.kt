package ch.grigala.telecine

import android.app.Activity
import android.app.Application
import android.app.Service
import com.bugsnag.android.Bugsnag
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import timber.log.Timber
import javax.inject.Inject

class TelecineApplication : Application(), HasActivityInjector, HasServiceInjector {
    @Inject
    private var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>? = null
    @Inject
    private var dispatchingServiceInjector: DispatchingAndroidInjector<Service>? = null

    override fun onCreate() {
        //DaggerTelecineComponent.builder().application(this).build().inject(this)
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Bugsnag.init(this, BuildConfig.BUGSNAG_KEY)
            Bugsnag.setReleaseStage(BuildConfig.BUILD_TYPE)
            Bugsnag.setProjectPackages("com.jakewharton.telecine")

            val tree = BugsnagTree()
            Bugsnag.getClient().beforeNotify { error ->
                tree.update(error)
                true
            }

            Timber.plant(tree)
        }
    }

    override fun activityInjector(): AndroidInjector<Activity>? {
        return dispatchingActivityInjector
    }

    override fun serviceInjector(): AndroidInjector<Service>? {
        return dispatchingServiceInjector
    }
}
