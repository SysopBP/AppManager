// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.widgets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import io.github.muntashirakon.AppManager.utils.appearance.ThemeEngine;

/** One bounded backdrop buffer per screen. Capture follows normal view draw order. */
final class GlassScene {
    interface Drawer { void draw(Canvas canvas); }

    final View root;
    ThemeEngine.Config config;
    private Bitmap frame;
    private Canvas frameCanvas;
    private float scale;
    private boolean capturing;
    private boolean failed;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final int[] rootLocation = new int[2];
    private final int[] viewLocation = new int[2];

    GlassScene(View root) {
        this.root = root;
        config = ThemeEngine.read(root.getContext());
    }

    static GlassScene find(View view) {
        View current = view;
        while (current != null) {
            if (current instanceof GlassCoordinatorLayout) {
                return ((GlassCoordinatorLayout) current).scene;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    boolean canCapture(Canvas canvas) {
        return config.blurEnabled && config.blur > 0 && !failed && !capturing && canvas.isHardwareAccelerated();
    }

    boolean isCapturing() { return capturing; }

    void capture(Drawer drawer) {
        if (capturing || root.getWidth() == 0 || root.getHeight() == 0) return;
        scale = Math.min(1f, Math.min(384f / root.getWidth(), 960f / root.getHeight()));
        int width = Math.max(1, Math.round(root.getWidth() * scale));
        int height = Math.max(1, Math.round(root.getHeight() * scale));
        if (frame == null || frame.getWidth() != width || frame.getHeight() != height) {
            frame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            frameCanvas = new Canvas(frame);
        }
        frame.eraseColor(Color.TRANSPARENT);
        root.getLocationInWindow(rootLocation);
        int save = frameCanvas.save();
        frameCanvas.scale(scale, scale);
        capturing = true;
        try {
            drawer.draw(frameCanvas);
        } catch (RuntimeException unsupportedView) {
            // Some third-party views or hardware-only images cannot draw into a bitmap.
            // Fall back to a solid surface rather than crashing or repeatedly retrying.
            failed = true;
        } finally {
            capturing = false;
            frameCanvas.restoreToCount(save);
        }
    }

    Bitmap snapshot(View view, Rect bounds, Bitmap reuse, Canvas canvas) {
        if (!capturing || frame == null) return null;
        int width = Math.max(1, Math.min(frame.getWidth(), (int) Math.ceil(bounds.width() * scale)));
        int height = Math.max(1, Math.min(frame.getHeight(), (int) Math.ceil(bounds.height() * scale)));
        Bitmap result = reuse;
        if (result == null || result.getWidth() != width || result.getHeight() != height) {
            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        result.eraseColor(Color.TRANSPARENT);
        view.getLocationInWindow(viewLocation);
        canvas.setBitmap(result);
        canvas.drawBitmap(frame,
                -(viewLocation[0] - rootLocation[0] + bounds.left) * scale,
                -(viewLocation[1] - rootLocation[1] + bounds.top) * scale, paint);
        return result;
    }

    boolean hasFailed() { return failed; }
    void disableBlur() { failed = true; }

    void release() {
        // Do not recycle buffers that a RenderThread display list may still reference.
        frame = null;
        frameCanvas = null;
        failed = false;
    }
}
