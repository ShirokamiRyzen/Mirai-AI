package com.ryzumi.miraiai.domain.util

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object MiraiToolManager {

    /**
     * Returns the OpenAPI function calling tool definitions for device OS and environmental tools.
     */
    fun getToolDefinitions(): List<JsonObject> {
        val tools = mutableListOf<JsonObject>()

        // 1. get_device_os_status
        val deviceTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "get_device_os_status")
                addProperty("description", "Query current real-time smartphone hardware model, Android OS version, battery percentage, charging state, power save status, and network connection status.")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    add("properties", JsonObject())
                    add("required", JsonArray())
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(deviceTool)

        // 2. get_realtime_clock
        val clockTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "get_realtime_clock")
                addProperty("description", "Query the user's current exact date, local time, day of the week, and timezone.")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    add("properties", JsonObject())
                    add("required", JsonArray())
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(clockTool)

        // 3. get_location_and_weather
        val weatherTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "get_location_and_weather")
                addProperty("description", "Query current GPS coordinates, city/country location name, live weather conditions (temperature, feels like, min/max, humidity, wind, pressure, cloud cover, rain probability, sunrise/sunset), and complete Air Quality Index / IKU (Indeks Kualitas Udara: US AQI, European AQI, PM2.5, PM10, UV Index, O3, NO2, SO2, CO, Dust).")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    add("properties", JsonObject())
                    add("required", JsonArray())
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        // 4. get_network_details
        val networkTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "get_network_details")
                addProperty("description", "Query complete real-time network details including local IP address, public IP address, ISP, connected Wi-Fi SSID, signal strength, saved Wi-Fi networks, and cellular carrier.")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    add("properties", JsonObject())
                    add("required", JsonArray())
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(networkTool)

        // 5. search_web
        val searchWebTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "search_web")
                addProperty("description", "Search the live web using search engine scraper (DuckDuckGo). Returns the top 10 search results containing titles, snippets/summaries, and direct webpage URLs for recent events, news, articles, media, images, or knowledge.")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    val props = JsonObject().apply {
                        val queryProp = JsonObject().apply {
                            addProperty("type", "string")
                            addProperty("description", "The search query keywords to look up on the web.")
                        }
                        add("query", queryProp)
                    }
                    add("properties", props)
                    val required = JsonArray().apply {
                        add("query")
                    }
                    add("required", required)
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(searchWebTool)

        // 6. read_article
        val readArticleTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "read_article")
                addProperty("description", "Fetch and extract clean article text, main content, metadata, and image links from a specific webpage URL using Jsoup and Readability4J. Use this to read full articles, documentation, or links found via web search.")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    val props = JsonObject().apply {
                        val urlProp = JsonObject().apply {
                            addProperty("type", "string")
                            addProperty("description", "The absolute HTTP or HTTPS URL of the webpage or article to read.")
                        }
                        add("url", urlProp)
                    }
                    add("properties", props)
                    val required = JsonArray().apply {
                        add("url")
                    }
                    add("required", required)
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(readArticleTool)

        return tools
    }

    /**
     * Executes the requested tool by name and returns the response string.
     */
    suspend fun executeTool(context: Context, functionName: String, argumentsJson: String? = null): String {
        return try {
            when (functionName) {
                "get_device_os_status" -> DeviceContextManager.getHardwareAndBatteryStatus(context)
                "get_realtime_clock" -> DeviceContextManager.getClockStatus()
                "get_location_and_weather" -> DeviceContextManager.getLocationAndWeatherStatus(context)
                "get_network_details" -> DeviceContextManager.getDetailedNetworkSummary(context)
                "search_web" -> executeSearchWeb(argumentsJson)
                "read_article" -> executeReadArticle(argumentsJson)
                else -> DeviceContextManager.getLiveDeviceContext(context)
            }
        } catch (e: Exception) {
            "Tool execution error: ${e.message}"
        }
    }

    private suspend fun executeSearchWeb(argumentsJson: String?): String {
        val query = parseStringArgument(argumentsJson, "query")
        if (query.isNullOrBlank()) {
            return "Error: 'query' argument is required for search_web."
        }

        val results = com.ryzumi.miraiai.domain.web.DuckDuckGoScraper.search(query, maxResults = 10)
        if (results.isEmpty()) {
            return "No search results found for query: \"$query\"."
        }

        val sb = StringBuilder()
        sb.append("Found ${results.size} search results for \"$query\":\n\n")
        results.forEachIndexed { index, item ->
            sb.append("${index + 1}. ${item.title}\n")
            sb.append("   Link: ${item.link}\n")
            if (item.snippet.isNotBlank()) {
                sb.append("   Snippet: ${item.snippet}\n")
            }
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    private suspend fun executeReadArticle(argumentsJson: String?): String {
        val url = parseStringArgument(argumentsJson, "url")
        if (url.isNullOrBlank()) {
            return "Error: 'url' argument is required for read_article."
        }

        val article = com.ryzumi.miraiai.domain.web.WebContentExtractor.extractArticle(url)
        val sb = StringBuilder()
        sb.append("Title: ${article.title}\n")
        sb.append("Source: ${article.url}\n")
        if (!article.byline.isNullOrBlank()) {
            sb.append("Author: ${article.byline}\n")
        }
        if (!article.excerpt.isNullOrBlank()) {
            sb.append("Excerpt: ${article.excerpt}\n")
        }
        if (article.images.isNotEmpty()) {
            sb.append("Images found:\n")
            article.images.forEach { imgUrl ->
                sb.append("- $imgUrl\n")
            }
        }
        sb.append("\nContent:\n")
        sb.append(article.textContent)

        return sb.toString().trimEnd()
    }

    private fun parseStringArgument(argumentsJson: String?, key: String): String? {
        if (argumentsJson.isNullOrBlank()) return null
        return try {
            val jsonObject = com.google.gson.JsonParser.parseString(argumentsJson).asJsonObject
            if (jsonObject.has(key)) {
                jsonObject.get(key).asString
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

