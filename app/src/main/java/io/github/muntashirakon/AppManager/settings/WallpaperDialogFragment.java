// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.widgets.WallpaperView;

/** Wallpaper controls save immediately and preview the exact background image effects. */
public class WallpaperDialogFragment extends DialogFragment {
    private WallpaperView mPreview;
    private MaterialSwitch mEnabled;
    private final ActivityResultLauncher<String[]> mPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    prefs().edit().putString(WallpaperView.URI, uri.toString())
                            .putBoolean(WallpaperView.ENABLED, true).apply();
                    if (mEnabled != null) mEnabled.setChecked(true);
                    if (mPreview != null) mPreview.refresh();
                } catch (SecurityException e) {
                    Toast.makeText(requireContext(), R.string.sphere_wallpaper_access_error,
                            Toast.LENGTH_LONG).show();
                }
            });

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(WallpaperView.PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(padding, padding / 2, padding, padding);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(panel);
        mPreview = new WallpaperView(context);
        mPreview.setBackgroundColor(com.google.android.material.color.MaterialColors.getColor(
                panel, com.google.android.material.R.attr.colorSurface));
        panel.addView(mPreview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.round(180 * getResources().getDisplayMetrics().density)));
        TextView description = new TextView(context);
        description.setText(R.string.sphere_wallpaper_description);
        description.setPadding(0, padding / 2, 0, padding / 2);
        panel.addView(description);
        mEnabled = new MaterialSwitch(context);
        mEnabled.setText(R.string.sphere_wallpaper_enabled);
        mEnabled.setChecked(prefs().getBoolean(WallpaperView.ENABLED, false));
        mEnabled.setOnCheckedChangeListener((button, checked) -> {
            prefs().edit().putBoolean(WallpaperView.ENABLED, checked).apply();
            mPreview.refresh();
        });
        panel.addView(mEnabled);
        MaterialButton choose = new MaterialButton(context);
        choose.setText(R.string.sphere_wallpaper_choose);
        choose.setAllCaps(false);
        choose.setOnClickListener(v -> mPicker.launch(new String[]{"image/*"}));
        panel.addView(choose);
        addSlider(panel, WallpaperView.DIM, R.string.sphere_wallpaper_dim, 100, 35);
        addSlider(panel, WallpaperView.BRIGHTNESS, R.string.sphere_wallpaper_brightness, 200, 100);
        mPreview.refresh();
        return new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.sphere_wallpaper_title)
                .setView(scroll)
                .setPositiveButton(R.string.close, null)
                .create();
    }

    private void addSlider(LinearLayout panel, String key, int label, int max, int defaultValue) {
        TextView value = new TextView(requireContext());
        SeekBar slider = new SeekBar(requireContext());
        slider.setMax(max);
        slider.setProgress(Math.max(0, Math.min(max, prefs().getInt(key, defaultValue))));
        value.setText(getString(label, slider.getProgress()));
        slider.setContentDescription(value.getText());
        slider.setMinimumHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(getString(label, progress));
                seekBar.setContentDescription(value.getText());
                if (fromUser) {
                    prefs().edit().putInt(key, progress).apply();
                    mPreview.refresh();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        panel.addView(value);
        panel.addView(slider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onDestroyView() {
        mPreview = null;
        mEnabled = null;
        super.onDestroyView();
    }
}
