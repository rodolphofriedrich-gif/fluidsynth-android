package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;

/** Constrói exclusivamente a interface da aba Configurações. */
public final class SettingsScreen {
    public final ScrollView scrollView;
    public final Spinner deviceSelector;
    public final TextView usbStatus;
    public final TextView bleStatus;
    public final SeekBar polyphonySeekBar;
    public final TextView polyphonyValue;
    public final Button bleSearchButton;
    public final LinearLayout libraryListContainer;
    public final Button importLibraryButton;
    public final LinearLayout importProgressPanel;
    public final TextView importProgressText;
    public final ProgressBar importProgressBar;

    private static void styleConnectionStatus(Context context, TextView status) {
        status.setTextColor(AppUi.MUTED);
        status.setTextSize(12);
        status.setMaxLines(2);
        status.setPadding(AppUi.dp(context, 10), AppUi.dp(context, 7),
                AppUi.dp(context, 10), AppUi.dp(context, 7));
        status.setBackground(AppUi.rounded(context, AppUi.CONTROL,
                VisualTheme.RADIUS_CONTROL, AppUi.CONTROL_BORDER));
        status.setGravity(android.view.Gravity.CENTER_VERTICAL);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    public SettingsScreen(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(context, 16), AppUi.dp(context, 12), AppUi.dp(context, 16), AppUi.dp(context, 24));
        root.setBackgroundColor(AppUi.BG);

        LinearLayout controllerPanel = AppUi.panel(context, root);
        controllerPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.controller_midi)));
        deviceSelector = new Spinner(context);
        deviceSelector.setPadding(AppUi.dp(context, 4), AppUi.dp(context, 4), AppUi.dp(context, 4), AppUi.dp(context, 4));
        deviceSelector.setContentDescription(context.getString(R.string.device_selector_accessibility));
        deviceSelector.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                new String[]{context.getString(R.string.internal_keyboard)}));
        controllerPanel.addView(deviceSelector, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AppUi.dp(context, 52)));

        usbStatus = new TextView(context);
        usbStatus.setText(context.getString(R.string.midi_waiting));
        styleConnectionStatus(context, usbStatus);
        controllerPanel.addView(usbStatus);

        bleStatus = new TextView(context);
        bleStatus.setText(context.getString(R.string.ble_search_off));
        styleConnectionStatus(context, bleStatus);
        LinearLayout.LayoutParams bleStatusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bleStatusParams.topMargin = AppUi.dp(context, 6);
        controllerPanel.addView(bleStatus, bleStatusParams);

        LinearLayout audioPanel = AppUi.panel(context, root);
        audioPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.polyphony_label)));
        TextView polyphonyHint = new TextView(context);
        polyphonyHint.setText(context.getString(R.string.polyphony_hint));
        polyphonyHint.setTextColor(AppUi.MUTED);
        polyphonyHint.setTextSize(12);
        polyphonyHint.setPadding(0, AppUi.dp(context, 3), 0, AppUi.dp(context, 8));
        audioPanel.addView(polyphonyHint);

        polyphonyValue = new TextView(context);
        polyphonyValue.setText(context.getString(R.string.polyphony_value, 512));
        AppUi.styleHeading(polyphonyValue);
        audioPanel.addView(polyphonyValue);

        polyphonySeekBar = new SeekBar(context);
        polyphonySeekBar.setMax(255);
        polyphonySeekBar.setProgress((512 / 8) - 1);
        polyphonySeekBar.setContentDescription(context.getString(R.string.polyphony_accessibility));
        AppUi.tint(polyphonySeekBar);
        audioPanel.addView(polyphonySeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AppUi.dp(context, 48)));

        LinearLayout actionsPanel = AppUi.panel(context, root);
        actionsPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.midi_connection_actions)));
        bleSearchButton = new Button(context);
        bleSearchButton.setText(context.getString(R.string.ble_search_off));
        bleSearchButton.setContentDescription(context.getString(R.string.ble_search_accessibility_off));
        AppUi.styleButton(context, bleSearchButton, AppUi.CONTROL, AppUi.TEXT, 48, 13);
        actionsPanel.addView(bleSearchButton);

        LinearLayout librariesPanel = AppUi.panel(context, root);
        librariesPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.imported_libraries)));
        TextView librariesHint = new TextView(context);
        librariesHint.setText(context.getString(R.string.library_visibility_hint));
        librariesHint.setTextColor(AppUi.MUTED);
        librariesHint.setTextSize(12);
        librariesHint.setPadding(0, AppUi.dp(context, 3), 0, AppUi.dp(context, 8));
        librariesPanel.addView(librariesHint);

        importLibraryButton = new Button(context);
        importLibraryButton.setText(R.string.import_library);
        importLibraryButton.setContentDescription(context.getString(R.string.import_library));
        AppUi.styleButton(context, importLibraryButton, AppUi.ACCENT, Color.WHITE, 48, 13);
        librariesPanel.addView(importLibraryButton);

        importProgressPanel = AppUi.panel(context, librariesPanel);
        importProgressPanel.setVisibility(View.GONE);
        importProgressText = new TextView(context);
        importProgressText.setTextColor(AppUi.MUTED);
        importProgressText.setTextSize(12);
        importProgressPanel.addView(importProgressText);
        importProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setMax(1000);
        importProgressPanel.addView(importProgressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppUi.dp(context, 8)));

        libraryListContainer = new LinearLayout(context);
        libraryListContainer.setOrientation(LinearLayout.VERTICAL);
        librariesPanel.addView(libraryListContainer);

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.addView(root);
    }
}
