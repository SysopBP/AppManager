// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.muntashirakon.AppManager.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;

import io.github.muntashirakon.AppManager.utils.appearance.ThemeEngine;

/** Preserves Material selection, ripple and app-status strokes around a frosted backdrop. */
public class GlassCardView extends MaterialCardView {
    private final GlassDrawable glass;
    private boolean glassStyle;
    private final RectF shapeBounds = new RectF();

    public GlassCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        glass = new GlassDrawable(this, getRadius());
        refreshTheme();
    }

    public void refreshTheme() {
        ThemeEngine.Config config = ThemeEngine.read(getContext());
        glassStyle = config.glass;
        setCardBackgroundColor(glassStyle ? Color.TRANSPARENT : config.surface);
        invalidate();
    }

    @Override public void draw(Canvas canvas) {
        if (glassStyle) {
            glass.setBounds(0, 0, getWidth(), getHeight());
            shapeBounds.set(0, 0, getWidth(), getHeight());
            ShapeAppearanceModel shape = getShapeAppearanceModel();
            glass.setCorners(shape.getTopLeftCornerSize().getCornerSize(shapeBounds),
                    shape.getTopRightCornerSize().getCornerSize(shapeBounds),
                    shape.getBottomRightCornerSize().getCornerSize(shapeBounds),
                    shape.getBottomLeftCornerSize().getCornerSize(shapeBounds));
            glass.draw(canvas);
        }
        super.draw(canvas);
    }

    @Override protected void onDetachedFromWindow() {
        glass.release();
        super.onDetachedFromWindow();
    }
}
