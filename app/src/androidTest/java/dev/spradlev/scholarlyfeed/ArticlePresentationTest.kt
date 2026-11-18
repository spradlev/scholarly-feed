// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class ArticlePresentationTest {
    @Test
    fun publishedDateDoesNotIncludeTime() {
        val publishedDate = formatArticleDate(1793188800000L)

        assertNotNull(publishedDate)
        assertFalse(requireNotNull(publishedDate).contains(":"))
    }

    @Test
    fun articleTextRemovesMarkupAndCollapsesWhitespace() {
        assertEquals(
            "Research findings support clear summaries.",
            cleanArticleText("<p>Research <strong>findings</strong> support<br/> clear summaries.</p>"),
        )
    }
}
