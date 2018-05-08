package ch.grigala.telecine

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Bitmap
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.MediaRecorder.OutputFormat.MPEG_4
import android.media.MediaRecorder.VideoEncoder.H264
import android.media.MediaRecorder.VideoSource.SURFACE
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.os.Environment
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.google.android.gms.analytics.HitBuilders
import com.jakewharton.telecine.DeleteRecordingBroadcastReceiver
import com.jakewharton.telecine.R
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Provider

internal class RecordingSession(private val context: Context, private val listener: Listener, private val resultCode: Int, private val data: Intent,
                                private val analytics: Analytics, private val showCountDown: Provider<Boolean>, private val videoSizePercentage: Provider<Int>) {

    private val mainThread = Handler(Looper.getMainLooper())

    private val outputRoot: File
    private val fileFormat = SimpleDateFormat("'Telecine_'yyyy-MM-dd-HH-mm-ss'.mp4'", Locale.US)

    private val notificationManager: NotificationManager
    private val windowManager: WindowManager
    private val projectionManager: MediaProjectionManager

    private var overlayView: OverlayView? = null
    private var recorder: MediaRecorder? = null
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var outputFile: String? = null
    private var running: Boolean = false
    private var recordingStartNanos: Long = 0

    private// Get the best camera profile available. We assume MediaRecorder supports the highest.
    val recordingInfo: RecordingInfo
        get() {
            val displayMetrics = DisplayMetrics()
            val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager
            wm!!.defaultDisplay.getRealMetrics(displayMetrics)
            val displayWidth = displayMetrics.widthPixels
            val displayHeight = displayMetrics.heightPixels
            val displayDensity = displayMetrics.densityDpi
            Timber.d("Display size: %s x %s @ %s", displayWidth, displayHeight, displayDensity)

            val configuration = context.resources.configuration
            val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE
            Timber.d("Display landscape: %s", isLandscape)
            val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
            val cameraWidth = if (camcorderProfile != null) camcorderProfile!!.videoFrameWidth else -1
            val cameraHeight = if (camcorderProfile != null) camcorderProfile!!.videoFrameHeight else -1
            val cameraFrameRate = if (camcorderProfile != null) camcorderProfile!!.videoFrameRate else 30
            Timber.d("Camera size: %s x %s framerate: %s", cameraWidth, cameraHeight, cameraFrameRate)

            val sizePercentage = videoSizePercentage.get()
            Timber.d("Size percentage: %s", sizePercentage)

            return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, isLandscape,
                    cameraWidth, cameraHeight, cameraFrameRate, sizePercentage)
        }

    internal interface Listener {
        /** Invoked before [.onStart] to prepare UI before recording.  */
        fun onPrepare()

        /** Invoked immediately prior to the start of recording.  */
        fun onStart()

        /** Invoked immediately after the end of recording.  */
        fun onStop()

        /** Invoked after all work for this session has completed.  */
        fun onEnd()
    }

    init {

        val picturesDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES)
        outputRoot = File(picturesDir, "Telecine")

        notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        projectionManager = context.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun showOverlay() {
        Timber.d("Adding overlay view to window.")

        val overlayListener = object : OverlayView.Listener {
            override fun onCancel() {
                cancelOverlay()
            }

            override fun onPrepare() {
                listener.onPrepare()
            }

            override fun onStart() {
                startRecording()
            }

            override fun onStop() {
                stopRecording()
            }

            override fun onResize() {
                windowManager.updateViewLayout(overlayView, overlayView!!.getLayoutParams())
            }
        }
        overlayView = OverlayView.create(context, overlayListener, showCountDown.get())
        windowManager.addView(overlayView, OverlayView.createLayoutParams(context))

        analytics.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_RECORDING)
                .setAction(Analytics.ACTION_OVERLAY_SHOW)
                .build())
    }

    private fun hideOverlay() {
        if (overlayView != null) {
            Timber.d("Removing overlay view from window.")
            windowManager.removeView(overlayView)
            overlayView = null

            analytics.send(HitBuilders.EventBuilder() //
                    .setCategory(Analytics.CATEGORY_RECORDING)
                    .setAction(Analytics.ACTION_OVERLAY_HIDE)
                    .build())
        }
    }

    private fun cancelOverlay() {
        hideOverlay()
        listener.onEnd()

        analytics.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_RECORDING)
                .setAction(Analytics.ACTION_OVERLAY_CANCEL)
                .build())
    }

    private fun startRecording() {
        Timber.d("Starting screen recording...")

        if (!outputRoot.exists() && !outputRoot.mkdirs()) {
            Timber.e("Unable to create output directory '%s'.", outputRoot.absolutePath)
            Toast.makeText(context, "Unable to create output directory.\nCannot record screen.",
                    LENGTH_SHORT).show()
            return
        }

        val recordingInfo = recordingInfo
        Timber.d("Recording: %s x %s @ %s", recordingInfo.width, recordingInfo.height,
                recordingInfo.density)

        recorder = MediaRecorder()
        recorder!!.setVideoSource(SURFACE)
        recorder!!.setOutputFormat(MPEG_4)
        recorder!!.setVideoFrameRate(recordingInfo.frameRate)
        recorder!!.setVideoEncoder(H264)
        recorder!!.setVideoSize(recordingInfo.width, recordingInfo.height)
        recorder!!.setVideoEncodingBitRate(8 * 1000 * 1000)

        val outputName = fileFormat.format(Date())
        outputFile = File(outputRoot, outputName).absolutePath
        Timber.i("Output file '%s'.", outputFile)
        recorder!!.setOutputFile(outputFile)

        try {
            recorder!!.prepare()
        } catch (e: IOException) {
            throw RuntimeException("Unable to prepare MediaRecorder.", e)
        }

        projection = projectionManager.getMediaProjection(resultCode, data)

        val surface = recorder!!.surface
        display = projection!!.createVirtualDisplay(DISPLAY_NAME, recordingInfo.width, recordingInfo.height,
                recordingInfo.density, VIRTUAL_DISPLAY_FLAG_PRESENTATION, surface, null, null)

        recorder!!.start()
        running = true
        recordingStartNanos = System.nanoTime()
        listener.onStart()

        Timber.d("Screen recording started.")

        analytics.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_RECORDING)
                .setAction(Analytics.ACTION_RECORDING_START)
                .build())
    }

    private fun stopRecording() {
        Timber.d("Stopping screen recording...")

        if (!running) {
            throw IllegalStateException("Not running.")
        }
        running = false

        hideOverlay()

        var propagate = false
        try {
            // Stop the projection in order to flush everything to the recorder.
            projection!!.stop()
            // Stop the recorder which writes the contents to the file.
            recorder!!.stop()

            propagate = true
        } finally {
            try {
                // Ensure the listener can tear down its resources regardless if stopping crashes.
                listener.onStop()
            } catch (e: RuntimeException) {
                if (propagate) {

                    throw e // Only allow listener exceptions to propagate if stopped successfully.
                }
            }

        }

        val recordingStopNanos = System.nanoTime()

        recorder!!.release()
        display!!.release()

        analytics.send(HitBuilders.EventBuilder() //
                .setCategory(Analytics.CATEGORY_RECORDING)
                .setAction(Analytics.ACTION_RECORDING_STOP)
                .build())
        analytics.send(HitBuilders.TimingBuilder() //
                .setCategory(Analytics.CATEGORY_RECORDING)
                .setValue(TimeUnit.NANOSECONDS.toMillis(recordingStopNanos - recordingStartNanos))
                .setVariable(Analytics.VARIABLE_RECORDING_LENGTH)
                .build())

        Timber.d("Screen recording stopped. Notifying media scanner of new video.")

        MediaScannerConnection.scanFile(context, arrayOf<String>(outputFile!!), null
        ) { path, uri ->
            if (uri == null) throw NullPointerException("uri == null")
            Timber.d("Media scanner completed.")
            mainThread.post { showNotification(uri, null) }
        }
    }

    private fun showNotification(uri: Uri?, bitmap: Bitmap?) {
        val viewIntent = Intent(ACTION_VIEW, uri)
        val pendingViewIntent = PendingIntent.getActivity(context, 0, viewIntent, FLAG_CANCEL_CURRENT)

        var shareIntent = Intent(ACTION_SEND)
        shareIntent.setType(MIME_TYPE)
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent = Intent.createChooser(shareIntent, null)
        val pendingShareIntent = PendingIntent.getActivity(context, 0, shareIntent, FLAG_CANCEL_CURRENT)

        val deleteIntent = Intent(context, DeleteRecordingBroadcastReceiver::class.java)
        deleteIntent.data = uri
        val pendingDeleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, FLAG_CANCEL_CURRENT)

        val title = context.getText(R.string.notification_captured_title)
        val subtitle = context.getText(R.string.notification_captured_subtitle)
        val share = context.getText(R.string.notification_captured_share)
        val delete = context.getText(R.string.notification_captured_delete)
        val builder = Notification.Builder(context) //
                .setContentTitle(title)
                .setContentText(subtitle)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setSmallIcon(R.drawable.ic_videocam_white_24dp)
                .setColor(context.resources.getColor(R.color.primary_normal))
                .setContentIntent(pendingViewIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_share_white_24dp, share, pendingShareIntent)
                .addAction(R.drawable.ic_delete_white_24dp, delete, pendingDeleteIntent)

        if (bitmap != null) {
            builder.setLargeIcon(createSquareBitmap(bitmap!!))
                    .setStyle(Notification.BigPictureStyle() //
                            .setBigContentTitle(title) //
                            .setSummaryText(subtitle) //
                            .bigPicture(bitmap))
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        if (bitmap != null) {
            listener.onEnd()
            return
        }

        object : AsyncTask<Void, Void, Bitmap>() {
            override fun doInBackground(vararg none: Void): Bitmap {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                return retriever.frameAtTime
            }

            override fun onPostExecute(bitmap: Bitmap?) {
                if (bitmap != null && !notificationDismissed()) {
                    showNotification(uri, bitmap)
                } else {
                    listener.onEnd()
                }
            }

            private fun notificationDismissed(): Boolean {
                return SDK_INT >= M && notificationManager.activeNotifications.size == 0
            }
        }.execute()
    }

    internal class RecordingInfo(val width: Int, val height: Int, val frameRate: Int, val density: Int)

    fun destroy() {
        if (running) {
            Timber.w("Destroyed while running!")
            stopRecording()
        }
    }

    companion object {
        val NOTIFICATION_ID = 522592

        private val DISPLAY_NAME = "telecine"
        private val MIME_TYPE = "video/mp4"

        fun calculateRecordingInfo(displayWidth: Int, displayHeight: Int,
                                   displayDensity: Int, isLandscapeDevice: Boolean, cameraWidth: Int, cameraHeight: Int,
                                   cameraFrameRate: Int, sizePercentage: Int): RecordingInfo {
            var displayWidth = displayWidth
            var displayHeight = displayHeight
            // Scale the display size before any maximum size calculations.
            displayWidth = displayWidth * sizePercentage / 100
            displayHeight = displayHeight * sizePercentage / 100

            if (cameraWidth == -1 && cameraHeight == -1) {
                // No cameras. Fall back to the display size.
                return RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity)
            }

            var frameWidth = if (isLandscapeDevice) cameraWidth else cameraHeight
            var frameHeight = if (isLandscapeDevice) cameraHeight else cameraWidth
            if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
                // Frame can hold the entire display. Use exact values.
                return RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity)
            }

            // Calculate new width or height to preserve aspect ratio.
            if (isLandscapeDevice) {
                frameWidth = displayWidth * frameHeight / displayHeight
            } else {
                frameHeight = displayHeight * frameWidth / displayWidth
            }
            return RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity)
        }

        private fun createSquareBitmap(bitmap: Bitmap): Bitmap {
            var x = 0
            var y = 0
            var width = bitmap.width
            var height = bitmap.height
            if (width > height) {
                x = (width - height) / 2

                width = height
            } else {
                y = (height - width) / 2

                height = width
            }
            return Bitmap.createBitmap(bitmap, x, y, width, height, null, true)
        }
    }
}
