package ch.grigala.telecine

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat.TRANSLUCENT
import android.os.Build
import android.text.TextUtils.getLayoutDirectionFromLocale
import android.view.Gravity
import android.view.View
import android.view.ViewAnimationUtils.createCircularReveal
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.BindDimen
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.jakewharton.telecine.R
import java.util.*

@SuppressLint("ViewConstructor") // Lint, in this case, I am smarter than you.
internal class OverlayView private constructor(context: Context, private val listener: Listener, private val showCountDown: Boolean) : FrameLayout(context) {

    @BindView(R.id.record_overlay_buttons) var buttonsView: View? = null
    @BindView(R.id.record_overlay_cancel) var cancelView: View? = null
    @BindView(R.id.record_overlay_start) var startView: View? = null
    @BindView(R.id.record_overlay_stop) var stopView: View? = null
    @BindView(R.id.record_overlay_recording) var recordingView: TextView? = null

    @BindDimen(R.dimen.overlay_width)
    var animationWidth: Int = 0

    internal interface Listener {
        /** Called when cancel is clicked. This view is unusable once this callback is invoked.  */
        fun onCancel()

        /**
         * Called when start is clicked and the view is animating itself out,
         * before [.onStart].
         */
        fun onPrepare()

        /**
         * Called when start is clicked and it is appropriate to start recording. This view will hide
         * itself completely before invoking this callback.
         */
        fun onStart()

        /** Called when stop is clicked. This view is unusable once this callback is invoked.  */
        fun onStop()

        /** Called when the size or layout params of this view have changed and require a relayout.  */
        fun onResize()
    }

    init {

        View.inflate(context, R.layout.overlay_view, this)
        ButterKnife.bind(this)

        if (getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL) {
            animationWidth = -animationWidth // Account for animating in from the other side of screen.
        }

        CheatSheet.setup(cancelView!!)
        CheatSheet.setup(startView!!)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val lp = layoutParams
        lp.height = insets.systemWindowInsetTop

        val canReceiveTouchEventsUnderStatusBar = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        if (!canReceiveTouchEventsUnderStatusBar) {
            val statusBarHeight = insets.systemWindowInsetTop
            lp.height += statusBarHeight
            setPaddingRelative(paddingStart, paddingTop + statusBarHeight, paddingEnd, paddingBottom)
        }

        listener.onResize()

        return insets.consumeSystemWindowInsets()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        translationX = animationWidth.toFloat()
        animate().translationX(0f)
                .setDuration(DURATION_ENTER_EXIT.toLong()).interpolator = DecelerateInterpolator()
    }

    @OnClick(R.id.record_overlay_cancel)
    fun onCancelClicked() {
        animate().translationX(animationWidth.toFloat())
                .setDuration(DURATION_ENTER_EXIT.toLong())
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { listener.onCancel() }
    }

    @OnClick(R.id.record_overlay_start)
    fun onStartClicked() {
        recordingView!!.visibility = View.VISIBLE
        val centerX = (startView!!.x + startView!!.width / 2).toInt()
        val centerY = (startView!!.y + startView!!.height / 2).toInt()
        val reveal = createCircularReveal(recordingView, centerX, centerY, 0f, width / 2f)
        reveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                buttonsView!!.visibility = View.GONE
            }
        })
        reveal.start()

        postDelayed({
            if (showCountDown) {
                showCountDown()
            } else {
                countdownComplete()
            }
        }, (if (showCountDown) COUNTDOWN_DELAY else NON_COUNTDOWN_DELAY).toLong())
    }

    private fun startRecording() {
        recordingView!!.visibility = View.INVISIBLE
        stopView!!.visibility = View.VISIBLE
        stopView!!.setOnClickListener { listener.onStop() }
        listener.onStart()
    }

    private fun showCountDown() {
        val countdown = resources.getStringArray(R.array.countdown)
        countdown(countdown, 0) // array resource must not be empty
    }

    private fun countdownComplete() {
        listener.onPrepare()
        recordingView!!.animate()
                .alpha(0f)
                .setDuration(COUNTDOWN_DELAY.toLong())
                .withEndAction { startRecording() }
    }

    private fun countdown(countdownArr: Array<String>, index: Int) {
        postDelayed({
            recordingView!!.text = countdownArr[index]
            if (index < countdownArr.size - 1) {
                countdown(countdownArr, index + 1)
            } else {
                countdownComplete()
            }
        }, COUNTDOWN_DELAY.toLong())
    }

    companion object {
        private val COUNTDOWN_DELAY = 1000
        private val NON_COUNTDOWN_DELAY = 500
        private val DURATION_ENTER_EXIT = 300

        fun create(context: Context, listener: Listener, showCountDown: Boolean): OverlayView {
            return OverlayView(context, listener, showCountDown)
        }

        fun createLayoutParams(context: Context): WindowManager.LayoutParams {
            val width = context.resources.getDimensionPixelSize(R.dimen.overlay_width)

            val params = WindowManager.LayoutParams(width, WRAP_CONTENT, TYPE_SYSTEM_ERROR, FLAG_NOT_FOCUSABLE
                    or FLAG_NOT_TOUCH_MODAL
                    or FLAG_LAYOUT_NO_LIMITS
                    or FLAG_LAYOUT_INSET_DECOR
                    or FLAG_LAYOUT_IN_SCREEN, TRANSLUCENT)
            params.gravity = Gravity.TOP or gravityEndLocaleHack()

            return params
        }

        @SuppressLint("RtlHardcoded") // Gravity.END is not honored by WindowManager for added views.
        private fun gravityEndLocaleHack(): Int {
            val direction = getLayoutDirectionFromLocale(Locale.getDefault())
            return if (direction == View.LAYOUT_DIRECTION_RTL) Gravity.LEFT else Gravity.RIGHT
        }
    }
}
