package dev.spradlev.scholarlyfeed

import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader
import java.io.StringWriter

internal data class OpmlSource(
    val title: String,
    val feedUrl: String,
    val level: Int,
)

internal class OpmlFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal fun parseOpml(xml: String): List<OpmlSource> {
    if (xml.toByteArray(Charsets.UTF_8).size > MAX_OPML_BYTES) {
        throw OpmlFormatException("The OPML file is larger than the 2 MB limit.")
    }

    val parser = Xml.newPullParser()
    try {
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        parser.setInput(StringReader(xml))

        var rootSeen = false
        var bodySeen = false
        var bodyDepth = 0
        val sources = mutableListOf<OpmlSource>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (!rootSeen) {
                        if (parser.name != "opml") {
                            throw OpmlFormatException("The document is not an OPML file.")
                        }
                        rootSeen = true
                    }
                    if (parser.name == "body") {
                        bodySeen = true
                        bodyDepth = parser.depth
                    }
                    if (parser.name == "outline" && bodyDepth > 0) {
                        val feedUrl = parser.getAttributeValue(null, "xmlUrl")
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                        if (feedUrl != null) {
                            val title = parser.getAttributeValue(null, "text")
                                ?.trim()
                                ?.takeIf(String::isNotEmpty)
                                ?: parser.getAttributeValue(null, "title")
                                    ?.trim()
                                    ?.takeIf(String::isNotEmpty)
                                ?: throw OpmlFormatException("A feed outline is missing its title.")
                            if (!isHttpsFeedUrl(feedUrl)) {
                                throw OpmlFormatException("OPML feed URLs must be valid HTTPS URLs.")
                            }
                            val levelAttribute = parser.getAttributeValue(null, "scholarlyFeedLevel")
                            val level = if (levelAttribute == null) {
                                1
                            } else {
                                levelAttribute.toIntOrNull()
                                    ?: throw OpmlFormatException("Source levels in OPML must be between 1 and 4.")
                            }
                            if (level !in 1..4) {
                                throw OpmlFormatException("Source levels in OPML must be between 1 and 4.")
                            }
                            sources += OpmlSource(title, feedUrl, level)
                            if (sources.size > MAX_OPML_SOURCES) {
                                throw OpmlFormatException("The OPML file contains more than 500 feed sources.")
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "body" && parser.depth == bodyDepth) bodyDepth = 0
                }
            }
            eventType = parser.next()
        }
        if (!rootSeen || !bodySeen) {
            throw OpmlFormatException("The OPML document must contain a body.")
        }
        if (sources.isEmpty()) {
            throw OpmlFormatException("The OPML document does not contain any feed sources.")
        }
        return sources
    } catch (exception: OpmlFormatException) {
        throw exception
    } catch (exception: XmlPullParserException) {
        throw OpmlFormatException("The OPML document is not valid XML.", exception)
    } catch (exception: Exception) {
        throw OpmlFormatException("The OPML document could not be read.", exception)
    }
}

internal fun createOpml(sources: List<FeedSource>): String {
    val writer = StringWriter()
    try {
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "opml")
        serializer.attribute(null, "version", "2.0")
        serializer.startTag(null, "head")
        serializer.startTag(null, "title")
        serializer.text("Scholarly Feed Sources")
        serializer.endTag(null, "title")
        serializer.endTag(null, "head")
        serializer.startTag(null, "body")
        sources.forEach { source ->
            serializer.startTag(null, "outline")
            serializer.attribute(null, "text", source.title)
            serializer.attribute(null, "title", source.title)
            serializer.attribute(null, "type", "rss")
            serializer.attribute(null, "xmlUrl", source.feedUrl)
            serializer.attribute(null, "scholarlyFeedLevel", source.level.toString())
            serializer.endTag(null, "outline")
        }
        serializer.endTag(null, "body")
        serializer.endTag(null, "opml")
        serializer.endDocument()
        serializer.flush()
    } catch (exception: Exception) {
        throw OpmlFormatException("Could not create the OPML document.", exception)
    }
    return writer.toString()
}

private fun isHttpsFeedUrl(value: String): Boolean {
    val uri = Uri.parse(value)
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
}

private const val MAX_OPML_BYTES = 2 * 1024 * 1024
private const val MAX_OPML_SOURCES = 500
