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
            "Frequência (Rate)\nIntensidade (Depth)",
            "Reverb · Chorus"
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
        for (int i = 1; i < MODULE_TITLES.length; i++) {
            moduleViews[i] = createModule(MODULE_TITLES[i], MODULE_DETAILS[i]);
        }
        root.addView(moduleGrid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        updateGridColumns(context.getResources().getConfiguration());

        TextView notice = new TextView(context);
        notice.setText("Transposição e afinação fina são independentes por canal. Os demais módulos serão habilitados nas próximas etapas.");
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
