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
                addProperty("description", "Query current GPS coordinates, city/country location name, and real-time live weather conditions (temperature, humidity, condition, wind speed).")
                val params = JsonObject().apply {
                    addProperty("type", "object")
                    add("properties", JsonObject())
                    add("required", JsonArray())
                }
                add("parameters", params)
            }
            add("function", fn)
        }
        tools.add(weatherTool)

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
                else -> DeviceContextManager.getLiveDeviceContext(context)
            }
        } catch (e: Exception) {
            "Tool execution error: ${e.message}"
        }
    }
}
