package com.ryzumi.miraiai.domain.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup

data class ArticleContent(
    val url: String,
    val title: String,
    val textContent: String,
    val excerpt: String? = null,
    val byline: String? = null,
    val images: List<String> = emptyList()
)

object WebContentExtractor {

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val TIMEOUT_MS = 15000
    private const val MAX_CONTENT_LENGTH = 12000 // Prevent exceeding LLM context budget

    /**
     * Fetches HTML from the target URL and extracts clean article text, metadata, and image links
     * using Jsoup and Readability4j.
     */
    suspend fun extractArticle(targetUrl: String): ArticleContent = withContext(Dispatchers.IO) {
        val trimmedUrl = targetUrl.trim()
        require(trimmedUrl.isNotBlank()) { "Target URL cannot be empty" }

        val validUrl = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            "https://$trimmedUrl"
        } else {
            trimmedUrl
        }

        val doc = Jsoup.connect(validUrl)
            .userAgent(USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
            .referrer("https://www.google.com/")
            .timeout(TIMEOUT_MS)
            .followRedirects(true)
            .ignoreHttpErrors(false)
            .get()


        val html = doc.html()
        val readability = Readability4J(validUrl, html)
        val article = readability.parse()

        val title = article.title?.trim().takeUnless { it.isNullOrBlank() }
            ?: doc.title().trim()

        var textContent = article.textContent?.trim() ?: ""

        // Fallback to Jsoup body text if readability produced little or no text
        if (textContent.length < 100) {
            // Remove scripts, styles, navigations, footers
            val cloneDoc = doc.clone()
            cloneDoc.select("script, style, noscript, nav, header, footer, iframe, svg, [role=navigation], [role=banner]").remove()
            val fallbackBody = cloneDoc.body()?.text()?.trim() ?: ""
            if (fallbackBody.isNotBlank()) {
                textContent = fallbackBody
            }
        }

        // Clean up excessive whitespace
        textContent = textContent.replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")

        // Truncate text if excessively long
        if (textContent.length > MAX_CONTENT_LENGTH) {
            textContent = textContent.take(MAX_CONTENT_LENGTH) + "\n\n...[Content truncated due to length]..."
        }

        // Extract key images inside article or page
        val images = mutableListOf<String>()
        article.content?.let { contentHtml ->
            val articleDoc = Jsoup.parse(contentHtml, validUrl)
            for (img in articleDoc.select("img[src]")) {
                val absSrc = img.attr("abs:src")
                if (absSrc.isNotBlank() && (absSrc.startsWith("http://") || absSrc.startsWith("https://"))) {
                    if (!images.contains(absSrc)) {
                        images.add(absSrc)
                    }
                }
            }
        }

        // If no images found in article node, extract top images from original doc
        if (images.isEmpty()) {
            for (img in doc.select("article img[src], main img[src], img[src]")) {
                if (images.size >= 5) break
                val absSrc = img.attr("abs:src")
                if (absSrc.isNotBlank() && (absSrc.startsWith("http://") || absSrc.startsWith("https://"))) {
                    if (!images.contains(absSrc)) {
                        images.add(absSrc)
                    }
                }
            }
        }

        ArticleContent(
            url = validUrl,
            title = title,
            textContent = textContent,
            excerpt = article.excerpt?.trim()?.takeIf { it.isNotBlank() },
            byline = article.byline?.trim()?.takeIf { it.isNotBlank() },
            images = images.take(5)
        )
    }
}
