package com.example.webradioapp.service

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object NewsManager {
    // RSS Feed URL for Sky TG24 - Le news di Sky Tg 24 (Spreaker)
    private const val SKY_TG24_RSS_URL = "https://www.spreaker.com/show/4311383/episodes/feed"
    private const val ZANZARA_RSS_URL = "https://backr24.ilsole24ore.com/podcast/lazanzara.xml"
    private const val ZOO_105_RSS_URL = "https://feeds.megaphone.fm/lozoodi105"

    fun fetchLatestNewsUrl(): String? = fetchLatestRssUrl(SKY_TG24_RSS_URL)
    
    fun fetchLatestZanzaraUrl(): String? = fetchLatestRssUrl(ZANZARA_RSS_URL)
    
    fun fetchLatestZoo105Url(): String? = fetchLatestRssUrl(ZOO_105_RSS_URL)

    /**
     * Fetches the latest MP3 URL from an RSS feed.
     * This method runs synchronously and should be called from a background thread.
     */
    private fun fetchLatestRssUrl(feedUrl: String): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(feedUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return parseNewsXml(connection.inputStream)
            } else {
                Log.e("NewsManager", "HTTP Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("NewsManager", "Failed to fetch feed: $feedUrl", e)
        } finally {
            connection?.disconnect()
        }
        return null
    }

    private fun parseNewsXml(inputStream: InputStream): String? {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "enclosure") {
                    val url = parser.getAttributeValue(null, "url")
                    val type = parser.getAttributeValue(null, "type")
                    if (url != null && (url.contains(".mp3", ignoreCase = true) || type == "audio/mpeg")) {
                        return url
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("NewsManager", "Failed to parse XML", e)
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        return null
    }
}
