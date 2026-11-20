package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;

/** Cria e atualiza itens da navegação inferior sem acoplar a navegação à lógica das telas. */
public final class NavigationTabFactory {
    private NavigationTabFactory() {}

    public static Button create(Context context, String icon, String label,
            boolean selected, String accessibilityLabel) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setText(icon + "\n" + label);
        button.setTextSize(11);
        button.setPadding(AppUi.dp(context, 8), AppUi.dp(context, 4),
                AppUi.dp(context, 8), AppUi.dp(context, 4));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setContentDescription(accessibilityLabel);
        update(context, button, selected);
        return button;
    }

    public static void update(Context context, Button button, boolean selected) {
        button.setSelected(selected);
        button.setTextColor(selected ? Color.WHITE : AppUi.MUTED);
        button.setTypeface(android.graphics.Typeface.DEFAULT,
                selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        button.setBackground(AppUi.ripple(context,
                selected ? VisualTheme.ACCENT_SOFT : AppUi.PANEL,
                VisualTheme.RADIUS_CONTROL,
                selected ? VisualTheme.ACCENT : AppUi.BORDER));
        button.setElevation(selected ? AppUi.dp(context, 2) : 0);
    }
}
