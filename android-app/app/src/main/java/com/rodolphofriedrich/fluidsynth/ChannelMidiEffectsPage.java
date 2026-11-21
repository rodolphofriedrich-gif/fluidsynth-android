package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Controles MIDI por canal apresentados no mesmo padrão visual dos efeitos globais. */
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
    private static final int KNOB_COUNT = 3;
    private final Context context;
    private final LinearLayout root;
    private final TextView channelLabel;
    private final TextView[] values = new TextView[ChannelMidiSettingsStore.CONTROLLERS.length];
    private final KnobView[] knobs = new KnobView[KNOB_COUNT];
    private final SeekBar[] sliders = new SeekBar[ChannelMidiSettingsStore.CONTROLLERS.length];
    private final Switch sustain;
    private Listener listener;
    private int channel;
    private boolean binding;

    public ChannelMidiEffectsPage(Context context) {
        this.context = context;
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(12), 0, dp(32));
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

        TextView controlsTitle = AppUi.sectionLabel(context, "CONTROLES CONTÍNUOS");
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
        controlsParams.topMargin = dp(14);
        root.addView(controlsTitle, controlsParams);

        LinearLayout knobRow = new LinearLayout(context);
        knobRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams knobRowParams = new LinearLayout.LayoutParams(-1, -2);
        knobRowParams.topMargin = dp(8);
        root.addView(knobRow, knobRowParams);

        for (int i = 0; i < KNOB_COUNT; i++) {
            final int index = i;
            final int cc = ChannelMidiSettingsStore.CONTROLLERS[i];
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(2), dp(4), dp(2), dp(4));

            TextView label = new TextView(context);
            label.setText(LABELS[i]);
            label.setTextColor(AppUi.MUTED);
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            cell.addView(label, new LinearLayout.LayoutParams(-1, -2));

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
            cell.addView(value, new LinearLayout.LayoutParams(-1, -2));

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

            knobRow.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }

        AppUi.addDivider(context, root);
        TextView sendsTitle = AppUi.sectionLabel(context, "ENVIOS E FILTROS");
        LinearLayout.LayoutParams sendsParams = new LinearLayout.LayoutParams(-1, -2);
        sendsParams.topMargin = dp(8);
        root.addView(sendsTitle, sendsParams);

        for (int i = KNOB_COUNT; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            final int index = i;
            final int cc = ChannelMidiSettingsStore.CONTROLLERS[i];
            LinearLayout group = new LinearLayout(context);
            group.setOrientation(LinearLayout.VERTICAL);
            group.setPadding(0, dp(10), 0, dp(10));
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(-1, -2);
            groupParams.topMargin = dp(2);
            root.addView(group, groupParams);
            if (i > KNOB_COUNT) AppUi.addDivider(context, root);

            LinearLayout heading = new LinearLayout(context);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = new TextView(context);
            label.setText(LABELS[i] + "  ·  CC " + cc);
            label.setTextColor(AppUi.TEXT);
            label.setTextSize(13);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            heading.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView value = new TextView(context);
            AppUi.styleValue(value);
            value.setTextSize(12);
            value.setTextColor(AppUi.MUTED);
            heading.addView(value);
            group.addView(heading);
            values[i] = value;

            SeekBar slider = new SeekBar(context);
            slider.setMax(127);
            slider.setContentDescription(LABELS[i] + ", CC " + cc);
            AppUi.tint(slider);
            LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(-1, dp(48));
            sliderParams.topMargin = dp(7);
            group.addView(slider, sliderParams);
            sliders[i] = slider;
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    value.setText(String.valueOf(progress));
                    if (fromUser && listener != null) listener.onControlChanged(channel, cc, progress);
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {
                    if (listener != null) listener.onControlCommitted(channel, cc, bar.getProgress());
                }
            });
        }
        setChannel(0, ChannelMidiSettingsStore.load(context, 0));
    }

    public View view() { return root; }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setChannel(int channel, int[] storedValues) {
        releaseSustain();
        this.channel = Math.max(0, Math.min(15, channel));
        int[] safe = storedValues == null
                ? ChannelMidiSettingsStore.load(context, this.channel) : storedValues;
        channelLabel.setText("Ajustes em tempo real · Canal " + (this.channel + 1));
        binding = true;
        for (int i = 0; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            int value = i < safe.length ? Math.max(0, Math.min(127, safe[i])) : 0;
            if (i < KNOB_COUNT) {
                knobs[i].setValue(value / 127f);
                knobs[i].setContentDescription(LABELS[i] + ", CC "
                        + ChannelMidiSettingsStore.CONTROLLERS[i] + ", " + value);
            } else if (sliders[i] != null) {
                sliders[i].setProgress(value);
            }
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
