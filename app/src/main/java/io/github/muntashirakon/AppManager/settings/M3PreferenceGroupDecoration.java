package io.github.muntashirakon.AppManager.settings;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.Color;
import android.view.View;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import io.github.muntashirakon.util.UiUtils;

@SuppressLint("RestrictedApi")
public class M3PreferenceGroupDecoration extends RecyclerView.ItemDecoration {
    @Px
    private final float mRadius;
    @Px
    private final float mRadiusInner;

    public M3PreferenceGroupDecoration(@Px float radius, @Px float innerRadius) {
        mRadius = radius;
        mRadiusInner = innerRadius;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (!(view instanceof MaterialCardView)) {
            return;
        }

        MaterialCardView cardView = (MaterialCardView) view;
        RecyclerView.Adapter<?> rawAdapter = parent.getAdapter();
        if (!(rawAdapter instanceof PreferenceGroupAdapter)) {
            return;
        }

        PreferenceGroupAdapter adapter = (PreferenceGroupAdapter) rawAdapter;
        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;

        // Skip category header
        if (isCategoryHeader(adapter, position)) {
            return;
        }

        // Restrained AppSphere settings surface: charcoal rows, gentle border,
        // and a narrow gap so grouped preferences remain visually distinct.
        int gap = Math.round(parent.getResources().getDisplayMetrics().density * 2);
        outRect.left = Math.round(parent.getResources().getDisplayMetrics().density * 16);
        outRect.right = outRect.left;
        outRect.top = isFirstInGroup(adapter, position) ? gap * 3 : 0;
        outRect.bottom = isLastInGroup(adapter, position) ? gap * 5 : gap;
        if (cardView instanceof io.github.muntashirakon.AppManager.widgets.GlassCardView) {
            ((io.github.muntashirakon.AppManager.widgets.GlassCardView) cardView).refreshTheme();
        } else {
            cardView.setCardBackgroundColor(com.google.android.material.color.MaterialColors.getColor(parent,
                    com.google.android.material.R.attr.colorSurfaceContainerLow));
        }
        cardView.setStrokeColor(ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(parent,
                com.google.android.material.R.attr.colorOutlineVariant)));
        cardView.setStrokeWidth(Math.max(1, Math.round(parent.getResources().getDisplayMetrics().density * 0.6f)));
        cardView.setCardElevation(0f);

        boolean isFirst = isFirstInGroup(adapter, position);
        boolean isLast = isLastInGroup(adapter, position);

        if (isFirst && isLast) {
            // Standalone preference
            UiUtils.setCardRadius(cardView, mRadius, mRadius);
        } else if (isFirst) {
            // Top of a preference group
            UiUtils.setCardRadius(cardView, mRadius, mRadiusInner);
        } else if (isLast) {
            // Bottom of a preference group
            UiUtils.setCardRadius(cardView, mRadiusInner, mRadius);
        } else {
            // Middle of a preference group
            UiUtils.setCardRadius(cardView, mRadiusInner, mRadiusInner);
        }
    }

    private boolean isCategoryHeader(@NonNull PreferenceGroupAdapter adapter, int position) {
        return adapter.getItem(position) instanceof PreferenceCategory;
    }

    private boolean isFirstInGroup(@NonNull PreferenceGroupAdapter adapter, int position) {
        if (position == 0) return true;

        // If the element right above this choice is a section separator, this is the top card.
        return isCategoryHeader(adapter, position - 1);
    }

    private boolean isLastInGroup(@NonNull PreferenceGroupAdapter adapter, int position) {
        if (position == adapter.getItemCount() - 1) return true;

        // If the element right below this card is a header separator, this is the bottom card.
        return isCategoryHeader(adapter, position + 1);
    }
}