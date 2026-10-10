// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import org.robolectric.RuntimeEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import io.github.muntashirakon.AppManager.BuildConfig;
import io.github.muntashirakon.AppManager.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class SphereReleaseNotesTest {
    @Test
    public void currentReleaseMatchesInstalledVersionAndShowsSphereFeatures() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        Changelog notes = new ChangelogParser(app, R.raw.changelog, BuildConfig.VERSION_CODE).parse();
        ChangelogHeader header = (ChangelogHeader) notes.getChangelogItems().getFirst();
        assertEquals(BuildConfig.VERSION_NAME, header.getVersionName());
        assertEquals(BuildConfig.VERSION_CODE, header.getVersionCode());
        assertEquals(1, notes.getChangelogItems().stream()
                .filter(item -> item instanceof ChangelogHeader).count());
        StringBuilder text = new StringBuilder();
        for (ChangelogItem item : notes.getChangelogItems()) {
            text.append(item.getChangeText()).append('\n');
        }
        assertTrue(text.toString().contains("AMOLED"));
        assertTrue(text.toString().contains("True glass"));
        assertTrue(text.toString().contains("InxLocker"));
        assertTrue(text.toString().contains("Android 17 startup crash"));
    }
}
