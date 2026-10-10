// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.main;
import android.app.Application;
import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.net.Uri;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import io.github.chimio.inxlocker.util.PrefsProvider;
import io.github.chimio.inxlocker.util.InstallerRouting;
import io.github.chimio.inxlocker.util.IntentRedirector;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class InstallerRoutingTest {
 private SharedPreferences prefs;
 @Before public void prepare() {
  prefs = RuntimeEnvironment.getApplication().getSharedPreferences("routing-test", Context.MODE_PRIVATE);
  prefs.edit().clear().putBoolean("routing_enabled", true)
    .putString("selected_installer_package", "custom.installer").commit();
  PrefsProvider.INSTANCE.init(prefs);
 }
 private Intent apk() { return new Intent(Intent.ACTION_VIEW).setDataAndType(
   Uri.parse("content://files/sample.apk"), "application/vnd.android.package-archive"); }
 @Test public void ordinaryViewsAreUnchanged() {
  assertFalse(InstallerRouting.shouldRedirect(new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))));
  assertTrue(InstallerRouting.shouldRedirect(apk()));
 }
 @Test public void disabledOrMissingDestinationDoesNotIntercept() {
  prefs.edit().putBoolean("routing_enabled", false).commit();
  assertFalse(InstallerRouting.shouldRedirect(apk()));
  prefs.edit().putBoolean("routing_enabled", true).remove("selected_installer_package").commit();
  assertFalse(InstallerRouting.shouldRedirect(apk()));
 }
 @Test public void explicitSystemFallbackDoesNotReturnToUnavailableInstaller() {
  assertFalse(InstallerRouting.shouldRedirect(apk().putExtra(
    io.github.chimio.inxlocker.util.IntentAnalyzer.EXTRA_BYPASS_ROUTING, true)));
 }
 @Test public void explicitDestinationsCannotLoop() {
  assertFalse(InstallerRouting.shouldRedirect(apk().setPackage("custom.installer")));
  assertFalse(InstallerRouting.shouldRedirect(apk().setComponent(new ComponentName("custom.installer", "custom.installer.Install"))));
  assertFalse(InstallerRouting.shouldRedirect(apk().setComponent(new ComponentName("other.installer", "other.installer.Install"))));
  prefs.edit().putStringSet("forced_installer_components", java.util.Collections.singleton("other.installer/other.installer.Install")).commit();
  assertTrue(InstallerRouting.shouldRedirect(apk().setComponent(new ComponentName("other.installer", "other.installer.Install"))));
 }
 @Test public void uninstallAndSessionInterceptionRequireOptIn() {
  Intent uninstall = new Intent(Intent.ACTION_DELETE, Uri.parse("package:test.app"));
  Intent session = new Intent("android.content.pm.action.CONFIRM_INSTALL");
  assertFalse(InstallerRouting.shouldRedirect(uninstall));
  assertFalse(InstallerRouting.shouldRedirect(session));
  prefs.edit().putBoolean("intercept_uninstall", true).putBoolean("intercept_session_install", true).commit();
  assertTrue(InstallerRouting.shouldRedirect(uninstall));
  assertTrue(InstallerRouting.shouldRedirect(session));
 }
 @Test public void redirectPreservesFileAccessAndCallerExtras() {
  Intent intent = apk().setAction(Intent.ACTION_INSTALL_PACKAGE);
  intent.putExtra("caller-token", "keep");
  intent.setClipData(ClipData.newRawUri("APK", intent.getData()));
  Uri data = intent.getData();
  IntentRedirector.INSTANCE.redirect(intent, "test");
  assertEquals("custom.installer", intent.getPackage());
  assertEquals(Intent.ACTION_VIEW, intent.getAction());
  assertEquals(data, intent.getData());
  assertEquals(data, intent.getClipData().getItemAt(0).getUri());
  assertEquals("keep", intent.getStringExtra("caller-token"));
  assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
 }
}
