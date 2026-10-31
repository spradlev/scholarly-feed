package dev.spradlev.scholarlyfeed

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
            .performClick()
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On"))
            .performClick()
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
    }

    @Test
    fun settingsButtonOpensSettingsAndSourceManagement() {
        composeRule.onNodeWithTag("bottomSettingsButton").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()
        composeRule.onNodeWithTag("manageSourcesButton").performClick()
        composeRule.onNodeWithText("Sources").assertExists()

        composeRule.onNodeWithText("Back to settings").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()
    }
}
