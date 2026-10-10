// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import java.io.InputStream;

/** Bounded, asynchronous wallpaper loading. Image effects never change display brightness. */
public class WallpaperView extends AppCompatImageView {
    public static final String PREFS = "appsphere_appearance";
    public static final String URI = "wallpaper_uri";
    public static final String ENABLED = "wallpaper_enabled";
    public static final String DIM = "wallpaper_dim";
    public static final String BRIGHTNESS = "wallpaper_brightness";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private String mUri = "";
    private int mGeneration;
    private int mDim = 35;

    public WallpaperView(Context context) { this(context, null); }

    public WallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.CENTER_CROP);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void refresh() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        mDim = Math.max(0, Math.min(100, prefs.getInt(DIM, 35)));
        float brightness = Math.max(0, Math.min(200, prefs.getInt(BRIGHTNESS, 100))) / 100f;
        ColorMatrix matrix = new ColorMatrix();
        matrix.setScale(brightness, brightness, brightness, 1f);
        setColorFilter(new ColorMatrixColorFilter(matrix));
        invalidate();
        String uri = prefs.getBoolean(ENABLED, false) ? prefs.getString(URI, "") : "";
        if (uri == null) uri = "";
        if (uri.equals(mUri)) return;
        mUri = uri;
        final int generation = ++mGeneration;
        setImageDrawable(null);
        if (uri.isEmpty()) return;
        final Uri source = Uri.parse(uri);
        final Context context = getContext().getApplicationContext();
        new Thread(() -> {
            Bitmap decoded = null;
            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), source),
                            (decoder, info, src) -> {
                                int width = info.getSize().getWidth();
                                int height = info.getSize().getHeight();
                                float scale = Math.min(1f, 1536f / Math.max(width, height));
                                decoder.setTargetSize(Math.max(1, (int) (width * scale)),
                                        Math.max(1, (int) (height * scale)));
                                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                            });
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    try (InputStream stream = context.getContentResolver().openInputStream(source)) {
                        BitmapFactory.decodeStream(stream, null, options);
                    }
                    options.inSampleSize = 1;
                    while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1536) {
                        options.inSampleSize *= 2;
                    }
                    options.inJustDecodeBounds = false;
                    try (InputStream stream = context.getContentResolver().openInputStream(source)) {
                        decoded = BitmapFactory.decodeStream(stream, null, options);
                    }
                }
            } catch (Exception ignored) {
                // An inaccessible or invalid image leaves the normal theme background visible.
            }
            final Bitmap bitmap = decoded;
            mHandler.post(() -> {
                if (generation != mGeneration) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                setImageBitmap(bitmap);
                if (bitmap == null) mUri = ""; // Retry on the next refresh if a provider becomes available.
            });
        }, "Sphere-wallpaper").start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getDrawable() != null) canvas.drawColor(Color.argb(Math.round(mDim * 2.55f), 0, 0, 0));
    }

    @Override
    protected void onDetachedFromWindow() {
        ++mGeneration;
        mUri = "";
        setImageDrawable(null);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh();
    }
}
