// SPDX-License-Identifier: GPL-3.0-only
// Derived from Chimioo/InxLocker, revision b783e91447005b9d2d6922670bd18986cb7e9d0a.
@file:Suppress("DEPRECATION")

package io.github.chimio.inxlocker.util

import android.content.Intent

object IntentAnalyzer {
    const val EXTRA_BYPASS_ROUTING = "io.github.muntashirakon.AppManager.extra.BYPASS_INSTALLER_ROUTING"
    private const val TAG = "IntentAnalyzer"
    private const val INTENT_ACTION_CONFIRM_INSTALL = "android.content.pm.action.CONFIRM_INSTALL"
    private const val INTENT_ACTION_CONFIRM_PERMISSIONS = "android.content.pm.action.CONFIRM_PERMISSIONS"

    sealed class Result {
        object ShouldRedirect : Result()
        object ShouldNotRedirect : Result()
    }

    fun analyze(intent: Intent): Result = runCatching {
        if (intent.getBooleanExtra(EXTRA_BYPASS_ROUTING, false)) return Result.ShouldNotRedirect
        if (!PrefsProvider.getBoolean("routing_enabled", false)) return Result.ShouldNotRedirect
        if (intent.action !in allowedActions) return Result.ShouldNotRedirect
        val target = if (intent.action == Intent.ACTION_DELETE || intent.action == Intent.ACTION_UNINSTALL_PACKAGE) {
            if (PrefsProvider.getBoolean("follow_uninstall_with_installer", true))
                PrefsProvider.getString("selected_installer_package", "")
            else PrefsProvider.getString("selected_uninstaller_package", "")
        } else PrefsProvider.getString("selected_installer_package", "")
        if (target.isNullOrBlank()) return Result.ShouldNotRedirect
        if (intent.component?.packageName == target || intent.`package` == target)
            return Result.ShouldNotRedirect
        val original = IntentSnapshot.capture(intent)

        if (PrefsProvider.getBoolean("enable_debug_log", false)) {
        d(TAG, "Intent action: ${original.action}")
        d(TAG, "Intent component: ${original.component}")
        d(TAG, "package: ${original.packageName}")
        d(TAG, "Intent type: ${intent.type}")
        d(TAG, "Intent data: ${intent.data}")
        d(TAG, "Intent clipData: ${intent.clipData}")
        d(TAG, "Intent extras: ${formatExtras(intent)}")
        }

        if (intent.action !in allowedActions) return Result.ShouldNotRedirect

        val forcedComponentPackages = PrefsProvider
            .getStringSet("forced_installer_components")
            .mapNotNull { it.substringBefore('/', missingDelimiterValue = "").takeIf { pkg -> pkg.isNotBlank() } }
            .toSet()

        intent.takeIf {
            mimeTypeFromIntent(it) || hasValidAction(it) || mimeTypeFromCIntentData(it)
        }?.takeIf {
            !hasSpecificComponent(it) || (it.component?.packageName in forcedComponentPackages)
        }?.run {
            when (action) {
                Intent.ACTION_DELETE,
                Intent.ACTION_UNINSTALL_PACKAGE -> {
                    if (PrefsProvider.getBoolean("intercept_uninstall", false)) {
                        Result.ShouldRedirect
                    } else {
                        Result.ShouldNotRedirect
                    }
                }

                INTENT_ACTION_CONFIRM_INSTALL,
                INTENT_ACTION_CONFIRM_PERMISSIONS -> {
                    if (PrefsProvider.getBoolean("intercept_session_install", false)) {
                        Result.ShouldRedirect
                    } else {
                        Result.ShouldNotRedirect
                    }
                }

                else -> Result.ShouldRedirect
            }
        } ?: Result.ShouldNotRedirect
    }.getOrElse {
        Result.ShouldNotRedirect
    }

    fun formatExtras(intent: Intent): String {
        return intent.extras?.let { bundle ->
            if (bundle.isEmpty) "{}"
            else bundle.keySet().joinToString(", ", "{", "}") { key ->
                "$key=${bundle.get(key)}"
            }
        } ?: "null"
    }

    private fun hasValidAction(intent: Intent): Boolean {
        return intent.action in listOf(
            Intent.ACTION_INSTALL_PACKAGE,
            INTENT_ACTION_CONFIRM_INSTALL,
            INTENT_ACTION_CONFIRM_PERMISSIONS,
            Intent.ACTION_UNINSTALL_PACKAGE,
            Intent.ACTION_DELETE
        )
    }

    private fun mimeTypeFromIntent(intent: Intent): Boolean {
        return intent.type == "application/vnd.android.package-archive"
    }

    //字符串分析大法，不优雅，但是好像没什么问题
    private fun mimeTypeFromCIntentData(intent: Intent): Boolean {
        val uri = intent.data?.toString().orEmpty()
        return hasApkFileExtension(uri) &&
                (uri.startsWith("file://") || uri.startsWith("content://"))
    }

    private fun hasApkFileExtension(uri: String): Boolean {
        val ext = uri.lowercase()
        return listOf(".apk", ".apks", ".apk.1").any { ext.endsWith(it) }
    }

    private fun hasSpecificComponent(intent: Intent): Boolean {
        return intent.component != null
    }


    private val allowedActions = listOf(
        Intent.ACTION_VIEW,
        Intent.ACTION_INSTALL_PACKAGE,
        INTENT_ACTION_CONFIRM_INSTALL,
        INTENT_ACTION_CONFIRM_PERMISSIONS,
        Intent.ACTION_UNINSTALL_PACKAGE,
        Intent.ACTION_DELETE
    )
}