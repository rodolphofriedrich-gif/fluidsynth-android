package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.view.View;

/** Constrói exclusivamente a interface da aba MIDI. */
public final class MidiScreen {
    public static final int[] NOTES = {60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79};

    public final ScrollView scrollView;
    public final TextView status;
    public final TextView audioDiagnostics;
    public final Button[] trackButtons = new Button[16];
    public final Button selectInstrumentButton;
    public final Button trackEnableButton;
    public final Button soloButton;
    public final SeekBar masterVolumeSeekBar;
    public final TextView masterVolumeValue;
    public final TextView midiActivity;
    public final LinearLayout importProgressPanel;
    public final TextView importProgressText;
    public final ProgressBar importProgressBar;
    public final SeekBar velocitySeekBar;
    public final TextView velocityValue;
    public final Button[] keyboardKeys = new Button[NOTES.length];

    public MidiScreen(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(context, 16), AppUi.dp(context, 12), AppUi.dp(context, 16), AppUi.dp(context, 24));
        root.setBackgroundColor(AppUi.BG);

        LinearLayout header = AppUi.panel(context, root);
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText(context.getString(R.string.app_title));
        title.setTextColor(AppUi.TEXT);
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        headerRow.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        audioDiagnostics = new TextView(context);
        audioDiagnostics.setText(context.getString(R.string.audio_initializing));
        audioDiagnostics.setTextColor(AppUi.MUTED);
        audioDiagnostics.setTextSize(11);
        midiActivity = new TextView(context);
        midiActivity.setText(context.getString(R.string.midi_activity_waiting));
        midiActivity.setTextColor(AppUi.MUTED);
        midiActivity.setTextSize(11);
        midiActivity.setPadding(AppUi.dp(context, 6), 0, AppUi.dp(context, 6), 0);
        headerRow.addView(midiActivity);
        headerRow.addView(audioDiagnostics);
        header.addView(headerRow);

        status = new TextView(context);
        status.setText(context.getString(R.string.status_select_track));
        status.setTextColor(AppUi.MUTED);
        status.setTextSize(12);
        status.setPadding(0, AppUi.dp(context, 3), 0, 0);
        header.addView(status);

        LinearLayout tracksPanel = AppUi.panel(context, root);
        tracksPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.tracks_title)));
        TextView trackHint = new TextView(context);
        trackHint.setText(context.getString(R.string.track_hint));
        trackHint.setTextColor(AppUi.MUTED);
        trackHint.setTextSize(12);
        trackHint.setPadding(0, AppUi.dp(context, 3), 0, AppUi.dp(context, 10));
        tracksPanel.addView(trackHint);

        GridLayout trackGrid = new GridLayout(context);
        trackGrid.setColumnCount(4);
        trackGrid.setRowCount(4);
        for (int i = 0; i < 16; i++) {
            Button trackButton = new Button(context);
            trackButton.setText(context.getString(R.string.track_channel_empty, i + 1));
            trackButton.setTextSize(12);
            trackButton.setTextColor(AppUi.TEXT);
            trackButton.setAllCaps(false);
            trackButton.setPadding(AppUi.dp(context, 2), AppUi.dp(context, 2), AppUi.dp(context, 2), AppUi.dp(context, 2));
            trackButton.setMinHeight(0);
            trackButton.setMinimumHeight(0);
            trackButton.setBackground(AppUi.ripple(context, AppUi.CONTROL, 10, AppUi.CONTROL_BORDER));
            trackButton.setContentDescription(context.getString(R.string.track_accessibility, i + 1));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 4, 1f), GridLayout.spec(i % 4, 1f));
            params.width = 0;
            params.height = AppUi.dp(context, 64);
            params.setMargins(AppUi.dp(context, 3), AppUi.dp(context, 3), AppUi.dp(context, 3), AppUi.dp(context, 3));
            trackGrid.addView(trackButton, params);
            trackButtons[i] = trackButton;
        }
        tracksPanel.addView(trackGrid);

        importProgressPanel = AppUi.panel(context, root);
        importProgressPanel.setVisibility(View.GONE);
        importProgressPanel.addView(AppUi.sectionLabel(context,
                context.getString(R.string.import_progress_title)));
        importProgressText = new TextView(context);
        importProgressText.setTextColor(AppUi.MUTED);
        importProgressText.setTextSize(12);
        importProgressPanel.addView(importProgressText);
        importProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setMax(1000);
        importProgressBar.setProgress(0);
        importProgressBar.setIndeterminate(false);
        importProgressPanel.addView(importProgressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppUi.dp(context, 8)));

        LinearLayout channelActionsPanel = AppUi.panel(context, root);
        selectInstrumentButton = new Button(context);
        selectInstrumentButton.setText(context.getString(R.string.choose_instrument));
        selectInstrumentButton.setContentDescription(context.getString(R.string.choose_instrument_accessibility));
        AppUi.styleButton(context, selectInstrumentButton, AppUi.CONTROL, AppUi.TEXT, 48, 14);
        channelActionsPanel.addView(selectInstrumentButton);

        LinearLayout channelControlRow = new LinearLayout(context);
        channelControlRow.setOrientation(LinearLayout.HORIZONTAL);
        trackEnableButton = new Button(context);
        trackEnableButton.setText(context.getString(R.string.track_active));
        AppUi.styleButton(context, trackEnableButton, AppUi.CONTROL, AppUi.TEXT, 48, 13);
        LinearLayout.LayoutParams halfButton = new LinearLayout.LayoutParams(0,
                AppUi.dp(context, 48), 1f);
        halfButton.setMargins(AppUi.dp(context, 3), AppUi.dp(context, 6), AppUi.dp(context, 3), 0);
        channelControlRow.addView(trackEnableButton, halfButton);
        soloButton = new Button(context);
        soloButton.setText(context.getString(R.string.solo_channel));
        AppUi.styleButton(context, soloButton, AppUi.CONTROL, AppUi.TEXT, 48, 13);
        LinearLayout.LayoutParams soloParams = new LinearLayout.LayoutParams(0,
                AppUi.dp(context, 48), 1f);
        soloParams.setMargins(AppUi.dp(context, 3), AppUi.dp(context, 6), AppUi.dp(context, 3), 0);
        channelControlRow.addView(soloButton, soloParams);
        channelActionsPanel.addView(channelControlRow);

        LinearLayout masterPanel = AppUi.panel(context, root);
        masterPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.master_volume_label)));
        masterVolumeValue = new TextView(context);
        masterVolumeValue.setText(context.getString(R.string.master_volume_value, 100));
        masterVolumeValue.setTextColor(AppUi.TEXT);
        masterVolumeValue.setTextSize(14);
        masterPanel.addView(masterVolumeValue);
        masterVolumeSeekBar = new SeekBar(context);
        masterVolumeSeekBar.setMax(100);
        masterVolumeSeekBar.setProgress(100);
        masterVolumeSeekBar.setContentDescription(context.getString(R.string.master_volume_accessibility));
        AppUi.tint(masterVolumeSeekBar);
        masterPanel.addView(masterVolumeSeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppUi.dp(context, 42)));

        LinearLayout keyboardPanel = AppUi.panel(context, root);
        keyboardPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.virtual_keyboard)));
        TextView keyboardHint = new TextView(context);
        keyboardHint.setText(context.getString(R.string.keyboard_hint));
        keyboardHint.setTextColor(AppUi.MUTED);
        keyboardHint.setTextSize(12);
        keyboardHint.setPadding(0, AppUi.dp(context, 3), 0, AppUi.dp(context, 8));
        keyboardPanel.addView(keyboardHint);

        keyboardPanel.addView(AppUi.sectionLabel(context, context.getString(R.string.velocity_label)));
        velocityValue = new TextView(context);
        velocityValue.setText(context.getString(R.string.velocity_value, 100));
        velocityValue.setTextColor(AppUi.TEXT);
        velocityValue.setTextSize(14);
        velocityValue.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        keyboardPanel.addView(velocityValue);

        velocitySeekBar = new SeekBar(context);
        velocitySeekBar.setMax(126);
        velocitySeekBar.setProgress(99);
        velocitySeekBar.setContentDescription(context.getString(R.string.velocity_accessibility));
        AppUi.tint(velocitySeekBar);
        keyboardPanel.addView(velocitySeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AppUi.dp(context, 42)));

        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(context);
            line.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = AppUi.dp(context, 6);
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
                key.setBackground(AppUi.ripple(context, AppUi.CONTROL, 10, AppUi.KEY_BORDER));
                key.setContentDescription(context.getString(R.string.key_accessibility, noteName(note), note));
                LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0,
                        AppUi.dp(context, 66), 1);
                keyParams.setMargins(AppUi.dp(context, 2), AppUi.dp(context, 2), AppUi.dp(context, 2), AppUi.dp(context, 2));
                line.addView(key, keyParams);
                keyboardKeys[index] = key;
            }
        }

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.addView(root);
    }

    public void showMidiActivity(int channel) {
        midiActivity.setText(getContextualMidiActivity(channel));
        midiActivity.setTextColor(android.graphics.Color.rgb(16, 185, 129));
    }

    private String getContextualMidiActivity(int channel) {
        return scrollView.getContext().getString(R.string.midi_activity_channel, channel + 1);
    }

    private static String noteName(int note) {
        return new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}[note % 12]
                + (note / 12 - 1);
    }
}
