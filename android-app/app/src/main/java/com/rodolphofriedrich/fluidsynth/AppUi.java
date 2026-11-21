package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.view.View;

/**
 * Componentes visuais compartilhados pelas telas.
 *
 * Mantém uma API pequena e estável para as telas existentes; tokens de cor
 * e identidade visual ficam centralizados em VisualTheme.
 */
public final class AppUi {
    public static final int BG = VisualTheme.BACKGROUND;
    public static final int PANEL = VisualTheme.SURFACE;
    public static final int CONTROL = VisualTheme.CONTROL;
    public static final int ACCENT = VisualTheme.ACCENT;
    public static final int TEXT = VisualTheme.TEXT_PRIMARY;
    public static final int MUTED = VisualTheme.TEXT_SECONDARY;
    public static final int BORDER = VisualTheme.BORDER;
    public static final int CONTROL_BORDER = VisualTheme.CONTROL_BORDER;
    public static final int KEY_BORDER = VisualTheme.KEY_BORDER;

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
        ColorStateList rippleColor = ColorStateList.valueOf(Color.argb(58, 255, 255, 255));
        return new RippleDrawable(rippleColor,
                rounded(context, color, radiusDp, strokeColor),
                rounded(context, Color.WHITE, radiusDp, 0));
    }

    public static void styleButton(Context context, Button button, int background,
            int textColor, int heightDp, int textSizeSp) {
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(textSizeSp);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        button.setBackground(ripple(context, background, VisualTheme.RADIUS_CONTROL, 0));
        button.setElevation(dp(context, 1));
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
        toggle.setThumbTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { VisualTheme.ACCENT, VisualTheme.TEXT_SECONDARY }));
        toggle.setTrackTintList(new ColorStateList(
                new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} },
                new int[] { VisualTheme.ACCENT_SOFT, VisualTheme.CONTROL_BORDER }));
    }

    public static TextView sectionLabel(Context context, String value) {
        TextView label = new TextView(context);
        label.setText(value);
        label.setTextColor(MUTED);
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setLetterSpacing(0.08f);
        return label;
    }

    /** Aplica tipografia de leitura a títulos de seção ou conteúdo principal. */
    public static void styleHeading(TextView view) {
        if (view == null) return;
        view.setTextColor(TEXT);
        view.setTextSize(16);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLetterSpacing(0.01f);
    }

    /** Números de mixer e parâmetros usam largura fixa para evitar oscilações visuais. */
    public static void styleValue(TextView view) {
        if (view == null) return;
        view.setTextColor(TEXT);
        view.setTextSize(13);
        view.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        view.setFontFeatureSettings("tnum");
    }

    /** Seção contínua: organiza conteúdo por espaço, sem cartão ou borda fechada. */
    public static LinearLayout flatSection(Context context, LinearLayout parent) {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(context, 12), 0, dp(context, 12));
        section.setBackgroundColor(Color.TRANSPARENT);
        section.setElevation(0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 8);
        parent.addView(section, params);
        return section;
    }

    /** Divisor hairline discreto entre grupos relacionados. */
    public static void addDivider(Context context, LinearLayout parent) {
        View divider = new View(context);
        divider.setBackgroundColor(Color.argb(26, 154, 170, 194));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 1)));
        params.topMargin = dp(context, 8);
        params.bottomMargin = dp(context, 8);
        parent.addView(divider, params);
    }

    public static LinearLayout panel(Context context, LinearLayout parent) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        box.setBackground(rounded(context, PANEL, VisualTheme.RADIUS_CARD, BORDER));
        box.setElevation(dp(context, 2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 12);
        parent.addView(box, params);
        return box;
    }
}
