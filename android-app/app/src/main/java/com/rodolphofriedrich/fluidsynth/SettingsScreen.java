package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
    public final Button chooseButton;
    public final TextView importTargetText;
    public final TextView importProgressText;
    public final ProgressBar importProgressBar;
    public final LinearLayout libraryListContainer;

    public SettingsScreen(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(context, 16), AppUi.dp(context, 12), AppUi.dp(context, 16), AppUi.dp(context, 24));
        root.setBackgroundColor(AppUi.BG);

        LinearLayout controllerPanel = AppUi.panel(context, root);
        controllerPanel.addView(AppUi.sectionLabel(context, "CONTROLADOR MIDI"));
        deviceSelector = new Spinner(context);
        deviceSelector.setPadding(AppUi.dp(context, 4), AppUi.dp(context, 4), AppUi.dp(context, 4), AppUi.dp(context, 4));
        deviceSelector.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                new String[]{context.getString(R.string.internal_keyboard)}));
        controllerPanel.addView(deviceSelector, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AppUi.dp(context, 52)));

        usbStatus = new TextView(context);
        usbStatus.setText(context.getString(R.string.midi_waiting));
        usbStatus.setTextColor(AppUi.MUTED);
        usbStatus.setTextSize(12);
        controllerPanel.addView(usbStatus);

        bleStatus = new TextView(context);
        bleStatus.setText(context.getString(R.string.ble_search_off));
        bleStatus.setTextColor(AppUi.MUTED);
        bleStatus.setTextSize(12);
        bleStatus.setPadding(0, AppUi.dp(context, 3), 0, 0);
        controllerPanel.addView(bleStatus);

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
        polyphonyValue.setTextColor(AppUi.TEXT);
        polyphonyValue.setTextSize(16);
        polyphonyValue.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        audioPanel.addView(polyphonyValue);

        polyphonySeekBar = new SeekBar(context);
        polyphonySeekBar.setMax(255);
        polyphonySeekBar.setProgress((512 / 8) - 1);
        polyphonySeekBar.setContentDescription(context.getString(R.string.polyphony_accessibility));
        AppUi.tint(polyphonySeekBar);
        audioPanel.addView(polyphonySeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AppUi.dp(context, 42)));

        LinearLayout actionsPanel = AppUi.panel(context, root);
        bleSearchButton = new Button(context);
        bleSearchButton.setText(context.getString(R.string.ble_search_off));
        AppUi.styleButton(context, bleSearchButton, AppUi.CONTROL, AppUi.TEXT, 48, 14);
        actionsPanel.addView(bleSearchButton);

        importTargetText = new TextView(context);
        importTargetText.setText(context.getString(R.string.import_target_channel, 1));
        importTargetText.setTextColor(AppUi.MUTED);
        importTargetText.setTextSize(12);
        importTargetText.setPadding(0, AppUi.dp(context, 8), 0, AppUi.dp(context, 4));
        importTargetText.setContentDescription(context.getString(R.string.import_target_accessibility, 1));
        actionsPanel.addView(importTargetText);

        chooseButton = new Button(context);
        chooseButton.setText(context.getString(R.string.import_sf2_channel, 1));
        AppUi.styleButton(context, chooseButton, AppUi.ACCENT, Color.WHITE, 54, 14);
        actionsPanel.addView(chooseButton);

        importProgressText = new TextView(context);
        importProgressText.setTextColor(AppUi.MUTED);
        importProgressText.setTextSize(12);
        importProgressText.setPadding(0, AppUi.dp(context, 8), 0, AppUi.dp(context, 2));
        importProgressText.setVisibility(View.GONE);
        actionsPanel.addView(importProgressText);

        importProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setMax(1000);
        importProgressBar.setProgress(0);
        importProgressBar.setIndeterminate(false);
        importProgressBar.setVisibility(View.GONE);
        actionsPanel.addView(importProgressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AppUi.dp(context, 8)));

        LinearLayout librariesPanel = AppUi.panel(context, root);
        librariesPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.imported_libraries)));
        TextView librariesHint = new TextView(context);
        librariesHint.setText(context.getString(R.string.library_visibility_hint));
        librariesHint.setTextColor(AppUi.MUTED);
        librariesHint.setTextSize(12);
        librariesHint.setPadding(0, AppUi.dp(context, 3), 0, AppUi.dp(context, 8));
        librariesPanel.addView(librariesHint);

        libraryListContainer = new LinearLayout(context);
        libraryListContainer.setOrientation(LinearLayout.VERTICAL);
        librariesPanel.addView(libraryListContainer);

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.addView(root);
    }
}
