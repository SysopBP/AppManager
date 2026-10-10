// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import io.github.muntashirakon.AppManager.utils.appearance.ThemeEngine;

/** Captures only when Android is already drawing a changed frame; there is no polling loop. */
public class GlassCoordinatorLayout extends CoordinatorLayout {
    final GlassScene scene;
    private boolean receiverRegistered;
    private ViewTreeObserver observer;
    private final ViewTreeObserver.OnPreDrawListener backdropUpdates = this::updateBackdrop;
    private final BroadcastReceiver powerChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            refreshTheme();
        }
    };

    public GlassCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        scene = new GlassScene(this);
    }

    public void refreshTheme() {
        scene.config = ThemeEngine.read(getContext());
        ThemeEngine.invalidateGlassSurfaces(this);
        invalidate();
    }

    private boolean updateBackdrop() {
        if (scene.canCapture()) {
            try {
                scene.capture(backdrop -> super.draw(backdrop));
            } catch (OutOfMemoryError memoryPressure) {
                scene.release();
                scene.disableBlur();
            }
            if (scene.hasFailed()) ThemeEngine.invalidateGlassSurfaces(this);
        }
        return true;
    }

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ContextCompat.registerReceiver(getContext(), powerChanges,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
        observer = getViewTreeObserver();
        observer.addOnPreDrawListener(backdropUpdates);
        refreshTheme();
    }

    @Override public void onDetachedFromWindow() {
        if (receiverRegistered) {
            getContext().unregisterReceiver(powerChanges);
            receiverRegistered = false;
        }
        if (observer != null && observer.isAlive()) observer.removeOnPreDrawListener(backdropUpdates);
        observer = null;
        scene.release();
        super.onDetachedFromWindow();
    }
}
