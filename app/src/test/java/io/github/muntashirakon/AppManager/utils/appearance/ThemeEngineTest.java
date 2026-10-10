// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.utils.appearance;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.PowerManager;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.github.muntashirakon.AppManager.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35}, application = Application.class)
public class ThemeEngineTest {
    private Context context;
    private PowerManager power;

    @Before public void setup() {
        context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(), io.github.muntashirakon.ui.R.style.AppTheme);
        ThemeEngine.preferences(context).edit().clear().commit();
        power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        shadowOf(power).setIsPowerSaveMode(false);
    }

    @Test public void blurRespectsApiAccessibilityAndBatterySaver() {
        assertEquals(Build.VERSION.SDK_INT >= 31, ThemeEngine.read(context).blurEnabled);
        ThemeEngine.preferences(context).edit().putBoolean(ThemeEngine.REDUCE, true).commit();
        assertFalse(ThemeEngine.read(context).blurEnabled);
        ThemeEngine.preferences(context).edit().putBoolean(ThemeEngine.REDUCE, false).commit();
        shadowOf(power).setIsPowerSaveMode(true);
        assertFalse(ThemeEngine.read(context).blurEnabled);
        shadowOf(power).setIsPowerSaveMode(false);
        ThemeEngine.preferences(context).edit().putString(ThemeEngine.STYLE, "material").commit();
        assertFalse(ThemeEngine.read(context).blurEnabled);
    }

    @Test public void persistedEffectsAreBoundedAndUnknownPresetsRecover() {
        ThemeEngine.preferences(context).edit().putInt(ThemeEngine.BLUR, 999)
                .putInt(ThemeEngine.OPACITY, -1).putString(ThemeEngine.STYLE, "obsolete").commit();
        ThemeEngine.Config config = ThemeEngine.read(context);
        assertEquals("glass", config.preset);
        assertEquals(40, config.blur);
        assertEquals(40, config.opacity);
    }

    @Test public void accentOverlaysMatchPreviewAndKeepReadableForegroundInBothModes() {
        for (int mode : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mode;
            for (String accent : new String[]{"violet", "blue", "mint", "amber"}) {
                Context themed = new ContextThemeWrapper(context.createConfigurationContext(configuration), io.github.muntashirakon.ui.R.style.AppTheme);
                ThemeEngine.preferences(themed).edit().putString(ThemeEngine.ACCENT, accent).commit();
                ThemeEngine.applyAccent(themed);
                ThemeEngine.Config config = ThemeEngine.read(themed);
                TypedValue primary = new TypedValue();
                assertTrue(themed.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, primary, true));
                assertEquals(config.accent, primary.data);
                assertTrue(accent + " foreground contrast", ColorUtils.calculateContrast(config.onAccent, config.accent) >= 4.5);
            }
        }
    }

    @Test public void previewAndPreferenceSurfacesInflateAcrossSupportedVersions() {
        View preview = LayoutInflater.from(context).inflate(R.layout.sphere_theme_preview, null);
        ThemeEngine.applyScreen(preview);
        assertNotNull(preview.findViewById(R.id.appsphere_floating_bar));
        assertNotNull(LayoutInflater.from(context).inflate(R.layout.m3_preference, null));
    }
}
