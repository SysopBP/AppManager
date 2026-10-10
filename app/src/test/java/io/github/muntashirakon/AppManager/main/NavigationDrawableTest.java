// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.main;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.github.muntashirakon.AppManager.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35}, application = Application.class)
public class NavigationDrawableTest {
    @Test
    public void navigationBackgroundInflatesWithoutTheme() throws Exception {
        Resources resources = RuntimeEnvironment.getApplication().getResources();
        try (XmlResourceParser parser = resources.getXml(R.drawable.appsphere_nav_item)) {
            // Reproduce the theme=null path in the reported Android 17 startup crash.
            Drawable drawable = Drawable.createFromXml(resources, parser, null);
            assertNotNull(drawable);
            assertTrue(drawable instanceof RippleDrawable);
            drawable.setState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected});
            assertNotNull(drawable.getCurrent());
        }
    }
}
