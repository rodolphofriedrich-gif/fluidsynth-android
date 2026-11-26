package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

/** Editor de timbre por canal; os módulos são ativados em etapas independentes. */
public final class TimbreScreen {
    public interface Listener {
        void onSampleTuningChanged(int channel, int coarseSemitones, int fineCents, boolean tracking);
        void onFilterAdsrChanged(int channel, float cutoffHz, float resonance, float attack, float decay, float sustain, float release, boolean tracking);
        void onLfoChanged(int channel, float rateHz, float depthCents, boolean tracking);
        void onEffectSendsChanged(int channel, int reverbSend, int chorusSend, boolean tracking);
    }

    public final ScrollView scrollView;

    private final Context context;
    private final TextView channelLabel;
    private final TextView instrumentLabel;
    private final GridLayout moduleGrid;
    private final KnobView transposeKnob;
    private final KnobView fineTuneKnob;
    private final TextView transposeValue;
    private final TextView fineTuneValue;
    private final KnobView[] timbreKnobs = new KnobView[6];
    private final TextView[] timbreValues = new TextView[6];
    private final float[] timbreParams = {20000f, 0f, 0.01f, 0.1f, 100f, 0.1f};
    private KnobView lfoRateKnob;
    private KnobView lfoDepthKnob;
    private TextView lfoRateValue;
    private TextView lfoDepthValue;
    private KnobView reverbSendKnob;
    private KnobView chorusSendKnob;
    private TextView reverbSendValue;
    private TextView chorusSendValue;
    private float lfoRateHz = TimbreLfoSettingsStore.DEFAULT_RATE_HZ;
    private float lfoDepthCents = TimbreLfoSettingsStore.DEFAULT_DEPTH_CENTS;
    private int reverbSend = TimbreEffectSendSettingsStore.DEFAULT_REVERB_SEND;
    private int chorusSend = TimbreEffectSendSettingsStore.DEFAULT_CHORUS_SEND;
    private int activeChannel;
    private int coarseSemitones;
    private int fineCents;
    private Listener listener;

    private static final String[] MODULE_TITLES = {
            "FONTE SONORA  ·  SAMPLE",
            "FILTRO  ·  FILTER",
            "ENVELOPE",
            "LFO",
            "EFEITOS  ·  EFFECTS"
    };

    private static final String[] MODULE_DETAILS = {
            "",
            "Brilho (Cutoff)\nRessonância (Resonance)",
            "Ataque (Attack) · Decaimento (Decay)\nSustentação (Sustain) · Liberação (Release)",
            "Vibrato de altura (Pitch) · Rate · Depth",
            "Envio por canal: Reverb (CC91) · Chorus (CC93)"
    };

    private final View[] moduleViews = new View[MODULE_TITLES.length];

    public TimbreScreen(Context context) {
        this.context = context;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(context, 16), AppUi.dp(context, 18),
                AppUi.dp(context, 16), AppUi.dp(context, 24));
        root.setBackgroundColor(AppUi.BG);

        TextView heading = new TextView(context);
        heading.setText("EDITOR DE TIMBRE");
        heading.setTextColor(AppUi.TEXT);
        heading.setTextSize(15);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setLetterSpacing(0.06f);
        root.addView(heading, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText("Edição do instrumento no canal selecionado");
        subtitle.setTextColor(AppUi.MUTED);
        subtitle.setTextSize(12);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = AppUi.dp(context, 4);
        subtitleParams.bottomMargin = AppUi.dp(context, 14);
        root.addView(subtitle, subtitleParams);

        LinearLayout selected = new LinearLayout(context);
        selected.setOrientation(LinearLayout.VERTICAL);
        selected.setPadding(AppUi.dp(context, 12), AppUi.dp(context, 10),
                AppUi.dp(context, 12), AppUi.dp(context, 10));
        selected.setBackground(AppUi.rounded(context, AppUi.PANEL, 10, AppUi.BORDER));

        channelLabel = new TextView(context);
        channelLabel.setText("CANAL 01");
        channelLabel.setTextColor(VisualTheme.ACCENT);
        channelLabel.setTextSize(11);
        channelLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selected.addView(channelLabel);

        instrumentLabel = new TextView(context);
        instrumentLabel.setText("Nenhum instrumento selecionado");
        instrumentLabel.setTextColor(AppUi.MUTED);
        instrumentLabel.setTextSize(13);
        instrumentLabel.setSingleLine(true);
        instrumentLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams instrumentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        instrumentParams.topMargin = AppUi.dp(context, 3);
        selected.addView(instrumentLabel, instrumentParams);

        LinearLayout.LayoutParams selectedParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        selectedParams.bottomMargin = AppUi.dp(context, 18);
        root.addView(selected, selectedParams);

        moduleGrid = new GridLayout(context);
        moduleGrid.setUseDefaultMargins(false);
        moduleViews[0] = createSampleModule();
        moduleViews[1] = createFilterModule();
        moduleViews[2] = createEnvelopeModule();
        moduleViews[3] = createLfoModule();
        moduleViews[4] = createEffectSendsModule();
        root.addView(moduleGrid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        updateGridColumns(context.getResources().getConfiguration());

        TextView notice = new TextView(context);
        notice.setText("Transposição, filtro, envelope, LFO e envios de efeitos são independentes por canal.");
        notice.setTextColor(AppUi.MUTED);
        notice.setTextSize(11);
        notice.setGravity(Gravity.CENTER);
        notice.setPadding(AppUi.dp(context, 12), AppUi.dp(context, 14),
                AppUi.dp(context, 12), AppUi.dp(context, 8));
        root.addView(notice, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.addView(root);

        transposeKnob = (KnobView) moduleViews[0].findViewWithTag("sample_transpose_knob");
        fineTuneKnob = (KnobView) moduleViews[0].findViewWithTag("sample_fine_knob");
        transposeValue = (TextView) moduleViews[0].findViewWithTag("sample_transpose_value");
        fineTuneValue = (TextView) moduleViews[0].findViewWithTag("sample_fine_value");

        transposeKnob.setListener((knob, value, tracking) -> {
            coarseSemitones = SampleTuningSettingsStore.clampCoarse(
                    Math.round(value * 48f) - 24);
            updateTuningLabels();
            dispatchTuning(tracking);
        });
        fineTuneKnob.setListener((knob, value, tracking) -> {
            fineCents = SampleTuningSettingsStore.clampFine(
                    Math.round(value * 200f) - 100);
            updateTuningLabels();
            dispatchTuning(tracking);
        });
        setTuning(0, 0);
    }

    /** Retira o conteúdo para incorporá-lo ao painel retrátil de processamento. */
    public View detachContent() {
        View content = scrollView.getChildAt(0);
        if (content != null) scrollView.removeView(content);
        return content;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private View createSampleModule() {
        LinearLayout module = new LinearLayout(context);
        module.setOrientation(LinearLayout.VERTICAL);
        module.setPadding(AppUi.dp(context, 12), AppUi.dp(context, 12),
                AppUi.dp(context, 12), AppUi.dp(context, 12));
        module.setBackground(AppUi.rounded(context, AppUi.PANEL, 10, AppUi.BORDER));

        TextView title = new TextView(context);
        title.setText(MODULE_TITLES[0]);
        title.setTextColor(VisualTheme.ACCENT);
        title.setTextSize(10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        module.addView(title);

        LinearLayout controls = new LinearLayout(context);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        controlsParams.topMargin = AppUi.dp(context, 12);
        module.addView(controls, controlsParams);

        LinearLayout transpose = createSampleControl("Transposição", "(Transpose)");
        KnobView transposeControl = new KnobView(context, 76);
        transposeControl.setTag("sample_transpose_knob");
        TextView transposeNumber = sampleValue("0 st");
        transposeNumber.setTag("sample_transpose_value");
        transpose.addView(transposeControl, centeredKnobParams());
        transpose.addView(transposeNumber, sampleValueParams());
        controls.addView(transpose, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout fine = createSampleControl("Afinação fina", "(Fine Tuning)");
        KnobView fineControl = new KnobView(context, 76);
        fineControl.setTag("sample_fine_knob");
        TextView fineNumber = sampleValue("0 cents");
        fineNumber.setTag("sample_fine_value");
        fine.addView(fineControl, centeredKnobParams());
        fine.addView(fineNumber, sampleValueParams());
        controls.addView(fine, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return module;
    }

    private LinearLayout createSampleControl(String portuguese, String english) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        TextView label = new TextView(context);
        label.setText(portuguese);
        label.setTextColor(AppUi.TEXT);
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        column.addView(label);
        TextView secondary = new TextView(context);
        secondary.setText(english);
        secondary.setTextColor(AppUi.MUTED);
        secondary.setTextSize(9);
        secondary.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams secondaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        secondaryParams.topMargin = AppUi.dp(context, 1);
        column.addView(secondary, secondaryParams);
        return column;
    }

    private LinearLayout.LayoutParams centeredKnobParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                AppUi.dp(context, 82), AppUi.dp(context, 82));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.topMargin = AppUi.dp(context, 6);
        return params;
    }

    private LinearLayout.LayoutParams sampleValueParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppUi.dp(context, 3);
        return params;
    }

    private TextView sampleValue(String value) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(VisualTheme.ACCENT);
        text.setTextSize(11);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    private View createFilterModule() {
        LinearLayout module = createActiveModule(MODULE_TITLES[1]);
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        module.addView(controls, moduleControlsParams());
        addTimbreControl(controls, 0, "Brilho", "(Cutoff)", "20 kHz");
        addTimbreControl(controls, 1, "Ressonância", "(Resonance)", "0%");
        return module;
    }

    private View createEnvelopeModule() {
        LinearLayout module = createActiveModule(MODULE_TITLES[2]);
        LinearLayout first = new LinearLayout(context);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.setGravity(Gravity.CENTER);
        module.addView(first, moduleControlsParams());
        addTimbreControl(first, 2, "Ataque", "(Attack)", "0.01 s");
        addTimbreControl(first, 3, "Decaimento", "(Decay)", "0.10 s");
        LinearLayout second = new LinearLayout(context);
        second.setOrientation(LinearLayout.HORIZONTAL);
        second.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams secondParams = moduleControlsParams();
        secondParams.topMargin = AppUi.dp(context, 8);
        module.addView(second, secondParams);
        addTimbreControl(second, 4, "Sustentação", "(Sustain)", "100%");
        addTimbreControl(second, 5, "Liberação", "(Release)", "0.10 s");
        return module;
    }

    private LinearLayout createActiveModule(String titleText) {
        LinearLayout module = new LinearLayout(context);
        module.setOrientation(LinearLayout.VERTICAL);
        module.setPadding(AppUi.dp(context, 12), AppUi.dp(context, 12),
                AppUi.dp(context, 12), AppUi.dp(context, 12));
        module.setBackground(AppUi.rounded(context, AppUi.PANEL, 10, AppUi.BORDER));
        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(VisualTheme.ACCENT);
        title.setTextSize(10);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setLetterSpacing(0.04f);
        module.addView(title);
        return module;
    }

    private LinearLayout.LayoutParams moduleControlsParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AppUi.dp(context, 10);
        return params;
    }

    private void addTimbreControl(LinearLayout parent, int index, String pt, String en, String initial) {
        LinearLayout column = createSampleControl(pt, en);
        KnobView knob = new KnobView(context, 68);
        TextView value = sampleValue(initial);
        timbreKnobs[index] = knob;
        timbreValues[index] = value;
        column.addView(knob, centeredKnobParams());
        column.addView(value, sampleValueParams());
        parent.addView(column, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        knob.setListener((view, normalized, tracking) -> {
            switch (index) {
                case 0: timbreParams[0] = (float) (20.0 * Math.pow(1000.0, normalized)); break;
                case 1: timbreParams[1] = normalized * 100f; break;
                case 2: timbreParams[2] = (float) (0.001 * Math.pow(10000.0, normalized)); break;
                case 3: timbreParams[3] = (float) (0.001 * Math.pow(10000.0, normalized)); break;
                case 4: timbreParams[4] = normalized * 100f; break;
                case 5: timbreParams[5] = (float) (0.001 * Math.pow(10000.0, normalized)); break;
            }
            updateTimbreLabels();
            dispatchFilterAdsr(tracking);
        });
    }

    private void setFilterAdsr(float cutoff, float resonance, float attack,
            float decay, float sustain, float release) {
        timbreParams[0] = TimbreFilterAdsrSettingsStore.clamp(cutoff, 20f, 20000f);
        timbreParams[1] = TimbreFilterAdsrSettingsStore.clamp(resonance, 0f, 100f);
        timbreParams[2] = TimbreFilterAdsrSettingsStore.clamp(attack, 0.001f, 10f);
        timbreParams[3] = TimbreFilterAdsrSettingsStore.clamp(decay, 0.001f, 10f);
        timbreParams[4] = TimbreFilterAdsrSettingsStore.clamp(sustain, 0f, 100f);
        timbreParams[5] = TimbreFilterAdsrSettingsStore.clamp(release, 0.001f, 10f);
        timbreKnobs[0].setValue((float) (Math.log(timbreParams[0] / 20.0) / Math.log(1000.0)));
        timbreKnobs[1].setValue(timbreParams[1] / 100f);
        timbreKnobs[2].setValue((float) (Math.log(timbreParams[2] / 0.001) / Math.log(10000.0)));
        timbreKnobs[3].setValue((float) (Math.log(timbreParams[3] / 0.001) / Math.log(10000.0)));
        timbreKnobs[4].setValue(timbreParams[4] / 100f);
        timbreKnobs[5].setValue((float) (Math.log(timbreParams[5] / 0.001) / Math.log(10000.0)));
        updateTimbreLabels();
    }

    private void updateTimbreLabels() {
        timbreValues[0].setText(timbreParams[0] >= 1000f
                ? String.format(Locale.getDefault(), "%.1f kHz", timbreParams[0] / 1000f)
                : String.format(Locale.getDefault(), "%.0f Hz", timbreParams[0]));
        timbreValues[1].setText(String.format(Locale.getDefault(), "%.0f%%", timbreParams[1]));
        timbreValues[2].setText(String.format(Locale.getDefault(), "%.3f s", timbreParams[2]));
        timbreValues[3].setText(String.format(Locale.getDefault(), "%.3f s", timbreParams[3]));
        timbreValues[4].setText(String.format(Locale.getDefault(), "%.0f%%", timbreParams[4]));
        timbreValues[5].setText(String.format(Locale.getDefault(), "%.3f s", timbreParams[5]));
    }

    private void dispatchFilterAdsr(boolean tracking) {
        if (listener != null) listener.onFilterAdsrChanged(activeChannel,
                timbreParams[0], timbreParams[1], timbreParams[2], timbreParams[3],
                timbreParams[4], timbreParams[5], tracking);
    }

    private View createLfoModule() {
        LinearLayout module = createActiveModule(MODULE_TITLES[3]);
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        module.addView(controls, moduleControlsParams());
        addLfoControl(controls, true);
        addLfoControl(controls, false);

        TextView hint = new TextView(context);
        hint.setText("Vibrato por canal. O ajuste soma-se à modulação original da SoundFont.");
        hint.setTextColor(AppUi.MUTED);
        hint.setTextSize(10);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = AppUi.dp(context, 8);
        module.addView(hint, hintParams);
        return module;
    }

    private void addLfoControl(LinearLayout parent, boolean rate) {
        LinearLayout column = createSampleControl(
                rate ? "Velocidade" : "Profundidade",
                rate ? "(Rate)" : "(Depth · Pitch)");
        KnobView knob = new KnobView(context, 68);
        TextView value = sampleValue(rate ? "5.0 Hz" : "0 cents");
        column.addView(knob, centeredKnobParams());
        column.addView(value, sampleValueParams());
        parent.addView(column, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (rate) {
            lfoRateKnob = knob;
            lfoRateValue = value;
            knob.setListener((view, normalized, tracking) -> {
                lfoRateHz = (float) (0.1 * Math.pow(200.0, normalized));
                updateLfoLabels();
                dispatchLfo(tracking);
            });
        } else {
            lfoDepthKnob = knob;
            lfoDepthValue = value;
            knob.setListener((view, normalized, tracking) -> {
                lfoDepthCents = normalized * 100f;
                updateLfoLabels();
                dispatchLfo(tracking);
            });
        }
    }

    private void setLfo(float rateHz, float depthCents) {
        lfoRateHz = Math.max(0.1f, Math.min(20f, rateHz));
        lfoDepthCents = Math.max(0f, Math.min(100f, depthCents));
        lfoRateKnob.setValue((float) (Math.log(lfoRateHz / 0.1) / Math.log(200.0)));
        lfoDepthKnob.setValue(lfoDepthCents / 100f);
        updateLfoLabels();
    }

    private void updateLfoLabels() {
        lfoRateValue.setText(String.format(Locale.getDefault(), "%.1f Hz", lfoRateHz));
        lfoDepthValue.setText(String.format(Locale.getDefault(), "%.0f cents", lfoDepthCents));
        lfoRateKnob.setContentDescription("Velocidade do LFO: " + lfoRateValue.getText());
        lfoDepthKnob.setContentDescription("Profundidade do vibrato: " + lfoDepthValue.getText());
    }

    private void dispatchLfo(boolean tracking) {
        if (listener != null) listener.onLfoChanged(activeChannel, lfoRateHz, lfoDepthCents, tracking);
    }

    private View createEffectSendsModule() {
        LinearLayout module = createActiveModule(MODULE_TITLES[4]);
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        module.addView(controls, moduleControlsParams());
        addEffectSendControl(controls, true);
        addEffectSendControl(controls, false);

        TextView hint = new TextView(context);
        hint.setText("Envios MIDI por canal. Os parâmetros globais ficam nas abas Reverb e Chorus.");
        hint.setTextColor(AppUi.MUTED);
        hint.setTextSize(10);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = AppUi.dp(context, 8);
        module.addView(hint, hintParams);
        return module;
    }

    private void addEffectSendControl(LinearLayout parent, boolean reverb) {
        LinearLayout column = createSampleControl(
                reverb ? "Envio de Reverb" : "Envio de Chorus",
                reverb ? "(MIDI CC 91)" : "(MIDI CC 93)");
        KnobView knob = new KnobView(context, 68);
        TextView value = sampleValue("0 / 127");
        column.addView(knob, centeredKnobParams());
        column.addView(value, sampleValueParams());
        parent.addView(column, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (reverb) {
            reverbSendKnob = knob;
            reverbSendValue = value;
            knob.setListener((view, normalized, tracking) -> {
                reverbSend = Math.max(0, Math.min(127, Math.round(normalized * 127f)));
                updateEffectSendLabels();
                dispatchEffectSends(tracking);
            });
        } else {
            chorusSendKnob = knob;
            chorusSendValue = value;
            knob.setListener((view, normalized, tracking) -> {
                chorusSend = Math.max(0, Math.min(127, Math.round(normalized * 127f)));
                updateEffectSendLabels();
                dispatchEffectSends(tracking);
            });
        }
    }

    private void setEffectSends(int reverb, int chorus) {
        reverbSend = Math.max(0, Math.min(127, reverb));
        chorusSend = Math.max(0, Math.min(127, chorus));
        reverbSendKnob.setValue(reverbSend / 127f);
        chorusSendKnob.setValue(chorusSend / 127f);
        updateEffectSendLabels();
    }

    private void updateEffectSendLabels() {
        reverbSendValue.setText(reverbSend + " / 127");
        chorusSendValue.setText(chorusSend + " / 127");
        reverbSendKnob.setContentDescription("Envio de Reverb CC 91: " + reverbSend);
        chorusSendKnob.setContentDescription("Envio de Chorus CC 93: " + chorusSend);
    }

    private void dispatchEffectSends(boolean tracking) {
        if (listener != null) listener.onEffectSendsChanged(
                activeChannel, reverbSend, chorusSend, tracking);
    }

    private View createModule(String title, String details) {
        LinearLayout module = new LinearLayout(context);
        module.setOrientation(LinearLayout.VERTICAL);
        module.setPadding(AppUi.dp(context, 12), AppUi.dp(context, 12),
                AppUi.dp(context, 12), AppUi.dp(context, 12));
        module.setBackground(AppUi.rounded(context, AppUi.PANEL, 10, AppUi.BORDER));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(VisualTheme.ACCENT);
        titleView.setTextSize(10);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setLetterSpacing(0.04f);
        module.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView detailView = new TextView(context);
        detailView.setText(details);
        detailView.setTextColor(AppUi.TEXT);
        detailView.setTextSize(12);
        detailView.setLineSpacing(AppUi.dp(context, 3), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = AppUi.dp(context, 8);
        module.addView(detailView, detailParams);

        TextView state = new TextView(context);
        state.setText("EM PREPARAÇÃO");
        state.setTextColor(AppUi.MUTED);
        state.setTextSize(9);
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = AppUi.dp(context, 10);
        module.addView(state, stateParams);
        return module;
    }

    public void setChannel(int channel, String instrument, boolean hasSoundFont,
            int coarseSemitones, int fineCents) {
        int safeChannel = Math.max(0, Math.min(15, channel));
        channelLabel.setText(String.format(Locale.ROOT, "CANAL %02d", safeChannel + 1));
        instrumentLabel.setText(instrument == null || instrument.trim().isEmpty()
                ? "Nenhum instrumento selecionado" : instrument);
        instrumentLabel.setTextColor(hasSoundFont ? AppUi.TEXT : AppUi.MUTED);
        activeChannel = safeChannel;
        setTuning(coarseSemitones, fineCents);
        setFilterAdsr(
                TimbreFilterAdsrSettingsStore.load(context, safeChannel, "cutoff", TimbreFilterAdsrSettingsStore.DEFAULT_CUTOFF_HZ),
                TimbreFilterAdsrSettingsStore.load(context, safeChannel, "resonance", TimbreFilterAdsrSettingsStore.DEFAULT_RESONANCE),
                TimbreFilterAdsrSettingsStore.load(context, safeChannel, "attack", TimbreFilterAdsrSettingsStore.DEFAULT_ATTACK_SECONDS),
                TimbreFilterAdsrSettingsStore.load(context, safeChannel, "decay", TimbreFilterAdsrSettingsStore.DEFAULT_DECAY_SECONDS),
                TimbreFilterAdsrSettingsStore.load(context, safeChannel, "sustain", TimbreFilterAdsrSettingsStore.DEFAULT_SUSTAIN_PERCENT),
                TimbreFilterAdsrSettingsStore.load(context, safeChannel, "release", TimbreFilterAdsrSettingsStore.DEFAULT_RELEASE_SECONDS));
        setLfo(TimbreLfoSettingsStore.loadRate(context, safeChannel),
                TimbreLfoSettingsStore.loadDepth(context, safeChannel));
        setEffectSends(TimbreEffectSendSettingsStore.loadReverb(context, safeChannel),
                TimbreEffectSendSettingsStore.loadChorus(context, safeChannel));
    }

    private void setTuning(int coarse, int fine) {
        coarseSemitones = SampleTuningSettingsStore.clampCoarse(coarse);
        fineCents = SampleTuningSettingsStore.clampFine(fine);
        transposeKnob.setValue((coarseSemitones + 24) / 48f);
        fineTuneKnob.setValue((fineCents + 100) / 200f);
        updateTuningLabels();
    }

    private void updateTuningLabels() {
        transposeValue.setText(String.format(Locale.getDefault(), "%+d st", coarseSemitones));
        fineTuneValue.setText(String.format(Locale.getDefault(), "%+d cents", fineCents));
        transposeKnob.setContentDescription("Transposição: " + coarseSemitones + " semitons");
        fineTuneKnob.setContentDescription("Afinação fina: " + fineCents + " cents");
    }

    private void dispatchTuning(boolean tracking) {
        if (listener != null) {
            listener.onSampleTuningChanged(activeChannel, coarseSemitones, fineCents, tracking);
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        updateGridColumns(configuration);
    }

    private void updateGridColumns(Configuration configuration) {
        int columns = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ? 2 : 1;
        moduleGrid.removeAllViews();
        moduleGrid.setColumnCount(columns);
        moduleGrid.setRowCount((moduleViews.length + columns - 1) / columns);

        for (int i = 0; i < moduleViews.length; i++) {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / columns), GridLayout.spec(i % columns, 1f));
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(AppUi.dp(context, 4), AppUi.dp(context, 4),
                    AppUi.dp(context, 4), AppUi.dp(context, 4));
            moduleGrid.addView(moduleViews[i], params);
        }
        moduleGrid.requestLayout();
    }
}
