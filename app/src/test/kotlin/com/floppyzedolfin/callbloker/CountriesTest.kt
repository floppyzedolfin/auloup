package com.floppyzedolfin.callbloker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountriesTest {

    @Test
    fun forIso_returnsCallingCode() {
        assertEquals(1, Countries.forIso("US")?.dialCode)
        assertEquals(44, Countries.forIso("GB")?.dialCode)
        assertEquals(33, Countries.forIso("fr")?.dialCode) // case-insensitive
    }

    @Test
    fun forIso_unknownReturnsNull() {
        assertNull(Countries.forIso("ZZ"))
    }

    @Test
    fun flag_isDerivedFromIso() {
        assertEquals("🇺🇸", Countries.forIso("US")?.flag)
        assertEquals("🇬🇧", Countries.forIso("GB")?.flag)
    }

    @Test
    fun all_isSortedAndNonEmpty() {
        val all = Countries.all()
        assertTrue(all.size > 200)
        val names = all.map { it.displayName }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun trunkPrefix_perCountry() {
        assertEquals("0", Countries.forIso("FR")?.trunkPrefix) // most of the world
        assertEquals("0", Countries.forIso("GB")?.trunkPrefix)
        assertEquals("1", Countries.forIso("US")?.trunkPrefix) // NANP
        assertEquals("8", Countries.forIso("RU")?.trunkPrefix)
        assertEquals("", Countries.forIso("IT")?.trunkPrefix) // leading 0 is part of the number
        assertEquals("06", Countries.forIso("HU")?.trunkPrefix)
    }
}
