package com.example

import org.junit.Test
import java.net.URL

class DownloadTest {
    @Test
    fun fetchDocs() {
        try {
            val classLoader = com.google.mlkit.genai.prompt.GenerativeModel::class.java.classLoader
            val sb = java.lang.StringBuilder()
            // Let's guess
            val possiblePkgs = listOf(
                "com.google.mlkit.genai.prompt.",
                "com.google.mlkit.common.model.",
                "com.google.mlkit.genai.",
                "com.google.mlkit.genai.common.",
                "com.google.mlkit.common.",
                "com.google.mlkit."
            )
            val classes = listOf("FeatureStatus", "DownloadStatus")
            for (p in possiblePkgs) {
                for (c in classes) {
                    try {
                        val cls = Class.forName(p + c, false, classLoader)
                        sb.append("FOUND: ${cls.name}\n")
                    } catch(e: Exception) { }
                }
            }
            java.io.File("/out.txt").writeText(sb.toString())
        } catch (e: Exception) {
            java.io.File("/out.txt").writeText("ERR " + e.message)
        }
    }
    
    fun htmlToText(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }
}
