package com.example.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object GoogleNewsFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class NewsArticle(
        val title: String,
        val link: String,
        val pubDate: String
    )

    /**
     * Fetches news from a general Google News RSS feed URL.
     */
    suspend fun fetchNewsFromUrl(url: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("GoogleNewsFetcher", "Unsuccessful response code: ${response.code} for URL: $url")
                return@withContext emptyList()
            }
            val xmlText = response.body?.string() ?: ""
            val parsed = parseRssXml(xmlText)
            Log.d("GoogleNewsFetcher", "Successfully fetched ${parsed.size} news from RSS: $url")
            return@withContext parsed
        } catch (e: Throwable) {
            Log.e("GoogleNewsFetcher", "Failed to fetch Google RSS News from $url", e)
            return@withContext emptyList()
        }
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

    private fun parseRssXml(xml: String): List<NewsArticle> {
        val list = mutableListOf<NewsArticle>()
        val itemPattern = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL)
        val titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL)
        val linkPattern = Pattern.compile("<link>(.*?)</link>", Pattern.DOTALL)
        val pubDatePattern = Pattern.compile("<pubDate>(.*?)</pubDate>", Pattern.DOTALL)

        val itemMatcher = itemPattern.matcher(xml)
        var count = 0
        while (itemMatcher.find() && count < 10) {
            val itemContent = itemMatcher.group(1) ?: continue
            val titleMatcher = titlePattern.matcher(itemContent)
            val linkMatcher = linkPattern.matcher(itemContent)
            val pubDateMatcher = pubDatePattern.matcher(itemContent)

            val title = if (titleMatcher.find()) {
                titleMatcher.group(1)?.let { cleanCData(it) } ?: ""
            } else ""

            val link = if (linkMatcher.find()) {
                linkMatcher.group(1)?.let { cleanCData(it) } ?: ""
            } else ""

            val pubDate = if (pubDateMatcher.find()) {
                pubDateMatcher.group(1)?.let { cleanCData(it) } ?: ""
            } else ""

            // Refine clean title by removing " - source" suffix which Google News automatically appends.
            val cleanTitle = if (title.contains(" - ")) {
                title.substringBeforeLast(" - ").trim()
            } else {
                title
            }

            if (cleanTitle.isNotEmpty()) {
                list.add(NewsArticle(cleanTitle, link, pubDate))
                count++
            }
        }
        return list
    }

    private fun cleanCData(text: String): String {
        return text.replace("<![CDATA[", "").replace("]]>", "").replace("&amp;", "&").trim()
    }
}
