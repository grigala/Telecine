package ch.grigala.telecine

import android.annotation.TargetApi
import android.app.Activity
import android.os.AsyncTask
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.jakewharton.telecine.R
import com.nightlynexus.demomode.DemoMode
import com.nightlynexus.demomode.DemoModeInitializer.DemoModeSetting.ENABLED
import com.nightlynexus.demomode.DemoModeInitializer.GrantPermissionResult.FAILURE
import com.nightlynexus.demomode.DemoModeInitializer.GrantPermissionResult.SUCCESS


internal class DemoModeHelper private constructor() {

    init {
        throw AssertionError("No instances.")
    }

    private enum class DemoModeAvailability {
        AVAILABLE, UNAVAILABLE, NEEDS_ROOT_ACCESS, NEEDS_DEMO_MODE_SETTING
    }

    internal interface ShowDemoModeSetting {
        fun show()

        fun hide()
    }

    companion object {
        private val REQUEST_CODE_ENABLE_DEMO_MODE = 5309

        fun showDemoModeSetting(activity: Activity, callback: ShowDemoModeSetting) {
            if (SDK_INT < M) {
                callback.hide()
                return
            }
            showDemoModeSetting23(activity, callback)
        }

        @TargetApi(M)
        private fun showDemoModeSetting23(activity: Activity,
                                          callback: ShowDemoModeSetting) {
            val demoModeInitializer = DemoMode.initializer(activity)

            object : AsyncTask<Void, Void, DemoModeAvailability>() {
                override fun doInBackground(vararg params: Void): DemoModeAvailability {
                    val grantBroadcastPermissionResult = demoModeInitializer.grantBroadcastPermission()
                    if (grantBroadcastPermissionResult == FAILURE) {
                        return DemoModeAvailability.NEEDS_ROOT_ACCESS
                    }
                    if (grantBroadcastPermissionResult != SUCCESS) {
                        return DemoModeAvailability.UNAVAILABLE
                    }
                    val setDemoModeSettingResult = demoModeInitializer.setDemoModeSetting(ENABLED)
                    return if (setDemoModeSettingResult != SUCCESS) {
                        DemoModeAvailability.NEEDS_DEMO_MODE_SETTING
                    } else DemoModeAvailability.AVAILABLE
                }

                override fun onPostExecute(demoModeAvailability: DemoModeAvailability) {
                    when (demoModeAvailability) {
                        DemoModeHelper.DemoModeAvailability.AVAILABLE -> callback.show()
                        DemoModeHelper.DemoModeAvailability.UNAVAILABLE -> callback.hide()
                        DemoModeHelper.DemoModeAvailability.NEEDS_ROOT_ACCESS -> {
                            callback.hide()
                            Toast.makeText(activity, R.string.root_permission_denied, LENGTH_SHORT).show()
                        }
                        DemoModeHelper.DemoModeAvailability.NEEDS_DEMO_MODE_SETTING -> {
                            callback.hide()
                            Toast.makeText(activity, R.string.enable_demo_mode_in_settings, LENGTH_SHORT).show()
                            activity.startActivityForResult(demoModeInitializer.demoModeScreenIntent(),
                                    REQUEST_CODE_ENABLE_DEMO_MODE)
                        }
                    }
                }
            }.execute()

        }

        @TargetApi(M)
        fun handleActivityResult(activity: Activity, requestCode: Int,
                                 callback: ShowDemoModeSetting): Boolean {
            if (requestCode != REQUEST_CODE_ENABLE_DEMO_MODE) {
                return false
            }
            if (DemoMode.initializer(activity).demoModeSetting == ENABLED) {
                showDemoModeSetting(activity, callback)
            }
            return true
        }
    }
}
