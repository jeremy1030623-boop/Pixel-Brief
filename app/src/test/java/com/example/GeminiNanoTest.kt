package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.ui.GeminiNanoStatusCard

@RunWith(RobolectricTestRunner::class)
class GeminiNanoTest {
    @get:Rule val composeTestRule = createComposeRule()
    @Test fun test() {
        try {
            composeTestRule.setContent {
                GeminiNanoStatusCard()
            }
        } catch (e: Throwable) {
            java.io.File("out.txt").writeText(android.util.Log.getStackTraceString(e))
        }
    }
}
