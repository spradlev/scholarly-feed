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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
    fun sourceCanBeAddedOrganizedAndRemovedWithoutFetching() {
        composeRule.onNodeWithText("Manage sources (0)").performClick()
        composeRule.onNodeWithTag("sourceTitleInput").performTextInput("Research Digest")
        composeRule.onNodeWithTag("sourceUrlInput").performTextInput("https://example.org/feed.xml")
        composeRule.onNodeWithTag("sourceLevelSelector").performClick()
        composeRule.onNodeWithText("Level 2: Academic commentary and analysis").performClick()
        composeRule.onNodeWithText("Add source").performClick()

        composeRule.onNodeWithText("Research Digest").assertExists()
        composeRule.onNodeWithText("https://example.org/feed.xml").assertExists()
        composeRule.onNodeWithText("Level 2: Academic commentary and analysis").assertExists()
        composeRule.onNodeWithText("Not verified").assertExists()

        composeRule.onNodeWithText("Back to feed").performClick()
        composeRule.onNodeWithText("Manage sources (1)").performClick()
        composeRule.onNodeWithContentDescription("Remove Research Digest").performClick()

        composeRule.onNodeWithText("No sources added yet.").assertExists()
        composeRule.onNodeWithText("Back to feed").performClick()
        composeRule.onNodeWithText("Manage sources (0)").assertExists()
    }

    @Test
    fun sourceRequiresHttpsUrl() {
        composeRule.onNodeWithText("Manage sources (0)").performClick()
        composeRule.onNodeWithTag("sourceTitleInput").performTextInput("Research Digest")
        composeRule.onNodeWithTag("sourceUrlInput").performTextInput("http://example.org/feed.xml")
        composeRule.onNodeWithText("Add source").performClick()

        composeRule.onNodeWithText("Enter a valid HTTPS URL with a host.").assertExists()
        composeRule.onNodeWithText("No sources added yet.").assertExists()
    }
}
