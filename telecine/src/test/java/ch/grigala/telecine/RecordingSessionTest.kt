package ch.grigala.telecine

import ch.grigala.telecine.RecordingSession.Companion.calculateRecordingInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecordingSessionTest {
    @Test
    fun videoSizeNoCamera() {
        val size = calculateRecordingInfo(1080, 1920, 160, false, -1, -1, 30, 100)
        assertThat(size.width).isEqualTo(1080)
        assertThat(size.height).isEqualTo(1920)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeResize() {
        val size = calculateRecordingInfo(1080, 1920, 160, false, -1, -1, 30, 75)
        assertThat(size.width).isEqualTo(810)
        assertThat(size.height).isEqualTo(1440)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeFitsInCamera() {
        val size = calculateRecordingInfo(1080, 1920, 160, false, 1920, 1080, 30, 100)
        assertThat(size.width).isEqualTo(1080)
        assertThat(size.height).isEqualTo(1920)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeFitsInCameraLandscape() {
        val size = calculateRecordingInfo(1920, 1080, 160, true, 1920, 1080, 30, 100)
        assertThat(size.width).isEqualTo(1920)
        assertThat(size.height).isEqualTo(1080)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeLargerThanCamera() {
        val size = calculateRecordingInfo(2160, 3840, 160, false, 1920, 1080, 30, 100)
        assertThat(size.width).isEqualTo(1080)
        assertThat(size.height).isEqualTo(1920)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeLargerThanCameraLandscape() {
        val size = calculateRecordingInfo(3840, 2160, 160, true, 1920, 1080, 30, 100)
        assertThat(size.width).isEqualTo(1920)
        assertThat(size.height).isEqualTo(1080)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeLargerThanCameraScaling() {
        val size = calculateRecordingInfo(1200, 1920, 160, false, 1920, 1080, 30, 100)
        assertThat(size.width).isEqualTo(1080)
        assertThat(size.height).isEqualTo(1728)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeLargerThanCameraScalingResizesFirst() {
        val size = calculateRecordingInfo(1200, 1920, 160, false, 1920, 1080, 30, 75)
        assertThat(size.width).isEqualTo(900)
        assertThat(size.height).isEqualTo(1440)
        assertThat(size.density).isEqualTo(160)
    }

    @Test
    fun videoSizeLargerThanCameraScalingLandscape() {
        val size = calculateRecordingInfo(1920, 1200, 160, true, 1920, 1080, 30, 100)
        assertThat(size.width).isEqualTo(1728)
        assertThat(size.height).isEqualTo(1080)
        assertThat(size.density).isEqualTo(160)
    }
}
