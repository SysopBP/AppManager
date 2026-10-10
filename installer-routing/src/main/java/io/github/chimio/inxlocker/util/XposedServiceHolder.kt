// SPDX-License-Identifier: GPL-3.0-only
package io.github.chimio.inxlocker.util
import io.github.libxposed.service.XposedService
object XposedServiceHolder {
    @Volatile private var serviceRef: XposedService? = null
    fun set(service: XposedService?) { serviceRef = service }
    fun get(): XposedService? = serviceRef
}
