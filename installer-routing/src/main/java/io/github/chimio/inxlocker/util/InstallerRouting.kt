// SPDX-License-Identifier: GPL-3.0-only
// Native UI bridge for the InxLocker engine.
package io.github.chimio.inxlocker.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

object InstallerRouting {
    private lateinit var local: SharedPreferences
    @Volatile private var remote: SharedPreferences? = null
    @Volatile private var initialized = false
    private val main = Handler(Looper.getMainLooper())
    interface ReloadCallback { fun onResult(message: String) }

    @JvmStatic @Synchronized fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        local = context.getSharedPreferences("selected_installer_package", Context.MODE_PRIVATE)
        val old = context.getSharedPreferences("appsphere_installer_control", Context.MODE_PRIVATE)
            .getString("preferred_installer", "")
        if (!local.contains("selected_installer_package") && !old.isNullOrEmpty()) {
            local.edit().putString("selected_installer_package", old).apply()
        }
        PrefsProvider.init(local)
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                try {
                    val shared = service.getRemotePreferences("selected_installer_package")
                    val editor = shared.edit()
                    local.all.forEach { (key, value) ->
                        when (value) {
                            is String -> editor.putString(key, value)
                            is Boolean -> editor.putBoolean(key, value)
                            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                        }
                    }
                    editor.apply()
                    remote = shared
                    PrefsProvider.init(shared)
                    XposedServiceHolder.set(service)
                } catch (_: Exception) {
                    remote = null
                    XposedServiceHolder.set(null)
                    PrefsProvider.init(local)
                }
            }
            override fun onServiceDied(service: XposedService) {
                remote = null
                XposedServiceHolder.set(null)
                PrefsProvider.init(local)
            }
        })
    }
    @JvmStatic fun isActive(): Boolean = XposedServiceHolder.get() != null
    @JvmStatic fun supportsHotReload(): Boolean = HotReloadTrigger.probe() is HotReloadTrigger.Capability.Available
    @JvmStatic fun getString(key: String, fallback: String): String = local.getString(key, fallback) ?: fallback
    @JvmStatic fun getBoolean(key: String, fallback: Boolean): Boolean = local.getBoolean(key, fallback)
    @JvmStatic fun putString(key: String, value: String) {
        local.edit().putString(key, value).apply()
        remote?.edit()?.putString(key, value)?.apply()
    }
    @JvmStatic fun putBoolean(key: String, value: Boolean) {
        local.edit().putBoolean(key, value).apply()
        remote?.edit()?.putBoolean(key, value)?.apply()
    }
    @JvmStatic fun setForcedComponent(packageName: String, component: String) {
        val set = local.getStringSet("forced_installer_components", emptySet())!!.toMutableSet()
        set.removeAll { it.startsWith(packageName + "/") }
        if (component.isNotBlank()) set.add(packageName + "/" + component)
        local.edit().putStringSet("forced_installer_components", set).apply()
        remote?.edit()?.putStringSet("forced_installer_components", set)?.apply()
    }
    @JvmStatic fun getForcedComponent(packageName: String): String =
        local.getStringSet("forced_installer_components", emptySet())!!
            .firstOrNull { it.startsWith(packageName + "/") }?.substringAfter('/') ?: ""
    @JvmStatic fun getForcedComponents(): Set<String> =
        local.getStringSet("forced_installer_components", emptySet())!!.toSet()
    @JvmStatic fun setForcedComponents(components: Set<String>) {
        local.edit().putStringSet("forced_installer_components", components).apply()
        remote?.edit()?.putStringSet("forced_installer_components", components)?.apply()
    }
    @JvmStatic fun reset() {
        local.edit().clear().putString("selected_installer_package", "").apply()
        remote?.edit()?.clear()?.putString("selected_installer_package", "")?.apply()
    }
    @JvmStatic fun reload(callback: ReloadCallback) {
        if (!supportsHotReload()) {
            callback.onResult("Hot reload requires an active LSPosed service with API 102. Restart scoped apps instead.")
            return
        }
        Thread({
            try {
                val started = HotReloadTrigger.reloadAllStale(onlyStale = false) { outcome ->
                    callback.onResult("Reloaded: ${outcome.success}; failed: ${outcome.failed}; unsupported: ${outcome.skipped}; exited: ${outcome.processDied}")
                }
                if (!started) main.post { callback.onResult("A hot reload is already running.") }
            } catch (e: Exception) {
                main.post { callback.onResult(e.message ?: "Hot reload failed.") }
            }
        }, "Sphere-hook-reload").start()
    }
    @JvmStatic fun shouldRedirect(intent: Intent): Boolean =
        IntentAnalyzer.analyze(intent) is IntentAnalyzer.Result.ShouldRedirect
}
