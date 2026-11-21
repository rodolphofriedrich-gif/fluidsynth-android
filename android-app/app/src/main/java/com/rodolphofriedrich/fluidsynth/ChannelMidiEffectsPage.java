package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** Controles MIDI por canal: todos os CCs contínuos usam knobs, sem SeekBars. */
public final class ChannelMidiEffectsPage {
    public interface Listener {
        void onControlChanged(int channel, int controller, int value);
        void onControlCommitted(int channel, int controller, int value);
        void onSustainChanged(int channel, boolean active);
    }

    private static final String[] LABELS = {
            "Modulação", "Expression", "Pan", "Envio Reverb",
            "Envio Chorus", "Cutoff", "Resonance"
    };
    private static final int COLUMNS = 3;
    private final Context context;
    private final LinearLayout root;
    private final TextView channelLabel;
    private final TextView[] values = new TextView[ChannelMidiSettingsStore.CONTROLLERS.length];
    private final KnobView[] knobs = new KnobView[ChannelMidiSettingsStore.CONTROLLERS.length];
    private final Switch sustain;
    private Listener listener;
    private int channel;
    private boolean binding;

    public ChannelMidiEffectsPage(Context context) {
        this.context = context;
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(12), 0, dp(72));
        root.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        root.setElevation(0);

        TextView title = new TextView(context);
        title.setText("Controles MIDI por canal");
        AppUi.styleHeading(title);
        title.setTextSize(17);
        root.addView(title);

        channelLabel = new TextView(context);
        channelLabel.setTextColor(AppUi.MUTED);
        channelLabel.setTextSize(12);
        LinearLayout.LayoutParams channelParams = new LinearLayout.LayoutParams(-1, -2);
        channelParams.topMargin = dp(4);
        root.addView(channelLabel, channelParams);

        LinearLayout activeRow = new LinearLayout(context);
        activeRow.setGravity(Gravity.CENTER_VERTICAL);
        activeRow.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        activeRow.setPadding(0, dp(8), 0, dp(8));
        activeRow.setMinimumHeight(dp(64));
        LinearLayout.LayoutParams activeParams = new LinearLayout.LayoutParams(-1, -2);
        activeParams.topMargin = dp(12);
        root.addView(activeRow, activeParams);

        LinearLayout activeText = new LinearLayout(context);
        activeText.setOrientation(LinearLayout.VERTICAL);
        TextView sustainTitle = new TextView(context);
        sustainTitle.setText("Sustain ativo");
        sustainTitle.setTextColor(AppUi.TEXT);
        sustainTitle.setTextSize(14);
        sustainTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activeText.addView(sustainTitle);
        TextView sustainHint = new TextView(context);
        sustainHint.setText("Mantém as notas enquanto estiver ativado");
        sustainHint.setTextColor(AppUi.MUTED);
        sustainHint.setTextSize(10);
        activeText.addView(sustainHint);
        activeRow.addView(activeText, new LinearLayout.LayoutParams(0, -2, 1f));

        sustain = new Switch(context);
        sustain.setThumbTintList(new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {VisualTheme.EFFECTS, AppUi.MUTED}));
        sustain.setTrackTintList(new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {0x609874E8, AppUi.CONTROL_BORDER}));
        activeRow.addView(sustain);
        activeRow.setOnClickListener(v -> sustain.setChecked(!sustain.isChecked()));
        sustain.setOnCheckedChangeListener((button, checked) -> {
            if (!binding && listener != null) listener.onSustainChanged(channel, checked);
        });

        AppUi.addDivider(context, root);
        TextView controlsTitle = AppUi.sectionLabel(context, "CONTROLES MIDI");
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
        controlsParams.topMargin = dp(12);
        root.addView(controlsTitle, controlsParams);

        LinearLayout row = null;
        for (int i = 0; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            final int index = i;
            final int cc = ChannelMidiSettingsStore.CONTROLLERS[i];
            if (i % COLUMNS == 0) {
                row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.topMargin = dp(10);
                root.addView(row, rowParams);
            }

            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(2), dp(4), dp(2), dp(6));

            TextView label = new TextView(context);
            label.setText(LABELS[i]);
            label.setTextColor(AppUi.MUTED);
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            cell.addView(label, new LinearLayout.LayoutParams(-1, dp(28)));

            KnobView knob = new KnobView(context, 68);
            knob.setContentDescription(LABELS[i] + ", CC " + cc);
            knob.setValue(0f);
            LinearLayout.LayoutParams knobParams = new LinearLayout.LayoutParams(dp(68), dp(68));
            knobParams.topMargin = dp(2);
            cell.addView(knob, knobParams);

            TextView value = new TextView(context);
            AppUi.styleValue(value);
            value.setTextSize(11);
            value.setTextColor(AppUi.MUTED);
            value.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
            valueParams.topMargin = dp(2);
            cell.addView(value, valueParams);

            values[i] = value;
            knobs[i] = knob;
            knob.setListener((view, normalized, tracking) -> {
                int progress = Math.round(normalized * 127f);
                value.setText(String.valueOf(progress));
                view.setContentDescription(LABELS[index] + ", CC " + cc + ", " + progress);
                if (listener != null) {
                    listener.onControlChanged(channel, cc, progress);
                    if (!tracking) listener.onControlCommitted(channel, cc, progress);
                }
            });
            row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        setChannel(0, ChannelMidiSettingsStore.load(context, 0));
    }

    public View view() { return root; }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setChannel(int channel, int[] storedValues) {
        // Liberar sustain antes de trocar o canal evita enviar CC64 ao canal errado.
        releaseSustain();
        this.channel = Math.max(0, Math.min(15, channel));
        int[] safe = storedValues == null
                ? ChannelMidiSettingsStore.load(context, this.channel) : storedValues;
        channelLabel.setText("Ajustes em tempo real · Canal " + (this.channel + 1));
        binding = true;
        for (int i = 0; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            int value = i < safe.length ? Math.max(0, Math.min(127, safe[i])) : 0;
            knobs[i].setValue(value / 127f);
            knobs[i].setContentDescription(LABELS[i] + ", CC "
                    + ChannelMidiSettingsStore.CONTROLLERS[i] + ", " + value);
            values[i].setText(String.valueOf(value));
        }
        sustain.setChecked(false);
        binding = false;
    }

    public void releaseSustain() {
        if (sustain.isChecked()) sustain.setChecked(false);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
