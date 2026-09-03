package com.ryzumi.miraiai

import com.ryzumi.miraiai.domain.macro.MacroEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroEngineTest {

    @Test
    fun testMacroReplacementStandard() {
        val input = "Hello {{user}}! I am {{char}}."
        val result = MacroEngine.processMacros(
            text = input,
            charName = "Aria",
            userName = "Fatih"
        )
        assertEquals("Hello Fatih! I am Aria.", result)
    }

    @Test
    fun testMacroReplacementCaseInsensitiveAndAngleBrackets() {
        val input = "{{CHAR}} looks at <user> and says hi."
        val result = MacroEngine.processMacros(
            text = input,
            charName = "Aria",
            userName = "Fatih"
        )
        assertEquals("Aria looks at Fatih and says hi.", result)
    }

    @Test
    fun testEmptyInput() {
        val result = MacroEngine.processMacros(
            text = "",
            charName = "Aria",
            userName = "Fatih"
        )
        assertEquals("", result)
    }
}
