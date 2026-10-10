// SPDX-License-Identifier: GPL-3.0-only
// Adapted from Chimioo/InxLocker; native, event-driven preferences for Sphere.
package io.github.chimio.inxlocker.util

import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

object PrefsProvider {
    private var prefs: SharedPreferences? = null
    private val cache = ConcurrentHashMap<String, Any>()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        key?.let {
            val value = prefs.all[it]
            if (value == null) cache.remove(it) else cache[it] = value
        }
    }
    @Synchronized fun init(hookPrefs: SharedPreferences) {
        release()
        prefs = hookPrefs
        hookPrefs.registerOnSharedPreferenceChangeListener(listener)
        hookPrefs.all.forEach { (key, value) -> if (value != null) cache[key] = value }
    }
    fun getString(key: String, defValue: String? = null): String? = cache[key] as? String ?: defValue
    fun getBoolean(key: String, defValue: Boolean = false): Boolean = cache[key] as? Boolean ?: defValue
    @Suppress("UNCHECKED_CAST")
    fun getStringSet(key: String, defValue: Set<String> = emptySet()): Set<String> =
        cache[key] as? Set<String> ?: defValue
    @Synchronized fun release() {
        runCatching { prefs?.unregisterOnSharedPreferenceChangeListener(listener) }
        prefs = null
        cache.clear()
    }
}
