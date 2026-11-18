// SPDX-License-Identifier: AGPL-3.0-or-later

package dev.spradlev.scholarlyfeed

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_sources")
internal data class FeedSource(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val feedTitle: String,
    val level: Int,
    val verifiedAtMillis: Long,
    val lastSuccessfulRefreshAtMillis: Long? = null,
    val etag: String? = null,
    val lastModified: String? = null,
)
