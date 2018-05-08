package ch.grigala.telecine

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.AsyncTask
import timber.log.Timber

class DeleteRecordingBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(RecordingSession.NOTIFICATION_ID)

        val uri = intent.data
        val contentResolver = context.contentResolver
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg none: Void): Void? {
                val rowsDeleted = contentResolver.delete(uri!!, null, null)
                if (rowsDeleted == 1) {
                    Timber.i("Deleted recording.")
                } else {
                    Timber.e("Error deleting recording.")
                }
                return null
            }
        }.execute()
    }
}