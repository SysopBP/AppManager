// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.session.SessionMonitoringService;
import io.github.muntashirakon.AppManager.utils.AppPref;

public class PowerPreferences extends PreferenceFragment {
    @Override public int getTitle() { return R.string.sphere_power; }
    @Override public void onCreatePreferences(@Nullable Bundle state, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_power, rootKey);
        SwitchPreferenceCompat session = requirePreference("keep_session");
        session.setChecked(Prefs.Privacy.isPersistentSessionAllowed());
        session.setOnPreferenceChangeListener((pref, value) -> {
            boolean enabled = (Boolean) value;
            AppPref.set(AppPref.PrefKey.PREF_ENABLE_PERSISTENT_SESSION_BOOL, enabled);
            Intent service = new Intent(requireContext(), SessionMonitoringService.class);
            if (enabled) ContextCompat.startForegroundService(requireContext(), service);
            else requireContext().stopService(service);
            return true;
        });
        requirePreference("battery_settings").setOnPreferenceClickListener(p -> {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
            return true;
        });
        requirePreference("privacy_settings").setOnPreferenceClickListener(p -> {
            startActivity(SettingsActivity.getSettingsIntent(requireContext(), "privacy_prefs"));
            return true;
        });
        requirePreference("backend_settings").setOnPreferenceClickListener(p -> {
            startActivity(SettingsActivity.getSettingsIntent(requireContext(), "mode_of_operations"));
            return true;
        });
        updateStatus();
    }
    @Override public void onResume() { super.onResume(); updateStatus(); }
    private void updateStatus() {
        if (getPreferenceScreen() == null) return;
        PowerManager power = (PowerManager) requireContext().getSystemService(android.content.Context.POWER_SERVICE);
        boolean unrestricted = power != null && power.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID);
        requirePreference("battery_status").setSummary(unrestricted
                ? R.string.sphere_power_unrestricted : R.string.sphere_power_optimized);
        requirePreference("backend_settings").setSummary(
                getString(R.string.sphere_power_backend_status, Ops.getMode(), Ops.getInferredMode(requireContext())));
        SwitchPreferenceCompat session = requirePreference("keep_session");
        session.setChecked(Prefs.Privacy.isPersistentSessionAllowed());
    }
}
