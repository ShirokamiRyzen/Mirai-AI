package com.ryzumi.miraiai

import com.ryzumi.miraiai.domain.util.MarkdownRenderer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRendererTest {

    @Test
    fun testHeadingsParsing() {
        val input = """
            # Heading 1
            ## Heading 2
            ### Heading 3
        """.trimIndent()

        val parsed = MarkdownRenderer.parseMarkdown(input)
        val plainText = parsed.text

        // Headings should not contain raw # prefixes
        assertFalse(plainText.contains("# Heading 1"))
        assertFalse(plainText.contains("## Heading 2"))
        assertFalse(plainText.contains("### Heading 3"))
        assertTrue(plainText.contains("Heading 1"))
        assertTrue(plainText.contains("Heading 2"))
        assertTrue(plainText.contains("Heading 3"))
    }

    @Test
    fun testHorizontalRuleParsing() {
        val input = """
            First Line
            ---
            Second Line
        """.trimIndent()

        val parsed = MarkdownRenderer.parseMarkdown(input)
        val plainText = parsed.text

        assertFalse(plainText.contains("---"))
        assertTrue(plainText.contains("─────"))
    }

    @Test
    fun testBlockquoteParsing() {
        val input = "> Fakta penting tentang tubuh kita."
        val parsed = MarkdownRenderer.parseMarkdown(input)
        val plainText = parsed.text

        assertFalse(plainText.startsWith(">"))
        assertTrue(plainText.contains("▍ Fakta penting tentang tubuh kita."))
    }

    @Test
    fun testTableParsing() {
        val input = """
            | 🏷️ Bagian | 📝 Penjelasan Singkat |
            |---|---|
            | Vulva | Bagian luar organ genital perempuan |
            | Klitoris | Organ kecil di atas lubang uretra |
        """.trimIndent()

        val parsed = MarkdownRenderer.parseMarkdown(input)
        val plainText = parsed.text

        // Delimiter line |---|---| should be completely removed
        assertFalse(plainText.contains("|---|---|"))
        // Table content rendered into structured view
        assertTrue(plainText.contains("Bagian"))
        assertTrue(plainText.contains("Vulva"))
        assertTrue(plainText.contains("Klitoris"))
    }

    @Test
    fun testListsAndInlineStyles() {
        val input = """
            - Item **bold** with *italic*
            1. Numbered item with `code`
            ~~strikethrough~~
        """.trimIndent()

        val parsed = MarkdownRenderer.parseMarkdown(input)
        val plainText = parsed.text

        assertTrue(plainText.contains("• Item bold with italic"))
        assertTrue(plainText.contains("1. Numbered item with code"))
        assertTrue(plainText.contains("strikethrough"))
    }
}
