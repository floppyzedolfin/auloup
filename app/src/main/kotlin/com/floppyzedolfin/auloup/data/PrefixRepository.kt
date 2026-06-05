package com.floppyzedolfin.auloup.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.Prefixes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.ZoneId

/**
 * A blocked prefix: how many calls it has blocked, and whether it is currently
 * enabled (only enabled prefixes block calls).
 */
data class BlockedPrefix(val prefix: String, val blockedCount: Int, val enabled: Boolean)

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
    private val seededKey = booleanPreferencesKey("defaults_seeded")

    /** Configured prefixes with their derived block counts, sorted by prefix. */
    val prefixes: Flow<List<BlockedPrefix>> =
        context.dataStore.data.map { prefs ->
            val counts = PrefixData.countsByPrefix(PrefixData.decodeHistory(prefs[historyKey]))
            PrefixData.decodePrefixes(prefs[prefixesKey])
                .map { (prefix, enabled) -> BlockedPrefix(prefix, counts[prefix] ?: 0, enabled) }
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

    /** Adds a prefix (enabled). Re-adding an existing prefix re-enables it. */
    suspend fun add(raw: String) {
        val prefix = Prefixes.normalize(raw) ?: return
        context.dataStore.edit { prefs ->
            val map = PrefixData.decodePrefixes(prefs[prefixesKey]).toMutableMap()
            map[prefix] = true
            prefs[prefixesKey] = PrefixData.encodePrefixes(map)
        }
    }

    /** Enables or disables a prefix; only enabled prefixes block calls. */
    suspend fun setEnabled(prefix: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val map = PrefixData.decodePrefixes(prefs[prefixesKey]).toMutableMap()
            if (prefix in map) {
                map[prefix] = enabled
                prefs[prefixesKey] = PrefixData.encodePrefixes(map)
            }
        }
    }

    /**
     * On first launch, seed the official regulator prefixes for the device's
     * region (enabled), so the relevant ones ship out of the box. Runs once,
     * guarded by [seededKey], so the user's later edits are never undone.
     */
    suspend fun seedDefaultsIfNeeded() {
        if (context.dataStore.data.first()[seededKey] == true) return
        val region = Countries.defaultFor(context).iso
        val official = OfficialLists.forIso(region)?.prefixes.orEmpty()
        context.dataStore.edit { prefs ->
            val map = PrefixData.decodePrefixes(prefs[prefixesKey]).toMutableMap()
            for (raw in official) {
                val prefix = Prefixes.normalize(raw) ?: continue
                if (prefix !in map) map[prefix] = true
            }
            prefs[prefixesKey] = PrefixData.encodePrefixes(map)
            prefs[seededKey] = true
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

    /** Forgets the whole blocked-call history. The configured prefixes are kept. */
    suspend fun clearHistory() {
        context.dataStore.edit { it.remove(historyKey) }
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
        val enabled = PrefixData.decodePrefixes(prefs[prefixesKey]).filterValues { it }.keys
        val match = Prefixes.longestMatch(internationalNumber, enabled)
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
