package com.ryzumi.miraiai.domain.util

object ImagePromptExtractor {

    /**
     * Checks if the text indicates an explicit image generation request.
     */
    fun isImageGenerationRequest(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase().trim()

        val keywords = listOf(
            "generate gambar", "buat gambar", "buatkan gambar", "bikin gambar", "bikinin gambar",
            "gambarin", "gambarkan", "draw an image", "draw a picture", "generate an image",
            "create an image", "create a picture", "render an image", "lukiskan", "lukis gambar",
            "hasilkan gambar", "bisa generate gambar", "tolong buat gambar", "tolong buatkan gambar"
        )

        if (keywords.any { lower.contains(it) }) return true

        // Regex patterns
        val pattern = Regex("""(?i)\b(generate|buat(?:kan)?|bikin(?:kan)?|bikinin|draw|create|make|render|lukis(?:kan)?)\s+.*?(?:gambar|image|foto|picture|lukisan|artwork|ilustrasi|illustration|photo)""")
        return pattern.containsMatchIn(text)
    }

    /**
     * Extracts a subject prompt from the user message or assistant response.
     */
    fun extractPrompt(userText: String, assistantText: String? = null): String? {
        val userPrompt = extractFromText(userText)
        if (!userPrompt.isNullOrBlank()) {
            return userPrompt
        }

        if (!assistantText.isNullOrBlank()) {
            val assistantPrompt = extractFromAssistantText(assistantText)
            if (!assistantPrompt.isNullOrBlank()) {
                return assistantPrompt
            }
        }

        return null
    }

    private fun extractFromText(text: String): String? {
        val patterns = listOf(
            Regex("""(?i)(?:bisakah\s+kamu\s+|tolong\s+|bisa\s+)?(?:generate|buat(?:kan)?|bikin(?:kan)?|bikinin|draw|create|render|lukis(?:kan)?)\s+(?:sebuah\s+|suatu\s+)?(?:gambar|image|foto|picture|lukisan|artwork|ilustrasi|illustration)\s+(?:tentang\s+|dari\s+|of\s+)?(.+)"""),
            Regex("""(?i)(?:bisakah\s+kamu\s+|tolong\s+|bisa\s+)?(?:gambarkan|gambarin|lukiskan)\s+(?:sebuah\s+|suatu\s+)?(.+)"""),
            Regex("""(?i)(?:gambar|image|picture)\s+(?:tentang\s+|dari\s+|of\s+)(.+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text.trim())
            if (match != null && match.groupValues.size > 1) {
                var extracted = match.groupValues[1].trim()
                // Clean punctuation at end (e.g. ?, !, .)
                extracted = extracted.replace(Regex("""[?!.,~]+$"""), "").trim()
                if (extracted.isNotBlank() && extracted.length > 2) {
                    return extracted
                }
            }
        }

        if (isImageGenerationRequest(text)) {
            // Fallback: strip command words
            val cleaned = text.replace(Regex("""(?i)\b(bisakah\s+kamu|tolong|bisa|generate|buatkan|buat|bikin|draw|create|render|gambar|image|foto|picture|lukisan|artwork|sebuah|suatu|tentang|dong|ya|kan)\b"""), " ")
                .replace(Regex("""\s+"""), " ")
                .replace(Regex("""[?!.,~]+$"""), "")
                .trim()
            if (cleaned.isNotBlank()) {
                return cleaned
            }
        }

        return null
    }

    private fun extractFromAssistantText(text: String): String? {
        val patterns = listOf(
            Regex("""(?i)(?:membuat|generate)\s+gambar\s+(.+?)(?:\s+(?:yang|sedang|dengan|untuk|~|\.|\!|\?)|$)"""),
            Regex("""(?i)(?:permintaan\s+untuk\s+membuat\s+gambar\s+)(.+?)(?:\s+(?:yang|sedang|dengan|untuk|~|\.|\!|\?)|$)"""),
            Regex("""(?i)(?:generating\s+an?\s+image\s+of\s+)(.+?)(?:\s+(?:that|with|for|\.|\!|\?)|$)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                var extracted = match.groupValues[1].trim()
                extracted = extracted.replace(Regex("""[?!.,~]+$"""), "").trim()
                if (extracted.isNotBlank() && extracted.length > 2) {
                    return extracted
                }
            }
        }
        return null
    }
}
