package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.graphics.Color;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/** Constrói exclusivamente a interface da aba MIDI. */
public final class MidiScreen {
    public static final int[] NOTES = {60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79};

    public final ScrollView scrollView;
    public final TextView status;
    public final TextView audioDiagnostics;
    public final Button[] trackButtons = new Button[16];
    public final Button selectInstrumentButton;
    public final Button trackEnableButton;
    public final SeekBar velocitySeekBar;
    public final TextView velocityValue;
    public final Button[] keyboardKeys = new Button[NOTES.length];

    public MidiScreen(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 48, 16, 24);
        root.setBackgroundColor(AppUi.BG);

        LinearLayout header = AppUi.panel(context, root);
        TextView title = new TextView(context);
        title.setText("FLUIDSYNTH PLAYER");
        title.setTextColor(AppUi.TEXT);
        title.setTextSize(19);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        header.addView(title);

        status = new TextView(context);
        status.setText("Selecione uma faixa e carregue um SoundFont");
        status.setTextColor(AppUi.MUTED);
        status.setTextSize(13);
        status.setPadding(0, 5, 0, 0);
        header.addView(status);

        audioDiagnostics = new TextView(context);
        audioDiagnostics.setText("Áudio: inicializando saída");
        audioDiagnostics.setTextColor(AppUi.MUTED);
        audioDiagnostics.setTextSize(11);
        audioDiagnostics.setPadding(0, 3, 0, 0);
        header.addView(audioDiagnostics);

        LinearLayout tracksPanel = AppUi.panel(context, root);
        tracksPanel.addView(AppUi.sectionLabel(context, "16 FAIXAS MIDI"));
        TextView trackHint = new TextView(context);
        trackHint.setText("Toque em uma faixa para selecioná-la");
        trackHint.setTextColor(AppUi.MUTED);
        trackHint.setTextSize(12);
        trackHint.setPadding(0, 3, 0, 10);
        tracksPanel.addView(trackHint);

        GridLayout trackGrid = new GridLayout(context);
        trackGrid.setColumnCount(4);
        trackGrid.setRowCount(4);
        for (int i = 0; i < 16; i++) {
            Button trackButton = new Button(context);
            trackButton.setText("CH " + String.format(java.util.Locale.ROOT, "%02d", i + 1) + "\nVAZIA");
            trackButton.setTextSize(11);
            trackButton.setTextColor(AppUi.TEXT);
            trackButton.setAllCaps(false);
            trackButton.setPadding(2, 2, 2, 2);
            trackButton.setMinHeight(0);
            trackButton.setMinimumHeight(0);
            trackButton.setBackground(AppUi.rounded(context, AppUi.CONTROL, 10, Color.rgb(55, 69, 94)));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 4, 1f), GridLayout.spec(i % 4, 1f));
            params.width = 0;
            params.height = (int) (64 * context.getResources().getDisplayMetrics().density);
            params.setMargins(3, 3, 3, 3);
            trackGrid.addView(trackButton, params);
            trackButtons[i] = trackButton;
        }
        tracksPanel.addView(trackGrid);

        LinearLayout channelActionsPanel = AppUi.panel(context, root);
        selectInstrumentButton = new Button(context);
        selectInstrumentButton.setText("ESCOLHER INSTRUMENTO");
        selectInstrumentButton.setContentDescription("Escolher ou trocar o instrumento do canal MIDI ativo");
        AppUi.styleButton(context, selectInstrumentButton, AppUi.CONTROL, AppUi.TEXT, 48, 14);
        channelActionsPanel.addView(selectInstrumentButton);

        trackEnableButton = new Button(context);
        trackEnableButton.setText("FAIXA ATIVA");
        AppUi.styleButton(context, trackEnableButton, AppUi.CONTROL, AppUi.TEXT, 48, 14);
        channelActionsPanel.addView(trackEnableButton);

        LinearLayout keyboardPanel = AppUi.panel(context, root);
        keyboardPanel.addView(AppUi.sectionLabel(context, "TECLADO VIRTUAL"));
        TextView keyboardHint = new TextView(context);
        keyboardHint.setText("O teclado toca na faixa selecionada");
        keyboardHint.setTextColor(AppUi.MUTED);
        keyboardHint.setTextSize(12);
        keyboardHint.setPadding(0, 3, 0, 8);
        keyboardPanel.addView(keyboardHint);

        LinearLayout velocityPanel = AppUi.panel(context, keyboardPanel);
        velocityPanel.addView(AppUi.sectionLabel(context, "VELOCITY"));
        velocityValue = new TextView(context);
        velocityValue.setText("Velocity: 100");
        velocityValue.setTextColor(AppUi.TEXT);
        velocityValue.setTextSize(15);
        velocityValue.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        velocityPanel.addView(velocityValue);

        velocitySeekBar = new SeekBar(context);
        velocitySeekBar.setMax(126);
        velocitySeekBar.setProgress(99);
        velocitySeekBar.setContentDescription("Velocity MIDI, de 1 a 127");
        velocityPanel.addView(velocitySeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (42 * context.getResources().getDisplayMetrics().density)));

        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(context);
            line.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = (int) (6 * context.getResources().getDisplayMetrics().density);
            keyboardPanel.addView(line, rowParams);
            for (int col = 0; col < 6; col++) {
                int index = row * 6 + col;
                int note = NOTES[index];
                Button key = new Button(context);
                key.setText(noteName(note));
                key.setTextSize(12);
                key.setTextColor(AppUi.TEXT);
                key.setAllCaps(false);
                key.setPadding(0, 0, 0, 0);
                key.setMinHeight(0);
                key.setMinimumHeight(0);
                key.setBackground(AppUi.rounded(context, AppUi.CONTROL, 10, Color.rgb(65, 82, 110)));
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0,
                        (int) (66 * context.getResources().getDisplayMetrics().density), 1);
                keyParams.setMargins(2, 2, 2, 2);
                line.addView(key, keyParams);
                keyboardKeys[index] = key;
            }
        }

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.addView(root);
    }

    private static String noteName(int note) {
        return new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}[note % 12]
                + (note / 12 - 1);
    }
}
