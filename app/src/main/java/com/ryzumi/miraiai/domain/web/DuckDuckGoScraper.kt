package com.ryzumi.miraiai.domain.web

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class SearchResult(
    val title: String,
    val snippet: String,
    val link: String
)

object DuckDuckGoScraper {

    private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val TIMEOUT_MS = 12000

    /**
     * Searches DuckDuckGo for top [maxResults] (default 10) results using Jsoup.
     */
    suspend fun search(query: String, maxResults: Int = 10): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()

        // 1. Primary: lite.duckduckgo.com (Fast, lightweight, resilient against JavaScript captcha modals)
        try {
            val liteDoc = Jsoup.connect("https://lite.duckduckgo.com/lite/")
                .data("q", trimmedQuery)
                .userAgent(DESKTOP_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
                .referrer("https://lite.duckduckgo.com/")
                .timeout(TIMEOUT_MS)
                .post()

            val rows = liteDoc.select("table tr")
            var currentTitle = ""
            var currentLink = ""

            for (row in rows) {
                if (results.size >= maxResults) break

                val linkEl = row.selectFirst("a.result-link")
                if (linkEl != null) {
                    currentTitle = linkEl.text().trim()
                    currentLink = extractCleanUrl(linkEl.attr("href"))
                    continue
                }

                val snippetEl = row.selectFirst("td.result-snippet")
                if (snippetEl != null && currentLink.isNotEmpty()) {
                    val snippet = snippetEl.text().trim()
                    if (!results.any { it.link == currentLink }) {
                        results.add(SearchResult(title = currentTitle, snippet = snippet, link = currentLink))
                    }
                    currentTitle = ""
                    currentLink = ""
                }
            }
        } catch (e: Exception) {
            // Log or continue to fallback
        }

        // 2. Secondary fallback: html.duckduckgo.com
        if (results.isEmpty()) {
            try {
                val doc = Jsoup.connect("https://html.duckduckgo.com/html/")
                    .data("q", trimmedQuery)
                    .userAgent(DESKTOP_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .referrer("https://html.duckduckgo.com/")
                    .timeout(TIMEOUT_MS)
                    .post()

                val resultElements = doc.select(".results_links, .result, .result__body")
                for (element in resultElements) {
                    if (results.size >= maxResults) break

                    val titleAnchor = element.selectFirst("h2.result__title a, a.result__a") ?: continue
                    val rawTitle = titleAnchor.text().trim()
                    val rawHref = titleAnchor.attr("href")

                    val cleanUrl = extractCleanUrl(rawHref)
                    if (cleanUrl.isBlank() || cleanUrl.startsWith("#")) continue

                    val snippetEl = element.selectFirst(".result__snippet, a.result__snippet")
                    val snippet = snippetEl?.text()?.trim() ?: ""

                    if (rawTitle.isNotEmpty() && !results.any { it.link == cleanUrl }) {
                        results.add(SearchResult(title = rawTitle, snippet = snippet, link = cleanUrl))
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }


        results.take(maxResults)
    }

    private fun extractCleanUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return ""
        try {
            var fullUrl = rawUrl
            if (fullUrl.startsWith("//")) {
                fullUrl = "https:$fullUrl"
            }

            // DuckDuckGo redirection link: //duckduckgo.com/l/?uddg=https%3A%2F%2F...
            if (fullUrl.contains("uddg=")) {
                val uri = Uri.parse(fullUrl)
                val uddgParam = uri.getQueryParameter("uddg")
                if (!uddgParam.isNullOrBlank()) {
                    return uddgParam
                }
            }

            // In case of percent encoding
            if (fullUrl.contains("%3A%2F%2F", ignoreCase = true)) {
                val decoded = URLDecoder.decode(fullUrl, StandardCharsets.UTF_8.name())
                val idx = decoded.indexOf("https://").takeIf { it != -1 } ?: decoded.indexOf("http://")
                if (idx != -1) {
                    val candidate = decoded.substring(idx)
                    val endIdx = candidate.indexOf('&').takeIf { it != -1 } ?: candidate.length
                    return candidate.substring(0, endIdx)
                }
            }

            return fullUrl
        } catch (e: Exception) {
            return rawUrl
        }
    }
}
