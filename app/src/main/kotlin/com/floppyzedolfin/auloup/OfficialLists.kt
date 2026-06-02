package com.floppyzedolfin.auloup

/** An official, regulator-published list of telemarketing prefixes to block. */
data class OfficialList(val iso: String, val prefixes: List<String>)

/**
 * Officially-reserved telephone-prospection number ranges, published by
 * national regulators. Currently only France has such a scheme: since
 * 2023-01-01 ARCEP requires cold-callers to use these "Numéros Polyvalents
 * Vérifiés" ranges (mobile 06/07 marketing is banned). Most other countries
 * use opt-out registries instead, so there is nothing comparable to import.
 */
object OfficialLists {

    val all: List<OfficialList> = listOf(
        // France — ARCEP prospection ranges (national 0162, 0163, … in E.164).
        OfficialList(
            iso = "FR",
            prefixes = listOf(
                "+33162", "+33163", "+33270", "+33271", "+33377", "+33378",
                "+33424", "+33425", "+33568", "+33569", "+33948", "+33949",
            ),
        ),
    )
}
