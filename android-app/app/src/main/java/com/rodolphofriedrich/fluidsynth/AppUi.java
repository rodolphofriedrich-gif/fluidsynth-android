package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Componentes visuais compartilhados pelas telas do aplicativo. */
public final class AppUi {
    public static final int BG = Color.rgb(13, 17, 27);
    public static final int PANEL = Color.rgb(23, 30, 45);
    public static final int CONTROL = Color.rgb(35, 45, 64);
    public static final int ACCENT = Color.rgb(79, 140, 255);
    public static final int TEXT = Color.rgb(239, 244, 255);
    public static final int MUTED = Color.rgb(164, 178, 201);

    private AppUi() {}

    public static GradientDrawable rounded(Context context, int color, int radiusDp, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusDp * context.getResources().getDisplayMetrics().density);
        if (strokeColor != 0) {
            shape.setStroke((int) context.getResources().getDisplayMetrics().density, strokeColor);
        }
        return shape;
    }

    public static void styleButton(Context context, Button button, int background,
            int textColor, int heightDp, int textSize) {
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(textSize);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(8, 8, 8, 8);
        button.setBackground(rounded(context, background, 12, 0));
        button.setElevation(0);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (heightDp * context.getResources().getDisplayMetrics().density)));
    }

    public static TextView sectionLabel(Context context, String value) {
        TextView label = new TextView(context);
        label.setText(value);
        label.setTextColor(MUTED);
        label.setTextSize(12);
        label.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        label.setLetterSpacing(0.06f);
        return label;
    }

    public static LinearLayout panel(Context context, LinearLayout parent) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(14, 12, 14, 12);
        box.setBackground(rounded(context, PANEL, 16, Color.rgb(42, 54, 75)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 12;
        parent.addView(box, params);
        return box;
    }
}
