// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.settings;

import android.os.Bundle;
import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.transition.MaterialSharedAxis;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.utils.appearance.AppearanceUtils;
import io.github.muntashirakon.AppManager.utils.appearance.TypefaceUtil;
import io.github.muntashirakon.dialog.SearchableFlagsDialogBuilder;
import io.github.muntashirakon.dialog.SearchableSingleChoiceDialogBuilder;

public class AppearancePreferences extends PreferenceFragment {
    private static final List<Integer> THEME_CONST = Arrays.asList(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES);
    private static final List<Integer> LAYOUT_ORIENTATION_CONST = Arrays.asList(
            View.LAYOUT_DIRECTION_LOCALE,
            View.LAYOUT_DIRECTION_LTR,
            View.LAYOUT_DIRECTION_RTL);

    private int mCurrentTheme;
    private int mCurrentLayoutDirection;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey);
        getPreferenceManager().setPreferenceDataStore(new SettingsDataStore());
        // App theme
        final String[] themes = getResources().getStringArray(R.array.themes);
        mCurrentTheme = Prefs.Appearance.getNightMode();
        Preference appTheme = Objects.requireNonNull(findPreference("app_theme"));
        appTheme.setSummary(themes[THEME_CONST.indexOf(mCurrentTheme)]);
        appTheme.setOnPreferenceClickListener(preference -> {
            new SearchableSingleChoiceDialogBuilder<>(requireActivity(), THEME_CONST, themes)
                    .setTitle(R.string.select_theme)
                    .setSelection(mCurrentTheme)
                    .setPositiveButton(R.string.apply, (dialog, which, selectedTheme) -> {
                        if (selectedTheme != null && selectedTheme != mCurrentTheme) {
                            mCurrentTheme = selectedTheme;
                            Prefs.Appearance.setNightMode(mCurrentTheme);
                            AppCompatDelegate.setDefaultNightMode(mCurrentTheme);
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });
        // AppSphere interface styles are independent of the upstream night-mode preference.
        Preference interfaceStyle = Objects.requireNonNull(findPreference("appsphere_interface_style"));
        String[] styleValues = getResources().getStringArray(R.array.appsphere_style_values);
        String[] styleNames = getResources().getStringArray(R.array.appsphere_style_entries);
        String selectedStyle = requireContext().getSharedPreferences("appsphere_appearance", Context.MODE_PRIVATE)
                .getString("interface_style", "glass");
        int currentStyle = Arrays.asList(styleValues).indexOf(selectedStyle);
        interfaceStyle.setSummary(styleNames[Math.max(0, currentStyle)]);
        interfaceStyle.setOnPreferenceClickListener(preference -> {
            String selected = requireContext().getSharedPreferences("appsphere_appearance", Context.MODE_PRIVATE)
                    .getString("interface_style", "glass");
            int checked = Math.max(0, Arrays.asList(styleValues).indexOf(selected));
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Interface style")
                    .setSingleChoiceItems(styleNames, checked, (dialog, which) -> {
                        requireContext().getSharedPreferences("appsphere_appearance", Context.MODE_PRIVATE)
                                .edit().putString("interface_style", styleValues[which]).apply();
                        interfaceStyle.setSummary(styleNames[which]);
                        AppearanceUtils.applyConfigurationChangesToActivities();
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });
        Preference studio = Objects.requireNonNull(findPreference("appsphere_appearance_studio"));
        studio.setOnPreferenceClickListener(preference -> {
            final String[] labels = {"Enable interface animations", "Enable touch haptics"};
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences(
                    "appsphere_appearance", Context.MODE_PRIVATE);
            final boolean[] checked = {prefs.getBoolean("animations", true),
                    prefs.getBoolean("haptics", true)};
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Appearance Studio")
                    .setMultiChoiceItems(labels, checked, (dialog, which, value) -> {
                        checked[which] = value;
                        prefs.edit().putBoolean(which == 0 ? "animations" : "haptics", value).apply();
                    })
                    .setPositiveButton(R.string.close, (dialog, which) ->
                            AppearanceUtils.applyConfigurationChangesToActivities())
                    .show();
            return true;
        });
        Objects.requireNonNull(findPreference("sphere_wallpaper"))
                .setOnPreferenceClickListener(preference -> {
                    new WallpaperDialogFragment().show(getParentFragmentManager(), "sphere_wallpaper");
                    return true;
                });
        // Black theme/custom theme
        SwitchPreferenceCompat fullBlackTheme = Objects.requireNonNull(findPreference("app_theme_pure_black"));
        fullBlackTheme.setChecked(Prefs.Appearance.isPureBlackTheme());
        fullBlackTheme.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = (boolean) newValue;
            Prefs.Appearance.setPureBlackTheme(enabled);
            AppearanceUtils.applyConfigurationChangesToActivities();
            return true;
        });
        // Black theme/custom theme
        SwitchPreferenceCompat useSystemFontPref = Objects.requireNonNull(findPreference("use_system_font"));
        useSystemFontPref.setChecked(Prefs.Appearance.useSystemFont());
        useSystemFontPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (((boolean) newValue)) {
                // Enable system font
                TypefaceUtil.replaceFontsWithSystem(requireContext());
            } else {
                // Disable system font
                TypefaceUtil.restoreFonts();
            }
            AppearanceUtils.applyConfigurationChangesToActivities();
            return true;
        });
        // Layout orientation
        final String[] layoutOrientations = getResources().getStringArray(R.array.layout_orientations);
        mCurrentLayoutDirection = Prefs.Appearance.getLayoutDirection();
        Preference layoutOrientation = Objects.requireNonNull(findPreference("layout_orientation"));
        layoutOrientation.setSummary(layoutOrientations[LAYOUT_ORIENTATION_CONST.indexOf(mCurrentLayoutDirection)]);
        layoutOrientation.setOnPreferenceClickListener(preference -> {
            new SearchableSingleChoiceDialogBuilder<>(requireActivity(), LAYOUT_ORIENTATION_CONST, layoutOrientations)
                    .setTitle(R.string.pref_layout_direction)
                    .setSelection(mCurrentLayoutDirection)
                    .setPositiveButton(R.string.apply, (dialog, which, selectedLayoutOrientation) -> {
                        mCurrentLayoutDirection = Objects.requireNonNull(selectedLayoutOrientation);
                        Prefs.Appearance.setLayoutDirection(mCurrentLayoutDirection);
                        AppearanceUtils.applyConfigurationChangesToActivities();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });
        // Enable/disable features
        FeatureController fc = FeatureController.getInstance();
        ((Preference) Objects.requireNonNull(findPreference("enabled_features")))
                .setOnPreferenceClickListener(preference -> {
                    new SearchableFlagsDialogBuilder<>(requireActivity(), FeatureController.featureFlags, FeatureController.getFormattedFlagNames(requireActivity()), fc.getFlags())
                            .setTitle(R.string.enable_disable_features)
                            .setOnMultiChoiceClickListener((dialog, which, item, isChecked) ->
                                    fc.modifyState(FeatureController.featureFlags.get(which), isChecked))
                            .setNegativeButton(R.string.close, null)
                            .show();
                    return true;
                });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getTitle() {
        return R.string.pref_cat_appearance;
    }
}

