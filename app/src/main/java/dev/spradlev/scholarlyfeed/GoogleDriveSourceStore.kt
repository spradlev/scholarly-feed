package dev.spradlev.scholarlyfeed

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class DriveSourceFile(
    val id: String,
    val sources: List<OpmlSource>,
)

internal data class SourceSyncConflict(
    val local: OpmlSource,
    val drive: OpmlSource,
)

internal data class SourceSyncPlan(
    val driveOnly: List<OpmlSource>,
    val conflicts: List<SourceSyncConflict>,
)

internal fun planSourceSync(
    localSources: List<FeedSource>,
    driveSources: List<OpmlSource>,
): SourceSyncPlan {
    val localByUrl = localSources.associateBy(FeedSource::feedUrl)
    val driveByUrl = driveSources.associateBy(OpmlSource::feedUrl)
    val driveOnly = driveSources.filterNot { it.feedUrl in localByUrl }
    val conflicts = localSources.mapNotNull { local ->
        val drive = driveByUrl[local.feedUrl] ?: return@mapNotNull null
        if (local.title == drive.title && local.level == drive.level) {
            null
        } else {
            SourceSyncConflict(
                local = OpmlSource(local.title, local.feedUrl, local.level),
                drive = drive,
            )
        }
    }
    return SourceSyncPlan(driveOnly, conflicts)
}

internal fun mergeSourceLists(
    localSources: List<FeedSource>,
    driveSources: List<OpmlSource>,
    useDriveConflictValues: Boolean,
): List<OpmlSource> {
    val driveByUrl = driveSources.associateBy(OpmlSource::feedUrl)
    val merged = localSources.map { local ->
        val drive = driveByUrl[local.feedUrl]
        if (useDriveConflictValues && drive != null) {
            drive
        } else {
            OpmlSource(local.title, local.feedUrl, local.level)
        }
    }
    val localUrls = localSources.mapTo(mutableSetOf(), FeedSource::feedUrl)
    return merged + driveSources.filterNot { it.feedUrl in localUrls }
}

internal class GoogleDriveSourceStore(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build(),
) {
    suspend fun readSourceFile(accessToken: String): DriveSourceFile? = withContext(Dispatchers.IO) {
        val listUrl = Uri.parse("$DRIVE_API/files")
            .buildUpon()
            .appendQueryParameter("spaces", "appDataFolder")
            .appendQueryParameter("q", "name = '$SOURCE_FILE_NAME' and trashed = false")
            .appendQueryParameter("fields", "files(id,name,modifiedTime)")
            .appendQueryParameter("pageSize", "100")
            .build()
            .toString()
        val listing = parseDriveJsonObject(
            request("GET", listUrl, accessToken),
            "Google Drive returned an invalid source-file listing.",
        )
        val files = listing.optJSONArray("files")
            ?: throw GoogleDriveSyncException("Google Drive returned a listing without a files array.")
        if (files.length() == 0) return@withContext null

        val file = (0 until files.length())
            .mapNotNull { index -> files.optJSONObject(index) }
            .filter { it.optString("name") == SOURCE_FILE_NAME }
            .maxByOrNull { it.optString("modifiedTime") }
            ?: return@withContext null
        val fileId = file.optString("id").takeIf(String::isNotBlank)
            ?: throw GoogleDriveSyncException("Google Drive returned a source file without an ID.")
        val contentUrl = Uri.parse("$DRIVE_API/files/${Uri.encode(fileId)}")
            .buildUpon()
            .appendQueryParameter("alt", "media")
            .build()
            .toString()
        val sources = parseOpml(request("GET", contentUrl, accessToken))
        DriveSourceFile(fileId, sources)
    }

    suspend fun writeSourceFile(
        accessToken: String,
        fileId: String?,
        sources: List<OpmlSource>,
    ): String = withContext(Dispatchers.IO) {
        val opml = createOpml(
            sources.map { source ->
                FeedSource(
                    feedUrl = source.feedUrl,
                    title = source.title,
                    feedTitle = "",
                    level = source.level,
                    verifiedAtMillis = 0L,
                )
            },
        )
        if (opml.toByteArray(Charsets.UTF_8).size > MAX_SYNC_FILE_BYTES) {
            throw GoogleDriveSyncException("The source list is larger than the 2 MB Google Drive sync limit.")
        }
        if (fileId == null) {
            createSourceFile(accessToken, opml)
        } else {
            val updateUrl = Uri.parse("$DRIVE_UPLOAD_API/files/${Uri.encode(fileId)}")
                .buildUpon()
                .appendQueryParameter("uploadType", "media")
                .build()
                .toString()
            request(
                method = "PATCH",
                url = updateUrl,
                accessToken = accessToken,
                body = opml.toByteArray(Charsets.UTF_8),
                contentType = OPML_CONTENT_TYPE,
            )
            fileId
        }
    }

    private fun createSourceFile(accessToken: String, opml: String): String {
        val boundary = "ScholarlyFeed-${System.nanoTime()}"
        val metadata = JSONObject()
            .put("name", SOURCE_FILE_NAME)
            .put("mimeType", OPML_CONTENT_TYPE)
            .put("parents", JSONArray().put("appDataFolder"))
        val body = multipartBody(boundary, metadata.toString(), opml)
        val url = Uri.parse("$DRIVE_UPLOAD_API/files")
            .buildUpon()
            .appendQueryParameter("uploadType", "multipart")
            .appendQueryParameter("fields", "id,name")
            .build()
            .toString()
        val result = parseDriveJsonObject(
            request(
                method = "POST",
                url = url,
                accessToken = accessToken,
                body = body,
                contentType = "multipart/related; boundary=$boundary",
            ),
            "Google Drive returned an invalid response after saving the source list.",
        )
        return result.optString("id").takeIf(String::isNotBlank)
            ?: throw GoogleDriveSyncException("Google Drive saved the source list without returning a file ID.")
    }

    private fun request(
        method: String,
        url: String,
        accessToken: String,
        body: ByteArray? = null,
        contentType: String? = null,
    ): String {
        val requestBody = body?.toRequestBody((contentType ?: "application/octet-stream").toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json, application/xml, text/xml")
            .method(method, requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body
                val responseText = responseBody.byteStream().use(::readBoundedResponse)
                if (!response.isSuccessful) {
                    if (response.code == 401 || response.code == 403) {
                        throw GoogleDriveSyncException(
                            "Google Drive access was denied or expired. Reconnect and try again.",
                        )
                    }
                    val apiMessage = try {
                        JSONObject(responseText).optJSONObject("error")?.optString("message")
                    } catch (_: org.json.JSONException) {
                        null
                    }?.takeIf(String::isNotBlank)
                    throw GoogleDriveSyncException(
                        apiMessage ?: "Google Drive request failed with HTTP ${response.code}.",
                    )
                }
                return responseText
            }
        } catch (exception: GoogleDriveSyncException) {
            throw exception
        } catch (exception: IOException) {
            throw GoogleDriveSyncException("Could not reach Google Drive. Check your connection and try again.", exception)
        }
    }

    private fun readBoundedResponse(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_SYNC_FILE_BYTES) {
                throw GoogleDriveSyncException("The Google Drive source list is larger than the 2 MB limit.")
            }
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }

    private fun multipartBody(boundary: String, metadata: String, opml: String): ByteArray =
        buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: $OPML_CONTENT_TYPE; charset=UTF-8\r\n\r\n")
            append(opml)
            append("\r\n--$boundary--\r\n")
        }.toByteArray(Charsets.UTF_8)

    private fun parseDriveJsonObject(json: String, errorMessage: String): JSONObject =
        try {
            JSONObject(json)
        } catch (exception: org.json.JSONException) {
            throw GoogleDriveSyncException(errorMessage, exception)
        }

    private companion object {
        const val SOURCE_FILE_NAME = "scholarly-feed-sources.opml"
        const val OPML_CONTENT_TYPE = "text/x-opml"
        const val MAX_SYNC_FILE_BYTES = 2 * 1024 * 1024
        const val DRIVE_API = "https://www.googleapis.com/drive/v3"
        const val DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
    }
}

internal class GoogleDriveSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)
