package com.rodolphofriedrich.fluidsynth;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Painel isolado para edição dos controladores MIDI do canal selecionado. */
public final class ChannelMidiControlsDialog {
    public interface Listener {
        void onControlChanged(int channel, int controller, int value);
        void onControlCommitted(int channel, int controller, int value);
        void onSustainChanged(int channel, boolean active);
        void onDismissed();
    }

    private static final String[] LABELS = {
            "Modulação", "Expression", "Pan", "Envio Reverb",
            "Envio Chorus", "Cutoff", "Resonance"
    };
    private final Context context;
    private final int channel;
    private final int[] values;
    private final Listener listener;
    private final SeekBar[] sliders = new SeekBar[ChannelMidiSettingsStore.CONTROLLERS.length];

    public ChannelMidiControlsDialog(Context context, int channel, int[] values, Listener listener) {
        this.context = context;
        this.channel = Math.max(0, Math.min(15, channel));
        this.values = values == null ? ChannelMidiSettingsStore.load(context, this.channel) : values.clone();
        this.listener = listener;
    }

    public void show() {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = AppUi.dp(context, 16);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);

        TextView description = new TextView(context);
        description.setText("Ajustes em tempo real do canal " + (channel + 1));
        description.setTextColor(AppUi.MUTED);
        description.setTextSize(13);
        content.addView(description);

        for (int i = 0; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            final int index = i;
            final int controller = ChannelMidiSettingsStore.CONTROLLERS[i];
            LinearLayout group = new LinearLayout(context);
            group.setOrientation(LinearLayout.VERTICAL);
            group.setPadding(0, AppUi.dp(context, 10), 0, AppUi.dp(context, 4));

            LinearLayout heading = new LinearLayout(context);
            heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView label = new TextView(context);
            label.setText(LABELS[i] + "  ·  CC " + controller);
            label.setTextColor(AppUi.TEXT);
            label.setTextSize(14);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView value = new TextView(context);
            value.setText(String.valueOf(this.values[i]));
            AppUi.styleValue(value);
            heading.addView(value);
            group.addView(heading);

            SeekBar slider = new SeekBar(context);
            slider.setMax(127);
            slider.setProgress(Math.max(0, Math.min(127, this.values[i])));
            slider.setContentDescription(LABELS[i] + ", canal " + (channel + 1));
            AppUi.tint(slider);
            group.addView(slider, new LinearLayout.LayoutParams(-1, AppUi.dp(context, 48)));
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    value.setText(String.valueOf(progress));
                    if (fromUser && listener != null) listener.onControlChanged(channel, controller, progress);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (listener != null) listener.onControlCommitted(channel, controller, seekBar.getProgress());
                }
            });
            sliders[i] = slider;
            content.addView(group);
        }

        Switch sustain = new Switch(context);
        sustain.setText("Sustain (CC 64)");
        sustain.setTextColor(AppUi.TEXT);
        sustain.setPadding(0, AppUi.dp(context, 12), 0, AppUi.dp(context, 8));
        sustain.setContentDescription("Ativar ou desativar sustain no canal " + (channel + 1));
        content.addView(sustain);
        sustain.setOnCheckedChangeListener((button, checked) -> {
            if (listener != null) listener.onSustainChanged(channel, checked);
        });

        AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Controles MIDI · Canal " + (channel + 1))
                .setView(scroll)
                .setNegativeButton("Fechar", null)
                .create();
        dialog.setOnDismissListener(ignored -> {
            if (sustain.isChecked()) sustain.setChecked(false);
            if (listener != null) listener.onDismissed();
        });
        dialog.show();

        // Reaplica os valores persistidos ao motor, que pode ter sido reinicializado.
        for (int i = 0; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            if (listener != null) listener.onControlChanged(channel,
                    ChannelMidiSettingsStore.CONTROLLERS[i], sliders[i].getProgress());
        }
    }
}
