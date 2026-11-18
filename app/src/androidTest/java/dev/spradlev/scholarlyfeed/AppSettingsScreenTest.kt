// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: FeedDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppPreferences.save(context, AppPreferences())
        database = Room.inMemoryDatabaseBuilder(context, FeedDatabase::class.java).build()
        composeRule.setContent {
            ScholarlyFeedScreen(
                database = database,
                fetcher = object : FeedFetcher {
                    override suspend fun fetch(url: String): ParsedFeed =
                        ParsedFeed("", url, emptyList())
                },
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
        AppPreferences.save(context, AppPreferences())
    }

    @Test
    fun settingsPersistTextSizeAndColorSchemeAndReturnToSources() {
        composeRule.onNodeWithTag("bottomSettingsButton").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()

        composeRule.onNodeWithTag("fontSizeOption_LARGER").performClick()
        composeRule.onNodeWithTag("readingScaleSlider")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(1.5f) }
        composeRule.onNodeWithTag("colorSchemeOption_DARK").performScrollTo().performClick()
        assertEquals(AppTextSize.LARGER, AppPreferences.load(context).textSize)
        assertEquals(1.5f, AppPreferences.load(context).readingScale)
        assertEquals(AppColorScheme.DARK, AppPreferences.load(context).colorScheme)

        composeRule.onNodeWithTag("manageSourcesButton").performScrollTo().performClick()
        composeRule.onNodeWithText("Sources").assertExists()
        composeRule.onNodeWithText("Back to settings").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()

        composeRule.onNodeWithTag("googleDriveSettingsButton").performScrollTo().performClick()
        composeRule.onNodeWithText("Connect Google Drive and sync").assertExists()
        composeRule.onNodeWithText("Back to settings").performClick()
        composeRule.onNodeWithText("Accessibility & display").assertExists()
    }
}
