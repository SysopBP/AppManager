// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.xposed;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Optional LSPosed scoped routing of explicit APK install intents.
 *
 * Only runs in apps explicitly scoped by the user in LSPosed. Does not intercept
 * PackageInstaller sessions, system_server, or uninstall operations.
 */
public final class InstallerRoutingHook implements IXposedHookLoadPackage {
    private static final String APPSPHERE = "io.github.muntashirakon.AppManager";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.packageName == null || lpparam.packageName.startsWith(APPSPHERE)) return;
        XposedHelpers.findAndHookMethod(Activity.class, "startActivity", Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Intent intent = (Intent) param.args[0];
                        if (intent == null || intent.getComponent() != null || intent.getPackage() != null) return;
                        String action = intent.getAction();
                        if (!Intent.ACTION_INSTALL_PACKAGE.equals(action)
                                && !Intent.ACTION_VIEW.equals(action)) return;
                        Uri data = intent.getData();
                        if (data == null || (!"content".equals(data.getScheme())
                                && !"file".equals(data.getScheme()))) return;
                        String mime = intent.getType();
                        if (!"application/vnd.android.package-archive".equals(mime)) return;
                        intent.setComponent(new ComponentName(APPSPHERE,
                                APPSPHERE + ".apk.installer.PackageInstallerActivity"));
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                });
    }
}
