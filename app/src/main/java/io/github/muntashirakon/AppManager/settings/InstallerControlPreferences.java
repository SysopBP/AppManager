// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ClipData;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import io.github.chimio.inxlocker.util.InstallerRouting;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.R;

/** Native controls for the integrated InxLocker engine; no external companion required. */
public class InstallerControlPreferences extends PreferenceFragment {
    private final Runnable stateListener = () -> { if (isAdded()) updateStatus(); };
    @Override public void onStart() { super.onStart(); InstallerRouting.addStateListener(stateListener); updateStatus(); }
    @Override public void onStop() { InstallerRouting.removeStateListener(stateListener); super.onStop(); }
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) launchInstaller(uri, false); });

    @Override
    public void onCreatePreferences(@Nullable Bundle state, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_installer_control, rootKey);
        requirePreference("route_status").setOnPreferenceClickListener(p -> {
            new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.sphere_routing_setup)
                    .setMessage(R.string.sphere_routing_setup_text)
                    .setPositiveButton(android.R.string.ok, null).show();
            return true;
        });
        bindSwitch("routing_enabled", false);
        bindSwitch("intercept_uninstall", false);
        bindSwitch("follow_uninstall_with_installer", true);
        bindSwitch("intercept_session_install", false);
        bindSwitch("fix_permissions", false);
        bindSwitch("bypass_coloros_adb_intercept", false);
        bindSwitch("enable_debug_log", false);
        SwitchPreferenceCompat hide = requirePreference("hide_launcher_icon");
        hide.setChecked(InstallerRouting.getBoolean("hide_launcher_icon", false));
        hide.setOnPreferenceChangeListener((p, value) -> {
            try {
                setLauncherVisible(!(Boolean) value);
                InstallerRouting.putBoolean(p.getKey(), (Boolean) value);
                return true;
            } catch (RuntimeException e) {
                showMessage(e.getMessage());
                return false;
            }
        });
        requirePreference("selected_installer_package").setOnPreferenceClickListener(p -> { choose(false); return true; });
        requirePreference("selected_uninstaller_package").setOnPreferenceClickListener(p -> { choose(true); return true; });
        requirePreference("forced_activity").setOnPreferenceClickListener(p -> { chooseForcedActivity(); return true; });
        requirePreference("forced_sources").setOnPreferenceClickListener(p -> { chooseForcedSources(); return true; });
        requirePreference("install_apk").setOnPreferenceClickListener(p -> {
            picker.launch(new String[]{"application/vnd.android.package-archive", "application/octet-stream"});
            return true;
        });
        requirePreference("hot_reload").setOnPreferenceClickListener(p -> {
            p.setEnabled(false);
            InstallerRouting.reload(message -> {
                if (!isAdded()) return;
                p.setEnabled(InstallerRouting.supportsHotReload());
                showMessage(message);
            });
            return true;
        });
        requirePreference("reset_routes").setOnPreferenceClickListener(p -> {
            new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.sphere_routing_reset)
                    .setMessage(R.string.sphere_routing_reset_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.sphere_reset, (dialog, which) -> {
                        setLauncherVisible(true);
                        InstallerRouting.reset();
                        updateStatus();
                    }).show();
            return true;
        });
        requirePreference("inxlocker_credit").setOnPreferenceClickListener(p -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Chimioo/InxLocker"))); }
            catch (android.content.ActivityNotFoundException e) { showMessage(getString(R.string.sphere_routing_credit_summary)); }
            return true;
        });
        updateStatus();
    }

    private void bindSwitch(String key, boolean fallback) {
        SwitchPreferenceCompat pref = requirePreference(key);
        pref.setChecked(InstallerRouting.getBoolean(key, fallback));
        pref.setOnPreferenceChangeListener((p, value) -> {
            InstallerRouting.putBoolean(key, (Boolean) value);
            // The preference framework updates its checked state after this callback returns.
            requirePreference("selected_uninstaller_package").setEnabled(
                    !"follow_uninstall_with_installer".equals(key) ? !InstallerRouting.getBoolean("follow_uninstall_with_installer", true) : !(Boolean) value);
            return true;
        });
    }

    @Override public void onResume() { super.onResume(); updateStatus(); }
    @Override public int getTitle() { return R.string.sphere_installer_control; }

    private void updateStatus() {
        if (getPreferenceScreen() == null) return;
        boolean active = InstallerRouting.isActive();
        Preference status = requirePreference("route_status");
        status.setTitle(active ? R.string.sphere_routing_connected : R.string.sphere_routing_inactive);
        status.setSummary(active ? R.string.sphere_routing_connected_summary : R.string.sphere_routing_inactive_summary);
        for (String key : new String[]{"routing_enabled", "intercept_uninstall", "intercept_session_install",
                "fix_permissions", "bypass_coloros_adb_intercept"}) requirePreference(key).setEnabled(active);
        for (String key : new String[]{"routing_enabled", "intercept_uninstall", "follow_uninstall_with_installer",
                "intercept_session_install", "fix_permissions", "bypass_coloros_adb_intercept", "enable_debug_log", "hide_launcher_icon"}) {
            SwitchPreferenceCompat pref = requirePreference(key);
            boolean fallback = "follow_uninstall_with_installer".equals(key);
            pref.setChecked(InstallerRouting.getBoolean(key, fallback));
        }
        requirePreference("hot_reload").setEnabled(InstallerRouting.supportsHotReload());
        requirePreference("selected_installer_package").setSummary(label(InstallerRouting.getString("selected_installer_package", "")));
        requirePreference("selected_uninstaller_package").setSummary(label(InstallerRouting.getString("selected_uninstaller_package", "")));
        requirePreference("selected_uninstaller_package").setEnabled(!InstallerRouting.getBoolean("follow_uninstall_with_installer", true));
        String target = InstallerRouting.getString("selected_installer_package", "");
        String forced = InstallerRouting.getForcedComponent(target);
        requirePreference("forced_activity").setEnabled(!target.isEmpty());
        requirePreference("forced_activity").setSummary(forced.isEmpty() ? getString(R.string.sphere_routing_auto_activity) : forced);
    }

    private String label(String pkg) {
        if (pkg.isEmpty()) return getString(R.string.sphere_routing_system_default);
        try {
            PackageManager pm = requireContext().getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)) + "\n" + pkg;
        } catch (PackageManager.NameNotFoundException e) { return pkg + "\n" + getString(R.string.sphere_routing_missing); }
    }

    private List<ResolveInfo> candidates(boolean uninstall) {
        Intent intent = new Intent(uninstall ? Intent.ACTION_DELETE : Intent.ACTION_VIEW);
        if (uninstall) intent.setData(Uri.parse("package:example.package"));
        else intent.setDataAndType(Uri.parse("content://sphere.invalid/sample.apk"), "application/vnd.android.package-archive");
        List<ResolveInfo> infos = requireContext().getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        List<ResolveInfo> result = new ArrayList<>();
        for (ResolveInfo info : infos) if (info.activityInfo != null && info.activityInfo.exported && info.activityInfo.enabled) result.add(info);
        return result;
    }

    private void choose(boolean uninstall) {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("", getString(R.string.sphere_routing_system_default));
        List<ResolveInfo> infos = candidates(uninstall);
        infos.sort((a, b) -> a.loadLabel(requireContext().getPackageManager()).toString()
                .compareToIgnoreCase(b.loadLabel(requireContext().getPackageManager()).toString()));
        for (ResolveInfo info : infos) items.put(info.activityInfo.packageName, label(info.activityInfo.packageName));
        List<String> packages = new ArrayList<>(items.keySet());
        String key = uninstall ? "selected_uninstaller_package" : "selected_installer_package";
        String selected = InstallerRouting.getString(key, "");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(uninstall ? R.string.sphere_routing_uninstaller : R.string.sphere_routing_installer)
                .setSingleChoiceItems(items.values().toArray(new String[0]), packages.indexOf(selected), (dialog, which) -> {
                    InstallerRouting.putString(key, packages.get(which));
                    updateStatus();
                    dialog.dismiss();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void chooseForcedActivity() {
        String target = InstallerRouting.getString("selected_installer_package", "");
        List<String> classes = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        classes.add(""); labels.add(getString(R.string.sphere_routing_auto_activity));
        try {
            ActivityInfo[] activities = requireContext().getPackageManager()
                    .getPackageInfo(target, PackageManager.GET_ACTIVITIES).activities;
            if (activities != null) for (ActivityInfo info : activities) {
                if (info.exported && info.enabled) { classes.add(info.name); labels.add(info.name); }
            }
        } catch (PackageManager.NameNotFoundException e) { showMessage(getString(R.string.sphere_routing_missing)); return; }
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.sphere_routing_activity)
                .setSingleChoiceItems(labels.toArray(new String[0]), classes.indexOf(InstallerRouting.getForcedComponent(target)), (dialog, which) -> {
                    InstallerRouting.setForcedComponent(target, classes.get(which));
                    updateStatus(); dialog.dismiss();
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void chooseForcedSources() {
        Set<String> selected = new HashSet<>(InstallerRouting.getForcedComponents());
        Map<String, String> entries = new LinkedHashMap<>();
        for (ResolveInfo info : candidates(false)) {
            String component = info.activityInfo.packageName + "/" + info.activityInfo.name;
            entries.put(component, info.loadLabel(requireContext().getPackageManager()) + "\n" + component);
        }
        for (String component : selected) entries.putIfAbsent(component, component);
        List<String> keys = new ArrayList<>(entries.keySet());
        boolean[] checked = new boolean[keys.size()];
        for (int i = 0; i < keys.size(); ++i) checked[i] = selected.contains(keys.get(i));
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.sphere_routing_sources)
                .setMultiChoiceItems(entries.values().toArray(new String[0]), checked, (dialog, which, enabled) -> {
                    if (enabled) selected.add(keys.get(which)); else selected.remove(keys.get(which));
                }).setPositiveButton(R.string.apply, (dialog, which) -> InstallerRouting.setForcedComponents(selected))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void setLauncherVisible(boolean visible) {
        requireContext().getPackageManager().setComponentEnabledSetting(
                new ComponentName(BuildConfig.APPLICATION_ID, "io.github.muntashirakon.AppManager.SphereLauncher"),
                visible ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void launchInstaller(Uri uri, boolean system) {
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive");
        intent.setClipData(ClipData.newRawUri("APK", uri));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String target = system ? "" : InstallerRouting.getString("selected_installer_package", "");
        if (!target.isEmpty()) {
            String activity = InstallerRouting.getForcedComponent(target);
            if (activity.isEmpty()) intent.setPackage(target);
            else intent.setComponent(new ComponentName(target, activity));
        }
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException | SecurityException e) {
            new MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.sphere_routing_launch_failed)
                    .setPositiveButton(R.string.sphere_routing_use_system, (dialog, which) -> {
                        if (!system) launchInstaller(uri, true);
                    }).setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private void showMessage(String message) {
        if (!isAdded() || !isResumed() || requireActivity().isFinishing()) return;
        new MaterialAlertDialogBuilder(requireContext()).setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }
}
