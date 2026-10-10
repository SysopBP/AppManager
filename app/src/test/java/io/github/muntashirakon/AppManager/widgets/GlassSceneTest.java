// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.widgets;

import static org.junit.Assert.*;

import android.app.Application;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import io.github.muntashirakon.AppManager.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GlassSceneTest {
    private GlassCoordinatorLayout root() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        GlassCoordinatorLayout root = new GlassCoordinatorLayout(new ContextThemeWrapper(
                activity, io.github.muntashirakon.ui.R.style.AppTheme), null);
        activity.setContentView(root);
        root.layout(0, 0, 1080, 2400);
        return root;
    }

    @Test public void snapshotContainsEarlierBackdropAndExcludesSurfaceForeground() {
        GlassCoordinatorLayout root = root();
        View card = new View(root.getContext());
        root.addView(card);
        card.layout(120, 360, 540, 560);
        Bitmap[] images = new Bitmap[2];
        root.scene.capture(canvas -> {
            canvas.drawColor(Color.RED);
            images[0] = root.scene.snapshot(card, new Rect(0, 0, card.getWidth(), card.getHeight()), null, new Canvas());
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            canvas.drawRect(card.getLeft(), card.getTop(), card.getRight(), card.getBottom(), paint);
            images[1] = root.scene.snapshot(card, new Rect(0, 0, card.getWidth(), card.getHeight()), null, new Canvas());
        });
        assertFalse(root.scene.hasFailed());
        assertEquals(Color.RED, images[0].getPixel(images[0].getWidth() / 2, images[0].getHeight() / 2));
        assertEquals(Color.BLUE, images[1].getPixel(images[1].getWidth() / 2, images[1].getHeight() / 2));
        // The first snapshot stays independent after later foreground content is drawn.
        assertEquals(Color.RED, images[0].getPixel(images[0].getWidth() / 2, images[0].getHeight() / 2));
    }

    @Test public void reusedBuffersUpdateWhenContentMovesAndRemainBounded() {
        GlassCoordinatorLayout root = root();
        Bitmap[] image = new Bitmap[1];
        Rect bounds = new Rect(0, 0, root.getWidth(), root.getHeight());
        root.scene.capture(canvas -> {
            canvas.drawColor(Color.RED);
            image[0] = root.scene.snapshot(root, bounds, null, new Canvas());
        });
        Bitmap first = image[0];
        root.scene.capture(canvas -> {
            canvas.drawColor(Color.GREEN);
            image[0] = root.scene.snapshot(root, bounds, image[0], new Canvas());
        });
        assertSame(first, image[0]);
        assertTrue(image[0].getWidth() <= 384);
        assertTrue(image[0].getHeight() <= 960);
        assertEquals(Color.GREEN, image[0].getPixel(20, 20));
    }

    @Test public void unsupportedCaptureFallsBackAndAlwaysClearsReentryGuard() {
        GlassCoordinatorLayout root = root();
        root.scene.capture(canvas -> { throw new IllegalArgumentException("hardware-only view"); });
        assertTrue(root.scene.hasFailed());
        assertFalse(root.scene.isCapturing());
        root.scene.release();
        assertFalse(root.scene.hasFailed());
    }
}
