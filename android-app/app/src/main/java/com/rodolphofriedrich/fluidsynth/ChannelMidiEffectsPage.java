package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

/** Controles MIDI por canal, incluindo sensibilidade Pitch Bend via RPN 0,0. */
public final class ChannelMidiEffectsPage {
    public interface Listener {
        void onControlChanged(int channel, int controller, int value);
        void onControlCommitted(int channel, int controller, int value);
        void onSustainChanged(int channel, boolean active);
        void onVelocityCurveChanged(int channel, int curve);
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
    private final KnobView pitchBendRangeKnob;
    private final TextView pitchBendRangeValue;
    private final Button[] velocityCurveButtons = new Button[3];
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

        AppUi.addDivider(context, root);
        TextView pitchTitle = AppUi.sectionLabel(context, "SENSIBILIDADE DO PITCH BEND");
        LinearLayout.LayoutParams pitchTitleParams = new LinearLayout.LayoutParams(-1, -2);
        pitchTitleParams.topMargin = dp(14);
        root.addView(pitchTitle, pitchTitleParams);

        LinearLayout pitchRow = new LinearLayout(context);
        pitchRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pitchRowParams = new LinearLayout.LayoutParams(-1, -2);
        pitchRowParams.topMargin = dp(8);
        root.addView(pitchRow, pitchRowParams);

        LinearLayout pitchCell = new LinearLayout(context);
        pitchCell.setOrientation(LinearLayout.VERTICAL);
        pitchCell.setGravity(Gravity.CENTER_HORIZONTAL);
        pitchCell.setPadding(dp(4), dp(4), dp(4), dp(6));

        TextView pitchLabel = new TextView(context);
        pitchLabel.setText("Faixa em semitons");
        pitchLabel.setTextColor(AppUi.MUTED);
        pitchLabel.setTextSize(11);
        pitchLabel.setGravity(Gravity.CENTER);
        pitchCell.addView(pitchLabel, new LinearLayout.LayoutParams(-1, -2));

        pitchBendRangeKnob = new KnobView(context, 76);
        pitchBendRangeKnob.setContentDescription("Faixa de Pitch Bend em semitons");
        LinearLayout.LayoutParams pitchKnobParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        pitchKnobParams.topMargin = dp(4);
        pitchCell.addView(pitchBendRangeKnob, pitchKnobParams);

        pitchBendRangeValue = new TextView(context);
        AppUi.styleValue(pitchBendRangeValue);
        pitchBendRangeValue.setTextSize(12);
        pitchBendRangeValue.setTextColor(AppUi.TEXT);
        pitchBendRangeValue.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pitchValueParams = new LinearLayout.LayoutParams(-1, -2);
        pitchValueParams.topMargin = dp(3);
        pitchCell.addView(pitchBendRangeValue, pitchValueParams);

        TextView pitchHint = new TextView(context);
        pitchHint.setText("Ajuste independente por canal · padrão: 2 semitons");
        pitchHint.setTextColor(AppUi.MUTED);
        pitchHint.setTextSize(10);
        pitchHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pitchHintParams = new LinearLayout.LayoutParams(-1, -2);
        pitchHintParams.topMargin = dp(2);
        pitchCell.addView(pitchHint, pitchHintParams);
        pitchRow.addView(pitchCell, new LinearLayout.LayoutParams(0, -2, 1f));

        pitchBendRangeKnob.setListener((view, normalized, tracking) -> {
            int range = ChannelMidiSettingsStore.MIN_PITCH_BEND_RANGE
                    + Math.round(normalized * (ChannelMidiSettingsStore.MAX_PITCH_BEND_RANGE
                    - ChannelMidiSettingsStore.MIN_PITCH_BEND_RANGE));
            pitchBendRangeValue.setText(range + (range == 1 ? " semitom" : " semitons"));
            view.setContentDescription("Faixa de Pitch Bend: " + range + " semitons");
            if (!tracking) {
                ChannelMidiSettingsStore.savePitchBendRange(context, channel, range);
                applyPitchBendRange(channel, range);
            }
        });

        AppUi.addDivider(context, root);
        TextView velocityCurveTitle = AppUi.sectionLabel(context, "CURVA DE VELOCIDADE");
        LinearLayout.LayoutParams velocityTitleParams = new LinearLayout.LayoutParams(-1, -2);
        velocityTitleParams.topMargin = dp(14);
        root.addView(velocityCurveTitle, velocityTitleParams);

        TextView velocityCurveHint = new TextView(context);
        velocityCurveHint.setText("Resposta da intensidade das notas · independente por canal");
        velocityCurveHint.setTextColor(AppUi.MUTED);
        velocityCurveHint.setTextSize(10);
        LinearLayout.LayoutParams velocityHintParams = new LinearLayout.LayoutParams(-1, -2);
        velocityHintParams.topMargin = dp(3);
        root.addView(velocityCurveHint, velocityHintParams);

        LinearLayout velocityCurveRow = new LinearLayout(context);
        velocityCurveRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams velocityRowParams = new LinearLayout.LayoutParams(-1, -2);
        velocityRowParams.topMargin = dp(8);
        root.addView(velocityCurveRow, velocityRowParams);
        String[] curveLabels = {"Linear", "Suave", "Forte"};
        for (int i = 0; i < velocityCurveButtons.length; i++) {
            final int curve = i;
            Button button = new Button(context);
            button.setText(curveLabels[i]);
            button.setTextSize(11);
            button.setAllCaps(false);
            button.setMinHeight(dp(40));
            button.setMinimumHeight(dp(40));
            button.setPadding(dp(4), 0, dp(4), 0);
            button.setOnClickListener(v -> {
                VelocityCurveSettingsStore.save(context, channel, curve);
                updateVelocityCurveButtons(curve);
                if (listener != null) listener.onVelocityCurveChanged(channel, curve);
            });
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i > 0) buttonParams.leftMargin = dp(6);
            velocityCurveRow.addView(button, buttonParams);
            velocityCurveButtons[i] = button;
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
        int pitchRange = ChannelMidiSettingsStore.loadPitchBendRange(context, this.channel);
        pitchBendRangeKnob.setValue((pitchRange - ChannelMidiSettingsStore.MIN_PITCH_BEND_RANGE)
                / (float) (ChannelMidiSettingsStore.MAX_PITCH_BEND_RANGE
                - ChannelMidiSettingsStore.MIN_PITCH_BEND_RANGE));
        pitchBendRangeValue.setText(pitchRange
                + (pitchRange == 1 ? " semitom" : " semitons"));
        pitchBendRangeKnob.setContentDescription("Faixa de Pitch Bend: "
                + pitchRange + " semitons");
        sustain.setChecked(false);
        int velocityCurve = VelocityCurveSettingsStore.load(context, this.channel);
        updateVelocityCurveButtons(velocityCurve);
        binding = false;
        applyPitchBendRange(this.channel, pitchRange);
    }

    private void updateVelocityCurveButtons(int selected) {
        String[] labels = {"Linear", "Suave", "Forte"};
        for (int i = 0; i < velocityCurveButtons.length; i++) {
            Button button = velocityCurveButtons[i];
            if (button == null) continue;
            boolean active = i == selected;
            button.setText(labels[i]);
            button.setTextColor(active ? AppUi.TEXT : AppUi.MUTED);
            button.setBackground(AppUi.ripple(context,
                    active ? 0x40398BFF : AppUi.CONTROL, 10,
                    active ? VisualTheme.ACCENT : AppUi.CONTROL_BORDER));
        }
    }

    /** RPN 0,0: Pitch Bend Sensitivity (semitons + cents), seguido de RPN null. */
    private void applyPitchBendRange(int targetChannel, int semitones) {
        if (listener == null) return;
        int safeRange = ChannelMidiSettingsStore.clampPitchBendRange(semitones);
        listener.onControlChanged(targetChannel, 101, 0);
        listener.onControlChanged(targetChannel, 100, 0);
        listener.onControlChanged(targetChannel, 6, safeRange);
        listener.onControlChanged(targetChannel, 38, 0);
        listener.onControlChanged(targetChannel, 101, 127);
        listener.onControlChanged(targetChannel, 100, 127);
    }

    public void releaseSustain() {
        if (sustain.isChecked()) sustain.setChecked(false);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
