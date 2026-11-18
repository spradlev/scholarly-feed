// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import android.text.Html
import java.text.DateFormat
import java.util.Date

internal fun formatArticleDate(publishedAtMillis: Long?): String? =
    publishedAtMillis?.let { timestamp ->
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }

internal fun cleanArticleText(value: String): String =
    Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT)
        .toString()
        .replace(Regex("\\s+"), " ")
        .trim()
