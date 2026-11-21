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
    private final Context context;
    private final LinearLayout root;
    private final TextView channelLabel;
    private final TextView[] values = new TextView[ChannelMidiSettingsStore.CONTROLLERS.length];
    private final SeekBar[] sliders = new SeekBar[ChannelMidiSettingsStore.CONTROLLERS.length];
    private final Switch sustain;
    private Listener listener;
    private int channel;
    private boolean binding;

    public ChannelMidiEffectsPage(Context context) {
        this.context = context;
        root = AppUi.panel(context, null);
        root.setOrientation(LinearLayout.VERTICAL);

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
        activeRow.setBackground(AppUi.ripple(context, AppUi.CONTROL, 12, AppUi.CONTROL_BORDER));
        activeRow.setPadding(dp(14), dp(8), dp(10), dp(8));
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

        TextView controlsTitle = AppUi.sectionLabel(context, "CONTROLADORES MIDI");
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
        controlsParams.topMargin = dp(18);
        root.addView(controlsTitle, controlsParams);

        for (int i = 0; i < ChannelMidiSettingsStore.CONTROLLERS.length; i++) {
            final int index = i;
            final int cc = ChannelMidiSettingsStore.CONTROLLERS[i];
            LinearLayout group = new LinearLayout(context);
            group.setOrientation(LinearLayout.VERTICAL);
            group.setPadding(dp(12), dp(9), dp(12), dp(7));
            group.setBackground(AppUi.rounded(context, AppUi.CONTROL, 12, AppUi.CONTROL_BORDER));
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(-1, -2);
            groupParams.topMargin = dp(8);
            root.addView(group, groupParams);

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
            value.setTextSize(13);
            heading.addView(value);
            group.addView(heading);
            values[i] = value;

            SeekBar slider = new SeekBar(context);
            slider.setMax(127);
            slider.setContentDescription(LABELS[i] + ", CC " + cc);
            AppUi.tint(slider);
            group.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
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
        this.channel = Math.max(0, Math.min(15, channel));
        int[] safe = storedValues == null
                ? ChannelMidiSettingsStore.load(context, this.channel) : storedValues;
        channelLabel.setText("Ajustes em tempo real · Canal " + (this.channel + 1));
        binding = true;
        for (int i = 0; i < sliders.length; i++) {
            int value = i < safe.length ? Math.max(0, Math.min(127, safe[i])) : 0;
            sliders[i].setProgress(value);
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
