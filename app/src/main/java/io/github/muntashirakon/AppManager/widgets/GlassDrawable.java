// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.widgets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import io.github.muntashirakon.AppManager.utils.appearance.ThemeEngine;

/** Blurs only the backdrop painted before this surface, leaving its own content sharp. */
public final class GlassDrawable extends Drawable {
    private final View view;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private final RectF bounds = new RectF();
    private final float[] radii = new float[8];
    private Bitmap snapshot;
    private final Canvas snapshotCanvas = new Canvas();
    private BlurRenderer renderer;
    private int alpha = 255;

    public GlassDrawable(View view, float cornerRadius) {
        this.view = view;
        setCorners(cornerRadius, cornerRadius, cornerRadius, cornerRadius);
    }

    public void setCorners(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        radii[0] = radii[1] = topLeft;
        radii[2] = radii[3] = topRight;
        radii[4] = radii[5] = bottomRight;
        radii[6] = radii[7] = bottomLeft;
    }

    @Override public void draw(@NonNull Canvas canvas) {
        GlassScene scene = GlassScene.find(view);
        ThemeEngine.Config config = scene == null ? ThemeEngine.read(view.getContext()) : scene.config;
        bounds.set(getBounds());
        if (bounds.isEmpty()) return;
        clip.reset();
        clip.addRoundRect(bounds, radii, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clip);
        boolean frosted = false;
        if (scene != null && scene.isCapturing()) {
            snapshot = scene.snapshot(view, getBounds(), snapshot, snapshotCanvas);
            if (snapshot != null) {
                // Software capture cannot draw RenderNodes. This is the backdrop for
                // later surfaces; blur is applied separately during the real GPU draw.
                paint.setShader(null);
                paint.setAlpha(alpha);
                canvas.drawBitmap(snapshot, null, bounds, paint);
                frosted = true;
            }
        } else if (scene != null && !scene.hasFailed() && config.blurEnabled && config.blur == 0
                && canvas.isHardwareAccelerated()) {
            // Clear glass needs only an alpha fill over the actual background.
            frosted = true;
        } else if (scene != null && !scene.hasFailed() && config.blurEnabled
                && snapshot != null && canvas.isHardwareAccelerated() && Build.VERSION.SDK_INT >= 31) {
            try {
                if (renderer == null) renderer = new BlurRenderer();
                renderer.draw(canvas, snapshot, bounds, config.blur * view.getResources().getDisplayMetrics().density);
                frosted = true;
            } catch (RuntimeException unsupportedRenderer) {
                scene.disableBlur();
            }
        }
        paint.setShader(null);
        paint.setColor(config.surface);
        paint.setAlpha(frosted ? Math.round(config.opacity * 2.55f * alpha / 255f) : alpha);
        canvas.drawRect(bounds, paint);
        // A subtle highlight and hairline rim make the surface read as glass.
        paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                Color.argb(24 * alpha / 255, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        paint.setAlpha(alpha);
        canvas.drawRect(bounds, paint);
        paint.setShader(null);
        canvas.restoreToCount(save);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(view.getResources().getDisplayMetrics().density);
        paint.setColor(Color.argb(46 * alpha / 255, 255, 255, 255));
        canvas.drawPath(clip, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    public void release() {
        if (renderer != null && Build.VERSION.SDK_INT >= 31) renderer.release();
        renderer = null;
        snapshot = null;
        snapshotCanvas.setBitmap(null);
    }

    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(@Nullable ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

    @RequiresApi(31)
    private static final class BlurRenderer {
        private final RenderNode node = new RenderNode("Sphere glass backdrop");
        private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private float radius = -1f;
        private final RectF target = new RectF();

        void draw(Canvas canvas, Bitmap bitmap, RectF bounds, float blur) {
            int width = Math.max(1, Math.round(bounds.width()));
            int height = Math.max(1, Math.round(bounds.height()));
            node.setPosition(0, 0, width, height);
            Canvas recording = node.beginRecording(width, height);
            target.set(0, 0, width, height);
            recording.drawBitmap(bitmap, null, target, bitmapPaint);
            node.endRecording();
            if (radius != blur) {
                radius = blur;
                node.setRenderEffect(blur > 0 ? RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP) : null);
            }
            int save = canvas.save();
            canvas.translate(bounds.left, bounds.top);
            canvas.drawRenderNode(node);
            canvas.restoreToCount(save);
        }

        void release() { node.discardDisplayList(); }
    }
}
