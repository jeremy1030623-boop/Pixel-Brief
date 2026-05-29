package com.example.api

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object GoogleNewsFetcher {

    data class NewsArticle(
        val title: String,
        val link: String,
        val pubDate: String,
        val source: String
    )

    /**
     * Fetches news from a general Google News RSS feed URL.
     */
    suspend fun fetchNewsFromUrl(urlStr: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        var connection: HttpsURLConnection? = null

        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, "UTF-8")

                var eventType = parser.eventType
                var currentTitle = ""
                var currentLink = ""
                var currentPubDate = ""
                var currentSource = ""
                var insideItem = false

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName.equals("item", ignoreCase = true)) {
                                insideItem = true
                            } else if (insideItem) {
                                when {
                                    tagName.equals("title", ignoreCase = true) -> currentTitle = parser.nextText()
                                    tagName.equals("link", ignoreCase = true) -> currentLink = parser.nextText()
                                    tagName.equals("pubDate", ignoreCase = true) -> currentPubDate = parser.nextText()
                                    tagName.equals("source", ignoreCase = true) -> currentSource = parser.nextText()
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("item", ignoreCase = true)) {
                                // Clean up title if it contains the source suffix
                                val cleanTitle = if (currentTitle.contains(" - ")) {
                                    currentTitle.substringBeforeLast(" - ").trim()
                                } else {
                                    currentTitle
                                }

                                articles.add(NewsArticle(cleanTitle, currentLink, currentPubDate, currentSource))
                                insideItem = false
                                // Reset for next item
                                currentTitle = ""
                                currentLink = ""
                                currentPubDate = ""
                                currentSource = ""
                            }
                        }
                    }
                    eventType = parser.next()
                }
                Log.d("GoogleNewsFetcher", "Successfully fetched ${articles.size} news from RSS: $urlStr")
            } else {
                Log.e("GoogleNewsFetcher", "Unsuccessful response code: ${connection.responseCode} for URL: $urlStr")
            }
        } catch (e: Exception) {
            Log.e("GoogleNewsFetcher", "Failed to fetch Google RSS News from $urlStr", e)
        } finally {
            connection?.disconnect()
        }
        return@withContext articles
    }

    /**
     * Fetches the latest Taiwan Google News.
     */
    suspend fun fetchLatestTaiwanNews(): List<NewsArticle> {
        return fetchNewsFromUrl("https://news.google.com/rss?hl=zh-TW&gl=TW&ceid=TW:zh-Hant")
    }

    /**
     * Fetches international (world) news in Traditional Chinese.
     */
    suspend fun fetchInternationalNews(): List<NewsArticle> {
        return fetchNewsFromUrl("https://news.google.com/rss/headlines/section/topic/WORLD?hl=zh-TW&gl=TW&ceid=TW:zh-Hant")
    }

    /**
     * Fetches news based on a specific location (city/county name).
     */
    suspend fun fetchNewsByLocation(city: String): List<NewsArticle> {
        val cleanQuery = city.trim()
        val query = if (cleanQuery.isNotEmpty()) cleanQuery else "台灣"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return fetchNewsFromUrl("https://news.google.com/rss/search?q=$encodedQuery&hl=zh-TW&gl=TW&ceid=TW:zh-Hant")
    }
}
