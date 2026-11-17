package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stillness", appName)
    }

    @Test
    fun navigateAcrossAllTabsAndStartBreathing() {
        // Tasks tab
        composeRule.onNodeWithText("Take a quiet moment").assertIsDisplayed()

        // Navigate to Breathe tab
        composeRule.onNodeWithTag("nav_tab_breathe").performClick()
        composeRule.onNodeWithText("Mindful Breathing").assertIsDisplayed()
        composeRule.onNodeWithTag("breathing_circle_canvas").assertIsDisplayed()

        // Start session and verify active phase
        composeRule.onNodeWithTag("breathe_play_pause_button").performClick()
        composeRule.onNodeWithText("Breathe In").assertIsDisplayed()

        // Pause session cleanly
        composeRule.onNodeWithTag("breathe_play_pause_button").performClick()

        // Navigate to Unwind tab
        composeRule.onNodeWithTag("nav_tab_unwind").performClick()
        composeRule.onNodeWithText("Tactile Unwind").assertIsDisplayed()
        composeRule.onNodeWithTag("still_pond_surface").assertIsDisplayed()
    }
}
