package com.ryzumi.miraiai.domain.macro

object MacroEngine {
    fun processMacros(
        text: String,
        charName: String,
        userName: String
    ): String {
        if (text.isBlank()) return text
        return text
            .replace("{{char}}", charName, ignoreCase = true)
            .replace("<char>", charName, ignoreCase = true)
            .replace("{{user}}", userName, ignoreCase = true)
            .replace("<user>", userName, ignoreCase = true)
    }

    fun stripThinking(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\[Thinking Process\\][\\s\\S]*?\\[Response\\]\\s*", RegexOption.IGNORE_CASE), "")
            .trimStart()
    }
}
