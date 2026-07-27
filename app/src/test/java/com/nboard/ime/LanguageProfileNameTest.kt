package com.nboard.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageProfileNameTest {
    @Test
    fun blankCustomName_usesLayoutDisplayName() {
        assertEquals(
            "Azerty (Gboard)",
            resolveLanguageProfileDisplayName("  ", "Azerty (Gboard)")
        )
    }

    @Test
    fun customName_isTrimmedAndPreferred() {
        assertEquals(
            "Work French",
            resolveLanguageProfileDisplayName("  Work French  ", "Azerty (Gboard)")
        )
    }
}
