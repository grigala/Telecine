package ch.grigala.telecine

import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Error
import timber.log.Timber
import java.util.*

/**
 * A logging implementation which buffers the last 200 messages and notifies on error exceptions.
 */
internal class BugsnagTree : Timber.Tree() {

    // Adding one to the initial size accounts for the add before remove.
    private val buffer = ArrayDeque<String>(BUFFER_SIZE + 1)

    override fun log(priority: Int, tag: String, message: String, t: Throwable?) {
        var message = message
        message = System.currentTimeMillis().toString() + " " + priorityToString(priority) + " " + message
        synchronized(buffer) {
            buffer.addLast(message)
            if (buffer.size > BUFFER_SIZE) {
                buffer.removeFirst()
            }
        }
        if (t != null && priority == Log.ERROR) {
            Bugsnag.notify(t)
        }
    }

    fun update(error: Error) {
        synchronized(buffer) {
            var i = 1
            for (message in buffer) {
                error.addToTab("Log", String.format(Locale.US, "%03d", i++), message)
            }
        }
    }

    companion object {
        private val BUFFER_SIZE = 200

        private fun priorityToString(priority: Int): String {
            when (priority) {
                Log.ERROR -> return "E"
                Log.WARN -> return "W"
                Log.INFO -> return "I"
                Log.DEBUG -> return "D"
                else -> return priority.toString()
            }
        }
    }
}
