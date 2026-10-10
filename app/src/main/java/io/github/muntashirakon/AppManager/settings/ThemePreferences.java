// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.settings;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.utils.appearance.AppearanceUtils;
import io.github.muntashirakon.AppManager.utils.appearance.ThemeEngine;
import io.github.muntashirakon.AppManager.widgets.WallpaperView;

public class ThemePreferences extends PreferenceFragment {
    @Override public void onCreatePreferences(@Nullable Bundle state, @Nullable String rootKey) {
        getPreferenceManager().setSharedPreferencesName(WallpaperView.PREFS);
        setPreferencesFromResource(R.xml.preferences_sphere_theme, rootKey);
        for (String key : new String[]{ThemeEngine.STYLE, ThemeEngine.ACCENT, ThemeEngine.BLUR,
                ThemeEngine.OPACITY, ThemeEngine.REDUCE}) {
            requirePreference(key).setOnPreferenceChangeListener((preference, value) -> {
                // Native preferences persist after the change listener returns.
                if (getView() != null) getView().post(this::updatePreview);
                return true;
            });
        }
        requirePreference("sphere_theme_apply").setOnPreferenceClickListener(preference -> {
            AppearanceUtils.applyConfigurationChangesToActivities();
            return true;
        });
        requirePreference("sphere_theme_reset").setOnPreferenceClickListener(preference -> {
            ThemeEngine.preferences(requireContext()).edit()
                    .remove(ThemeEngine.STYLE).remove(ThemeEngine.ACCENT)
                    .remove(ThemeEngine.BLUR).remove(ThemeEngine.OPACITY).remove(ThemeEngine.REDUCE).apply();
            AppearanceUtils.applyConfigurationChangesToActivities();
            return true;
        });
        requirePreference("sphere_theme_wallpaper").setOnPreferenceClickListener(preference -> {
            new WallpaperDialogFragment().show(getParentFragmentManager(), "sphere_wallpaper");
            return true;
        });
    }

    @Override public void onResume() {
        super.onResume();
        updatePreview();
    }

    private void updatePreview() {
        if (!isAdded()) return;
        ((ThemePreviewPreference) requirePreference("sphere_theme_preview")).refresh();
        ThemeEngine.Config config = ThemeEngine.read(requireContext());
        requirePreference(ThemeEngine.BLUR).setEnabled(config.blurEnabled);
        requirePreference(ThemeEngine.OPACITY).setEnabled(config.blurEnabled);
        Preference status = requirePreference("sphere_theme_rendering");
        if (!config.glass) status.setSummary(R.string.sphere_theme_solid_status);
        else if (ThemeEngine.preferences(requireContext()).getBoolean(ThemeEngine.REDUCE, false)) {
            status.setSummary(R.string.sphere_theme_reduced_status);
        } else if (Build.VERSION.SDK_INT < 31) status.setSummary(R.string.sphere_theme_older_status);
        else {
            PowerManager power = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
            status.setSummary(power != null && power.isPowerSaveMode()
                    ? R.string.sphere_theme_power_status : R.string.sphere_theme_blur_status);
        }
    }

    @Override public int getTitle() { return R.string.sphere_theme_engine; }
}
