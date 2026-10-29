package dev.spradlev.scholarlyfeed

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

        val focusMode = composeRule.onNodeWithContentDescription("Focus Mode")
        focusMode
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .performClick()
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "On"))
            .performClick()
            .assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Off"))
    }

    @Test
    fun settingsButtonOpensSourceManagement() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Sources").assertExists()

        composeRule.onNodeWithText("Back to feed").performClick()
        composeRule.onNodeWithContentDescription("Settings").assertExists()
    }
}
