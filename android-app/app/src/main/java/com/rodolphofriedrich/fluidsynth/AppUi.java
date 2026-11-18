package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Componentes visuais compartilhados pelas telas do aplicativo. */
public final class AppUi {
    public static final int BG = Color.rgb(13, 17, 27);
    public static final int PANEL = Color.rgb(23, 30, 45);
    public static final int CONTROL = Color.rgb(35, 45, 64);
    public static final int ACCENT = Color.rgb(79, 140, 255);
    public static final int TEXT = Color.rgb(239, 244, 255);
    public static final int MUTED = Color.rgb(164, 178, 201);
    public static final int BORDER = Color.rgb(42, 54, 75);
    public static final int CONTROL_BORDER = Color.rgb(55, 69, 94);
    public static final int KEY_BORDER = Color.rgb(65, 82, 110);

    private AppUi() {}

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(Context context, int color, int radiusDp, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radiusDp));
        if (strokeColor != 0) shape.setStroke(Math.max(1, dp(context, 1)), strokeColor);
        return shape;
    }

    public static RippleDrawable ripple(Context context, int color, int radiusDp, int strokeColor) {
        ColorStateList rippleColor = ColorStateList.valueOf(Color.argb(70, 255, 255, 255));
        return new RippleDrawable(rippleColor,
                rounded(context, color, radiusDp, strokeColor),
                rounded(context, Color.WHITE, radiusDp, 0));
    }

    public static void styleButton(Context context, Button button, int background,
            int textColor, int heightDp, int textSizeSp) {
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(textSizeSp);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        button.setBackground(ripple(context, background, 12, 0));
        button.setElevation(0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, heightDp));
        params.topMargin = dp(context, 6);
        button.setLayoutParams(params);
    }

    public static void setButtonEnabled(Button button, boolean enabled) {
        if (button == null) return;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.42f);
    }

    public static void tint(SeekBar seekBar) {
        if (seekBar == null) return;
        ColorStateList accent = ColorStateList.valueOf(ACCENT);
        seekBar.setProgressTintList(accent);
        seekBar.setThumbTintList(accent);
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(CONTROL));
    }

    public static void tint(Switch toggle) {
        if (toggle == null) return;
        toggle.setThumbTintList(ColorStateList.valueOf(ACCENT));
        toggle.setTrackTintList(ColorStateList.valueOf(Color.argb(110, 79, 140, 255)));
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
        box.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        box.setBackground(rounded(context, PANEL, 16, BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 12);
        parent.addView(box, params);
        return box;
    }
}
