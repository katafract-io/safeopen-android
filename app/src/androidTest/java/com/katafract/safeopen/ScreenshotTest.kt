package com.katafract.safeopen

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Compose UI screenshots for Play Console listing. v1 captures the Scanner
 * home state on each AVD form factor (phone / 7in / 10in tablet) — the
 * scan-prompt is the natural first impression of SafeOpen.
 *
 * Future iterations can navigate via the ViewModel into ResultScreen
 * and HistoryScreen for richer listings.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val storage = TestStorage()

    @After
    fun teardown() {
        if (::scenario.isInitialized) {
            try { scenario.close() } catch (_: Throwable) {}
        }
    }

    @Test
    fun a_scanner() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        device.waitForIdle(2_000)
        Thread.sleep(1_500)
        screenshot("01_scanner")
    }

    private fun screenshot(name: String) {
        val bitmap: Bitmap =
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                ?: error("UiAutomation.takeScreenshot() returned null")
        storage.openOutputFile("$name.png").use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
