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
     * Fetches news from a general Google News RSS feed URL, safely following redirects.
     */
    suspend fun fetchNewsFromUrl(urlStr: String): List<NewsArticle> {
        return fetchNewsFromUrlWithRedirect(urlStr, 0)
    }

    private suspend fun fetchNewsFromUrlWithRedirect(urlStr: String, redirectCount: Int): List<NewsArticle> = withContext(Dispatchers.IO) {
        if (redirectCount > 3) {
            Log.e("GoogleNewsFetcher", "Too many redirects for URL: $urlStr")
            return@withContext emptyList()
        }
        val articles = mutableListOf<NewsArticle>()
        var connection: HttpsURLConnection? = null

        try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = true // Try standard automatic redirects
            connection.requestMethod = "GET"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "application/xml,text/xml,*/*")

            val status = connection.responseCode
            if (status == HttpsURLConnection.HTTP_MOVED_TEMP || status == HttpsURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    Log.d("GoogleNewsFetcher", "Following manual redirect from $urlStr to $newUrl")
                    return@withContext fetchNewsFromUrlWithRedirect(newUrl, redirectCount + 1)
                }
            }

            if (status == HttpsURLConnection.HTTP_OK) {
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
                                    tagName.equals("title", ignoreCase = true) -> {
                                        try { currentTitle = parser.nextText() } catch (e: Exception) { currentTitle = parser.text ?: "" }
                                    }
                                    tagName.equals("link", ignoreCase = true) -> {
                                        try { currentLink = parser.nextText() } catch (e: Exception) { currentLink = parser.text ?: "" }
                                    }
                                    tagName.equals("pubDate", ignoreCase = true) -> {
                                        try { currentPubDate = parser.nextText() } catch (e: Exception) { currentPubDate = parser.text ?: "" }
                                    }
                                    tagName.equals("source", ignoreCase = true) -> {
                                        try { currentSource = parser.nextText() } catch (e: Exception) { currentSource = parser.text ?: "" }
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName.equals("item", ignoreCase = true)) {
                                val cleanTitle = if (currentTitle.contains(" - ")) {
                                    currentTitle.substringBeforeLast(" - ").trim()
                                } else {
                                    currentTitle
                                }
                                if (cleanTitle.isNotEmpty()) {
                                    val cleanSource = if (currentSource.isEmpty()) "中央社 CNA" else currentSource
                                    articles.add(NewsArticle(cleanTitle, currentLink, currentPubDate, cleanSource))
                                }
                                insideItem = false
                                currentTitle = ""
                                currentLink = ""
                                currentPubDate = ""
                                currentSource = ""
                            }
                        }
                    }
                    eventType = try {
                        parser.next()
                    } catch (e: Exception) {
                        Log.e("GoogleNewsFetcher", "XML parse loop exception caught, closing early", e)
                        XmlPullParser.END_DOCUMENT // Stop feed parsing gracefully instead of aborting the whole request
                    }
                }
                Log.d("GoogleNewsFetcher", "Successfully fetched ${articles.size} news from RSS: $urlStr")
            } else {
                Log.e("GoogleNewsFetcher", "Unsuccessful response code $status for URL: $urlStr")
            }
        } catch (e: Exception) {
            Log.e("GoogleNewsFetcher", "Failed to fetch RSS of $urlStr", e)
        } finally {
            connection?.disconnect()
        }
        return@withContext articles
    }

    /**
     * Fetches the latest Taiwan Google News index (with fallbacks to CNA / Yahoo News).
     */
    suspend fun fetchLatestTaiwanNews(): List<NewsArticle> {
        var list = fetchNewsFromUrl("https://news.google.com/rss?hl=zh-TW&gl=TW&ceid=TW:zh-Hant")
        if (list.isEmpty()) {
            Log.d("GoogleNewsFetcher", "Google News RSS empty/failed; trying CNA RSS as fallback...")
            list = fetchNewsFromUrl("https://www.cna.com.tw/rss/aall.aspx")
        }
        if (list.isEmpty()) {
            Log.d("GoogleNewsFetcher", "CNA RSS empty; trying Yahoo News as second-level fallback...")
            list = fetchNewsFromUrl("https://tw.news.yahoo.com/rss/realtime")
        }
        return list
    }

    /**
     * Fetches international (world) news in Traditional Chinese (with fallback to CNA International).
     */
    suspend fun fetchInternationalNews(): List<NewsArticle> {
        var list = fetchNewsFromUrl("https://news.google.com/rss/headlines/section/topic/WORLD?hl=zh-TW&gl=TW&ceid=TW:zh-Hant")
        if (list.isEmpty()) {
            Log.d("GoogleNewsFetcher", "Google World News RSS empty/failed; trying CNA International RSS...")
            list = fetchNewsFromUrl("https://www.cna.com.tw/rss/aint.aspx")
        }
        return list
    }

    /**
     * Fetches news based on a specific location (city/county name).
     */
    suspend fun fetchNewsByLocation(city: String): List<NewsArticle> {
        val cleanQuery = city.trim()
        val query = if (cleanQuery.isNotEmpty()) cleanQuery else "台灣"
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        var list = fetchNewsFromUrl("https://news.google.com/rss/search?q=$encodedQuery&hl=zh-TW&gl=TW&ceid=TW:zh-Hant")
        if (list.isEmpty()) {
            Log.d("GoogleNewsFetcher", "Google Local $city News empty; trying general CNA Focus News RSS...")
            list = fetchNewsFromUrl("https://www.cna.com.tw/rss/aspt.aspx") // CNA sports / topic focus
        }
        if (list.isEmpty()) {
            list = fetchLatestTaiwanNews()
        }
        return list
    }
}
