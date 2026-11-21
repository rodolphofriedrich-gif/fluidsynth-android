package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.widget.Button;

/** Cria e atualiza itens da navegação inferior sem acoplar a navegação à lógica das telas. */
public final class NavigationTabFactory {
    private static final float LABEL_TEXT_SIZE_SP = 10f;
    private static final float ICON_SIZE_MULTIPLIER = 2.1f;

    private NavigationTabFactory() {}

    public static Button create(Context context, String icon, String label,
            boolean selected, String accessibilityLabel) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setText(createTabText(icon, label), android.widget.TextView.BufferType.SPANNABLE);
        button.setTextSize(LABEL_TEXT_SIZE_SP);
        button.setLineSpacing(AppUi.dp(context, 1), 1f);
        button.setPadding(AppUi.dp(context, 8), AppUi.dp(context, 3),
                AppUi.dp(context, 8), AppUi.dp(context, 3));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setContentDescription(accessibilityLabel);
        update(context, button, selected);
        return button;
    }

    private static SpannableString createTabText(String icon, String label) {
        String text = icon + "\n" + label;
        SpannableString styled = new SpannableString(text);
        int iconEnd = icon.length();
        styled.setSpan(new RelativeSizeSpan(ICON_SIZE_MULTIPLIER),
                0, iconEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new StyleSpan(Typeface.BOLD),
                0, iconEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }

    public static void update(Context context, Button button, boolean selected) {
        button.setSelected(selected);
        button.setTextColor(selected ? Color.WHITE : AppUi.MUTED);
        button.setTypeface(Typeface.DEFAULT,
                selected ? Typeface.BOLD : Typeface.NORMAL);
        button.setBackground(AppUi.ripple(context,
                selected ? Color.argb(58, 57, 139, 255) : Color.TRANSPARENT,
                VisualTheme.RADIUS_CONTROL,
                0));
        button.setElevation(0);
    }
}
