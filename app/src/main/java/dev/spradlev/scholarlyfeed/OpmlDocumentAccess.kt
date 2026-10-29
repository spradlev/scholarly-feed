package dev.spradlev.scholarlyfeed

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

internal fun readOpmlDocument(context: Context, uri: Uri): List<OpmlSource> {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Could not open the selected OPML file.")
    val xml = input.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_OPML_BYTES) {
                throw OpmlFormatException("The OPML file is larger than the 2 MB limit.")
            }
        }
        output.toByteArray().toString(StandardCharsets.UTF_8)
    }
    return parseOpml(xml)
}

internal fun writeOpmlDocument(context: Context, uri: Uri, xml: String) {
    val output = context.contentResolver.openOutputStream(uri)
        ?: throw IOException("Could not open the selected file for writing.")
    output.use { stream -> stream.write(xml.toByteArray(StandardCharsets.UTF_8)) }
}

private const val MAX_OPML_BYTES = 2 * 1024 * 1024
