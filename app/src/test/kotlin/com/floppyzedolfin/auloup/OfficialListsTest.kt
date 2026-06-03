package com.floppyzedolfin.auloup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialListsTest {

    @Test
    fun forIso_returnsTheListForACoveredCountry() {
        val fr = OfficialLists.forIso("FR")
        assertNotNull(fr)
        assertEquals("FR", fr!!.iso)
        assertTrue(fr.prefixes.isNotEmpty())
    }

    @Test
    fun forIso_isNullForCountriesWithoutAList() {
        assertNull(OfficialLists.forIso("US"))
        assertNull(OfficialLists.forIso("ZZ"))
    }

    @Test
    fun everyOfficialPrefixIsCanonicalAndBelongsToItsCountry() {
        for (list in OfficialLists.all) {
            for (prefix in list.prefixes) {
                // Stored prefixes should already be in canonical "+CC…" form...
                assertEquals("not canonical: $prefix", prefix, Prefixes.normalize(prefix))
                // ...and actually map to the country they are filed under.
                assertEquals(
                    "$prefix is not a ${list.iso} number",
                    list.iso,
                    Countries.countryForPrefix(prefix)?.iso,
                )
            }
        }
    }
}
