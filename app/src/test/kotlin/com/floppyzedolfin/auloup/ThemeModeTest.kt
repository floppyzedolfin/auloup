package com.floppyzedolfin.auloup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun explicitChoicesIgnoreTheSystemSetting() {
        assertFalse(ThemeMode.isDark(ThemeMode.LIGHT, systemDark = true))
        assertFalse(ThemeMode.isDark(ThemeMode.LIGHT, systemDark = false))
        assertTrue(ThemeMode.isDark(ThemeMode.DARK, systemDark = false))
        assertTrue(ThemeMode.isDark(ThemeMode.DARK, systemDark = true))
    }

    @Test
    fun systemAndUnknownFollowTheOsSetting() {
        assertTrue(ThemeMode.isDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(ThemeMode.isDark(ThemeMode.SYSTEM, systemDark = false))
        // Any unrecognised value falls back to the system setting.
        assertTrue(ThemeMode.isDark("whatever", systemDark = true))
    }
}
