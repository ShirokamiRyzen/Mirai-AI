package com.ryzumi.miraiai.domain.util

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.ryzumi.miraiai.domain.engine.ImageGenerationManager

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
                addProperty("description", "Fetch and read any URL content from the web, including webpage articles, REST API endpoints, raw JSON, plain text, XML, or documentation. Always use this whenever the user provides any URL or link.")
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
        // 7. generate_image
        val imageGenTool = JsonObject().apply {
            addProperty("type", "function")
            val fn = JsonObject().apply {
                addProperty("name", "generate_image")
                addProperty("description", "Generate or draw an image, artwork, illustration, or photo based on a descriptive prompt. Use this whenever the user asks to generate, create, draw, paint, or make an image/picture/photo.")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    val props = JsonObject().apply {
                        val promptProp = JsonObject().apply {
                            addProperty("type", "string")
                            addProperty("description", "The detailed descriptive prompt for image generation in English.")
                        }
                        add("prompt", promptProp)
                    }
                    add("properties", props)
                    val required = JsonArray().apply {
                        add("prompt")
                    }
                    add("required", required)
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(imageGenTool)

        return tools
    }

    /**
     * Executes the requested tool by name and returns the response string.
     */
    suspend fun executeTool(context: Context, functionName: String, argumentsJson: String? = null): String {
        android.util.Log.d("MiraiToolManager", "executeTool called: $functionName with args: $argumentsJson")
        return try {
            val result = when (functionName) {
                "get_device_os_status" -> DeviceContextManager.getHardwareAndBatteryStatus(context)
                "get_realtime_clock" -> DeviceContextManager.getClockStatus()
                "get_location_and_weather" -> DeviceContextManager.getLocationAndWeatherStatus(context)
                "get_network_details" -> DeviceContextManager.getDetailedNetworkSummary(context)
                "search_web" -> executeSearchWeb(argumentsJson)
                "read_article" -> executeReadArticle(argumentsJson)
                "generate_image" -> executeGenerateImage(context, argumentsJson)
                else -> DeviceContextManager.getLiveDeviceContext(context)
            }
            android.util.Log.d("MiraiToolManager", "Tool $functionName result preview: ${result.take(200)}")
            result
        } catch (e: Exception) {
            android.util.Log.e("MiraiToolManager", "Tool $functionName failed", e)
            "Tool execution error: ${e.message}"
        }
    }

    private suspend fun executeGenerateImage(context: Context, argumentsJson: String?): String {
        val prompt = parseStringArgument(argumentsJson, "prompt")
        if (prompt.isNullOrBlank()) {
            return "Error: 'prompt' argument is required for generate_image."
        }

        val activeConfig = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.ryzumi.miraiai.data.local.MiraiDatabase.getInstance(context).inferenceConfigDao().getActiveConfigSync()
            } catch (e: Exception) { null }
        }
        val modelName = activeConfig?.imageGenModelId?.takeIf { it != "none" && it.isNotBlank() } ?: "Stable Diffusion 3.5"

        val result = ImageGenerationManager.generateImage(context, prompt, modelName, activeConfig)
        return if (result.isSuccess) {
            val localPath = result.getOrNull() ?: ""
            "Image generation completed successfully! Local image file saved at: $localPath. Prompt: \"$prompt\"."
        } else {
            "Image generation failed: ${result.exceptionOrNull()?.message}"
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
        val content = article.textContent.trim()

        // If the content is raw JSON, XML, or plain structured data, return the raw data as-is
        val isJson = (content.startsWith("{") && content.endsWith("}")) ||
                (content.startsWith("[") && content.endsWith("]"))
        if (isJson || article.title == "API JSON Response" || article.title == "Plain Text / Web Content") {
            return content
        }

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
        sb.append(content)

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

