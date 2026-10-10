// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.utils.appearance;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.appbar.MaterialToolbar;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.widgets.GlassCardView;
import io.github.muntashirakon.AppManager.widgets.GlassCoordinatorLayout;
import io.github.muntashirakon.AppManager.widgets.GlassDrawable;
import io.github.muntashirakon.AppManager.widgets.WallpaperView;

/** Shared presets, semantic colors and rendering policy; night mode stays user-controlled. */
public final class ThemeEngine {
    public static final String STYLE = "interface_style";
    public static final String ACCENT = "theme_accent";
    public static final String BLUR = "glass_blur";
    public static final String OPACITY = "glass_opacity";
    public static final String REDUCE = "reduce_transparency";

    private ThemeEngine() {}

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(WallpaperView.PREFS, Context.MODE_PRIVATE);
    }

    public static final class Config {
        public final String preset;
        public final boolean glass;
        public final boolean blurEnabled;
        public final int blur;
        public final int opacity;
        public final int surface;
        public final int foreground;
        public final int accent;
        public final int onAccent;

        private Config(Context context, SharedPreferences prefs) {
            String selected = prefs.getString(STYLE, "glass");
            preset = "material".equals(selected) || "miuix".equals(selected) ? selected : "glass";
            glass = "glass".equals(preset);
            blur = clamp(prefs.getInt(BLUR, 18), 0, 40);
            opacity = clamp(prefs.getInt(OPACITY, 64), 40, 90);
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            blurEnabled = glass && Build.VERSION.SDK_INT >= 31 && !prefs.getBoolean(REDUCE, false)
                    && (power == null || !power.isPowerSaveMode());
            surface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.rgb(28, 27, 34));
            foreground = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
            int[] colors = accentResources(prefs.getString(ACCENT, "system"));
            accent = colors == null ? MaterialColors.getColor(context,
                    com.google.android.material.R.attr.colorPrimary, Color.rgb(111, 77, 198)) : context.getColor(colors[0]);
            onAccent = colors == null ? MaterialColors.getColor(context,
                    com.google.android.material.R.attr.colorOnPrimary, Color.WHITE) : context.getColor(colors[1]);
        }
    }

    public static Config read(Context context) { return new Config(context, preferences(context)); }

    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static int[] accentResources(String name) {
        if ("violet".equals(name)) return new int[]{R.color.sphere_violet_primary, R.color.sphere_violet_on_primary};
        if ("blue".equals(name)) return new int[]{R.color.sphere_blue_primary, R.color.sphere_blue_on_primary};
        if ("mint".equals(name)) return new int[]{R.color.sphere_mint_primary, R.color.sphere_mint_on_primary};
        if ("amber".equals(name)) return new int[]{R.color.sphere_amber_primary, R.color.sphere_amber_on_primary};
        return null;
    }

    public static void applyAccent(Context context) {
        String accent = preferences(context).getString(ACCENT, "system");
        int style = 0;
        if ("violet".equals(accent)) style = R.style.SphereAccent_Violet;
        else if ("blue".equals(accent)) style = R.style.SphereAccent_Blue;
        else if ("mint".equals(accent)) style = R.style.SphereAccent_Mint;
        else if ("amber".equals(accent)) style = R.style.SphereAccent_Amber;
        if (style != 0) context.getTheme().applyStyle(style, true);
    }

    public static void applyScreen(View root) {
        Config config = read(root.getContext());
        if (root instanceof GlassCoordinatorLayout) ((GlassCoordinatorLayout) root).refreshTheme();
        if (config.glass) {
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{config.surface, ColorUtils.blendARGB(config.surface, config.accent, 0.24f), config.surface}));
        } else root.setBackgroundColor(config.surface);
        View wallpaper = root.findViewById(R.id.sphere_wallpaper);
        if (wallpaper instanceof WallpaperView) ((WallpaperView) wallpaper).refresh();
        applyToolbar(root.findViewById(R.id.toolbar), config);
        applyToolbar(root.findViewById(R.id.toolbar2), config);
        View appbar = root.findViewById(R.id.appbar);
        if (appbar != null) {
            appbar.setBackgroundColor(Color.TRANSPARENT);
            appbar.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            appbar.setElevation(0f);
        }
        View secondAppbar = root.findViewById(R.id.appbar2);
        if (secondAppbar != null) secondAppbar.setBackgroundColor(Color.TRANSPARENT);
        View dashboard = root.findViewById(R.id.appsphere_dashboard);
        if (dashboard != null) applyDashboard(dashboard, config);
        View bar = root.findViewById(R.id.appsphere_floating_bar);
        if (bar != null) {
            applySurface(bar, config, 28f);
            updateNavigation(root);
        }
        refreshCards(root);
    }

    private static void applyToolbar(View toolbar, Config config) {
        if (toolbar == null) return;
        applySurface(toolbar, config, 0);
        if (toolbar instanceof MaterialToolbar) {
            ((MaterialToolbar) toolbar).setTitleTextColor(config.foreground);
            ((MaterialToolbar) toolbar).setNavigationIconTint(config.foreground);
        }
    }

    private static void applySurface(View view, Config config, float radiusDp) {
        view.setBackgroundTintList(null);
        float radius = radiusDp * view.getResources().getDisplayMetrics().density;
        if (config.glass) view.setBackground(new GlassDrawable(view, radius));
        else {
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(radius);
            background.setColor("miuix".equals(config.preset)
                    ? ColorUtils.blendARGB(config.surface, config.accent, 0.12f) : config.surface);
            view.setBackground(background);
        }
    }

    private static void applyDashboard(View view, Config config) {
        if (view instanceof MaterialButton) {
            ((MaterialButton) view).setBackgroundTintList(ColorStateList.valueOf(config.accent));
            ((MaterialButton) view).setTextColor(config.onAccent);
        } else {
            if (view instanceof TextView) ((TextView) view).setTextColor(config.foreground);
            if (view.getBackground() != null) applySurface(view, config, 24);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyDashboard(group.getChildAt(i), config);
        }
    }

    private static void refreshCards(View view) {
        if (view instanceof GlassCardView) ((GlassCardView) view).refreshTheme();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) refreshCards(group.getChildAt(i));
        }
    }

    public static void updateNavigation(View root) {
        Config config = read(root.getContext());
        for (int id : new int[]{R.id.appsphere_nav_home, R.id.appsphere_nav_apps,
                R.id.appsphere_nav_installer, R.id.appsphere_nav_running, R.id.appsphere_nav_settings}) {
            TextView item = root.findViewById(id);
            if (item == null) continue;
            int color = item.isSelected() ? config.accent : config.foreground;
            item.setTextColor(color);
            item.setCompoundDrawableTintList(ColorStateList.valueOf(color));
        }
    }
}
