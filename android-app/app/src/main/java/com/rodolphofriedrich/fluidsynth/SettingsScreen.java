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
import android.widget.Switch;
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
    public final TextView importProgressText;
    public final ProgressBar importProgressBar;
    public final LinearLayout libraryListContainer;

    public SettingsScreen(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 24, 16, 24);
        root.setBackgroundColor(AppUi.BG);

        LinearLayout controllerPanel = AppUi.panel(context, root);
        controllerPanel.addView(AppUi.sectionLabel(context, "CONTROLADOR MIDI"));
        deviceSelector = new Spinner(context);
        deviceSelector.setPadding(4, 4, 4, 4);
        deviceSelector.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Teclado interno do aplicativo"}));
        controllerPanel.addView(deviceSelector, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (52 * context.getResources().getDisplayMetrics().density)));

        usbStatus = new TextView(context);
        usbStatus.setText("MIDI: aguardando controlador");
        usbStatus.setTextColor(AppUi.MUTED);
        usbStatus.setTextSize(12);
        controllerPanel.addView(usbStatus);

        bleStatus = new TextView(context);
        bleStatus.setText("BLE MIDI: busca desligada");
        bleStatus.setTextColor(AppUi.MUTED);
        bleStatus.setTextSize(12);
        bleStatus.setPadding(0, 3, 0, 0);
        controllerPanel.addView(bleStatus);

        LinearLayout audioPanel = AppUi.panel(context, root);
        audioPanel.addView(AppUi.sectionLabel(context, "POLIFONIA"));
        TextView polyphonyHint = new TextView(context);
        polyphonyHint.setText("Limite máximo de vozes simultâneas do sintetizador");
        polyphonyHint.setTextColor(AppUi.MUTED);
        polyphonyHint.setTextSize(12);
        polyphonyHint.setPadding(0, 3, 0, 8);
        audioPanel.addView(polyphonyHint);

        polyphonyValue = new TextView(context);
        polyphonyValue.setText("512 vozes");
        polyphonyValue.setTextColor(AppUi.TEXT);
        polyphonyValue.setTextSize(16);
        polyphonyValue.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        audioPanel.addView(polyphonyValue);

        polyphonySeekBar = new SeekBar(context);
        polyphonySeekBar.setMax(255);
        polyphonySeekBar.setProgress((512 / 8) - 1);
        polyphonySeekBar.setContentDescription("Polifonia, de 8 a 2048 vozes, em passos de 8");
        audioPanel.addView(polyphonySeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (42 * context.getResources().getDisplayMetrics().density)));

        LinearLayout actionsPanel = AppUi.panel(context, root);
        bleSearchButton = new Button(context);
        bleSearchButton.setText("Ativar busca BLE");
        AppUi.styleButton(context, bleSearchButton, AppUi.CONTROL, AppUi.TEXT, 48, 14);
        LinearLayout.LayoutParams bleParams = (LinearLayout.LayoutParams) bleSearchButton.getLayoutParams();
        bleParams.topMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
        bleSearchButton.setLayoutParams(bleParams);
        actionsPanel.addView(bleSearchButton);

        chooseButton = new Button(context);
        chooseButton.setText("＋  Importar SoundFont para a faixa");
        AppUi.styleButton(context, chooseButton, AppUi.ACCENT, Color.WHITE, 54, 14);
        LinearLayout.LayoutParams chooseParams = (LinearLayout.LayoutParams) chooseButton.getLayoutParams();
        chooseParams.topMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
        chooseButton.setLayoutParams(chooseParams);
        actionsPanel.addView(chooseButton);

        importProgressText = new TextView(context);
        importProgressText.setTextColor(AppUi.MUTED);
        importProgressText.setTextSize(12);
        importProgressText.setPadding(0, 8, 0, 2);
        importProgressText.setVisibility(View.GONE);
        actionsPanel.addView(importProgressText);

        importProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setMax(1000);
        importProgressBar.setProgress(0);
        importProgressBar.setIndeterminate(false);
        importProgressBar.setVisibility(View.GONE);
        actionsPanel.addView(importProgressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (8 * context.getResources().getDisplayMetrics().density)));

        LinearLayout librariesPanel = AppUi.panel(context, root);
        librariesPanel.addView(AppUi.sectionLabel(context, "BIBLIOTECAS IMPORTADAS"));
        TextView librariesHint = new TextView(context);
        librariesHint.setText("Desativar oculta a biblioteca ao escolher instrumentos. Canais que já a utilizam não são alterados.");
        librariesHint.setTextColor(AppUi.MUTED);
        librariesHint.setTextSize(12);
        librariesHint.setPadding(0, 3, 0, 8);
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
