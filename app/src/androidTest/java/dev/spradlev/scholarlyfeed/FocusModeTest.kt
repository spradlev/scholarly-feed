// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusModeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun focusModeSwitchCanBeTurnedOnAndOff() {
        composeRule.onNodeWithText("Your feed is empty. Add sources to set up your reading list.")
            .assertExists()

        val focusMode = composeRule.onNodeWithTag("bottomFocusModeButton")
        focusMode
            .assert(SemanticsMatcher.expectValue(ContentDescription, listOf("Focus mode")))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
            .performMouseInput { moveTo(center) }
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("Focus mode")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        focusMode
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On"))
            .performClick()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
    }

    @Test
    fun searchButtonExposesItsActionAndShowsTooltipOnTouchHold() {
        val search = composeRule.onNodeWithTag("feedSearchButton")
        search
            .assert(SemanticsMatcher.expectValue(ContentDescription, listOf("Search feed")))
            .performTouchInput { longClick() }

        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("Search feed")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag("refreshFeedsButton")
            .assert(SemanticsMatcher.expectValue(ContentDescription, listOf("Refresh feeds")))
    }

    @Test
    fun settingsButtonOpensSettingsAndSourceManagement() {
        val settings = composeRule.onNodeWithTag("bottomSettingsButton")
        settings.assert(SemanticsMatcher.expectValue(ContentDescription, listOf("Settings")))
            .performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()
        composeRule.onNodeWithTag("manageSourcesButton").performScrollTo().performClick()
        composeRule.onNodeWithText("Sources").assertExists()

        composeRule.onNodeWithText("Back to settings").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()
    }

    @Test
    fun homeButtonReturnsFromSettingsToFeed() {
        composeRule.onNodeWithTag("bottomSettingsButton").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()

        composeRule.onNodeWithTag("bottomHomeButton")
            .assert(SemanticsMatcher.expectValue(ContentDescription, listOf("Home")))
            .performClick()

        composeRule.onNodeWithText("Scholarly Feed").assertExists()
    }
}
