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
     * Fetches the latest Taiwan Google News using the official RSS feed channel.
     * Guaranteed to work dynamically and stably.
     */
    suspend fun fetchLatestTaiwanNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val url = "https://news.google.com/rss?hl=zh-TW&gl=TW&ceid=TW:zh-Hant"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("GoogleNewsFetcher", "Unsuccessful response code: ${response.code}")
                return@withContext emptyList()
            }
            val xmlText = response.body?.string() ?: ""
            val parsed = parseRssXml(xmlText)
            Log.d("GoogleNewsFetcher", "Successfully fetched ${parsed.size} news from RSS")
            return@withContext parsed
        } catch (e: Throwable) {
            Log.e("GoogleNewsFetcher", "Failed to fetch Google RSS News", e)
            return@withContext emptyList()
        }
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
