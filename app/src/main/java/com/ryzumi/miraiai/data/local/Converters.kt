package com.ryzumi.miraiai.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    private val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun fromListToString(list: List<String>?): String {
        if (list == null) return "[]"
        return gson.toJson(list)
    }

    @TypeConverter
    @JvmStatic
    fun fromStringToList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
