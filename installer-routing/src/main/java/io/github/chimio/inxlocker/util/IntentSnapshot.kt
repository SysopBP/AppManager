// SPDX-License-Identifier: GPL-3.0-only
// Derived from Chimioo/InxLocker, revision b783e91447005b9d2d6922670bd18986cb7e9d0a.
package io.github.chimio.inxlocker.util

import android.content.ComponentName
import android.content.Intent

data class IntentSnapshot(
    val action: String?,
    val component: ComponentName?,
    val packageName: String?
) {
    companion object {
        fun capture(intent: Intent) = IntentSnapshot(
            action = intent.action,
            component = intent.component,
            packageName = intent.`package`
        )
    }
}