package com.floppyzedolfin.auloup

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.ZoneId

/**
 * A blocked prefix: how many calls it has blocked, and whether it came from an
 * official regulator list (vs. added by the user).
 */
data class BlockedPrefix(val prefix: String, val blockedCount: Int, val official: Boolean)

/** One blocked call: the matched prefix, the caller's number, and when. */
data class BlockedCall(val prefix: String, val number: String, val timeMillis: Long)

/** The decision for one screened call, plus how many were blocked today. */
data class ScreenResult(val blocked: Boolean, val notify: Boolean, val blockedToday: Int = 0)

/** Stored values for the theme preference. */
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    /** Whether to use the dark colour scheme for [mode], given the OS setting. */
    fun isDark(mode: String, systemDark: Boolean): Boolean = when (mode) {
        LIGHT -> false
        DARK -> true
        else -> systemDark
    }
}

private val Context.dataStore by preferencesDataStore(name = "auloup")

/**
 * Single source of truth for the configured prefixes, the history of blocked
 * calls, and the "notify on block" preference.
 *
 * Both the prefix list and the call history are stored as small JSON values
 * under one key each — keeping the schema in one place with no extra
 * dependencies. Per-prefix counts are derived from the history, so they can
 * never drift from it.
 */
class PrefixRepository(private val context: Context) {

    private val prefixesKey = stringPreferencesKey("prefixes")
    private val historyKey = stringPreferencesKey("history")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val themeKey = stringPreferencesKey("theme_mode")

    /** Configured prefixes with their derived block counts, sorted by prefix. */
    val prefixes: Flow<List<BlockedPrefix>> =
        context.dataStore.data.map { prefs ->
            val counts = PrefixData.countsByPrefix(PrefixData.decodeHistory(prefs[historyKey]))
            PrefixData.decodePrefixes(prefs[prefixesKey])
                .map { (prefix, official) -> BlockedPrefix(prefix, counts[prefix] ?: 0, official) }
                .sortedBy { it.prefix }
        }

    /** Whether to show a notification when a call is blocked (default on). */
    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsKey] ?: true }

    /** Theme preference: "system" (default), "light", or "dark". */
    val themeMode: Flow<String> =
        context.dataStore.data.map { it[themeKey] ?: ThemeMode.SYSTEM }

    /** Every blocked call, most recent first. */
    val allCalls: Flow<List<BlockedCall>> =
        context.dataStore.data.map { prefs ->
            PrefixData.decodeHistory(prefs[historyKey]).sortedByDescending { it.timeMillis }
        }

    /** Blocked calls for [prefix], most recent first. */
    fun history(prefix: String): Flow<List<BlockedCall>> = context.dataStore.data.map { prefs ->
        PrefixData.decodeHistory(prefs[historyKey])
            .filter { it.prefix == prefix }
            .sortedByDescending { it.timeMillis }
    }

    suspend fun add(raw: String, official: Boolean = false) {
        val prefix = Prefixes.normalize(raw) ?: return
        context.dataStore.edit { prefs ->
            val map = PrefixData.decodePrefixes(prefs[prefixesKey]).toMutableMap()
            map[prefix] = official || (map[prefix] ?: false)
            prefs[prefixesKey] = PrefixData.encodePrefixes(map)
        }
    }

    /** Imports a regulator list, flagging every prefix as official. */
    suspend fun importOfficial(prefixes: List<String>) {
        context.dataStore.edit { prefs ->
            val map = PrefixData.decodePrefixes(prefs[prefixesKey]).toMutableMap()
            for (raw in prefixes) {
                val prefix = Prefixes.normalize(raw) ?: continue
                map[prefix] = true
            }
            prefs[prefixesKey] = PrefixData.encodePrefixes(map)
        }
    }

    /** Removes a prefix and forgets its blocked-call history. */
    suspend fun remove(prefix: String) {
        context.dataStore.edit { prefs ->
            val map = PrefixData.decodePrefixes(prefs[prefixesKey]).toMutableMap().apply { remove(prefix) }
            prefs[prefixesKey] = PrefixData.encodePrefixes(map)
            val history = PrefixData.decodeHistory(prefs[historyKey]).filterNot { it.prefix == prefix }
            prefs[historyKey] = PrefixData.encodeHistory(history)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsKey] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeKey] = mode }
    }

    /**
     * Screens a call against the stored prefixes. [internationalNumber] is the
     * caller's number canonicalised to international form for matching, while
     * [rawNumber] is what actually arrived (recorded in the history). If it
     * matches, records the call against the most specific (longest) matching
     * prefix and reports that the call should be blocked, plus whether to notify.
     */
    suspend fun screenAndRecord(rawNumber: String, internationalNumber: String, timeMillis: Long): ScreenResult {
        val prefs = context.dataStore.data.first()
        val notify = prefs[notificationsKey] ?: true
        val match = Prefixes.longestMatch(internationalNumber, PrefixData.decodePrefixes(prefs[prefixesKey]).keys)
            ?: return ScreenResult(blocked = false, notify = notify)

        var blockedToday = 0
        context.dataStore.edit { p ->
            val history = PrefixData.decodeHistory(p[historyKey]) + BlockedCall(match, rawNumber, timeMillis)
            p[historyKey] = PrefixData.encodeHistory(history)
            blockedToday = PrefixData.blockedOnDayOf(history, timeMillis, ZoneId.systemDefault())
        }
        return ScreenResult(blocked = true, notify = notify, blockedToday = blockedToday)
    }
}
