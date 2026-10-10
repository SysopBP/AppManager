// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.utils.appearance.ThemeEngine;

public class ThemePreviewPreference extends Preference {
    public ThemePreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.sphere_theme_preview);
        setSelectable(false);
    }

    @Override public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ThemeEngine.applyScreen(holder.itemView);
    }

    public void refresh() { notifyChanged(); }
}
