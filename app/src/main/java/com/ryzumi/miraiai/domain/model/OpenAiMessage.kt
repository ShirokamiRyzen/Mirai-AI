package com.ryzumi.miraiai.domain.model

data class OpenAiMessage(
    val role: String, // "system", "user", "assistant", "tool"
    val content: Any? = null, // String OR List<OpenAiContentPart> for Vision models, or String for tool response
    val name: String? = null,
    val tool_call_id: String? = null,
    val tool_calls: List<OpenAiToolCall>? = null
)

data class OpenAiToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAiFunctionCall
)

data class OpenAiFunctionCall(
    val name: String,
    val arguments: String
)

data class OpenAiContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    val image_url: OpenAiImageUrl? = null
)

data class OpenAiImageUrl(
    val url: String // "data:image/jpeg;base64,..." or http url
)
