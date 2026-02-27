package io.lifephysics.architect2.data

import io.lifephysics.architect2.domain.TrendItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL

/**
 * Fetches and parses the Google Trends daily trending searches RSS feed.
 *
 * The feed URL format is:
 *   https://trends.google.com/trending/rss?geo=US   (country-specific )
 *   https://trends.google.com/trending/rss?geo=     (global — empty geo param )
 *
 * No authentication is required. The feed is updated approximately once per day.
 *
 * Parsing strategy: XmlPullParser is used to extract the ht:approx_traffic,
 * ht:picture, ht:news_item_title, and ht:news_item_source tags from each <item>.
 */
class TrendsRepository {

    companion object {
        private const val BASE_URL = "https://trends.google.com/trending/rss"
        private const val HT_NS = "https://trends.google.com/trending/rss"
    }

    /**
     * Returns a list of [TrendItem]s for the given country code.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "US", "GB", "BR" ).
     *                    Pass null or empty string for the global feed.
     */
    suspend fun getTrends(countryCode: String?): List<TrendItem> = withContext(Dispatchers.IO) {
        val geo = countryCode?.uppercase()?.trim() ?: ""
        val feedUrl = "$BASE_URL?geo=$geo"

        try {
            val connection = URL(feedUrl).openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "LifeArchitect/1.0")
            val stream = connection.getInputStream()

            parseRss(stream)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseRss(stream: java.io.InputStream): List<TrendItem> {
        val items = mutableListOf<TrendItem>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType

        // Per-item accumulators
        var title = ""
        var link = ""
        var traffic = ""
        var imageUrl: String? = null
        var newsTitle: String? = null
        var newsSource: String? = null
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name ?: ""
            val ns = parser.namespace ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName == "item" -> {
                            insideItem = true
                            // Reset accumulators for each new item
                            title = ""; link = ""; traffic = ""
                            imageUrl = null; newsTitle = null; newsSource = null
                        }
                        insideItem && tagName == "title" && ns.isEmpty() ->
                            title = parser.nextText()

                        insideItem && tagName == "link" && ns.isEmpty() ->
                            link = parser.nextText()

                        insideItem && tagName == "approx_traffic" ->
                            traffic = parser.nextText()

                        insideItem && tagName == "picture" ->
                            imageUrl = parser.nextText()

                        insideItem && tagName == "news_item_title" ->
                            newsTitle = parser.nextText()

                        insideItem && tagName == "news_item_source" ->
                            newsSource = parser.nextText()
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (tagName == "item" && insideItem) {
                        if (title.isNotBlank()) {
                            items.add(
                                TrendItem(
                                    title = title,
                                    traffic = traffic,
                                    link = link,
                                    imageUrl = imageUrl,
                                    newsTitle = newsTitle,
                                    newsSource = newsSource
                                )
                            )
                        }
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }

        stream.close()
        return items
    }
}
