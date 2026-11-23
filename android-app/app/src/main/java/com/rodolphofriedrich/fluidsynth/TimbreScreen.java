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

/**
 * Estrutura visual inicial do Editor de Timbre.
 *
 * Esta fase não cria nem altera parâmetros sonoros: prepara a navegação e os
 * módulos que receberão controles em etapas futuras.
 */
public final class TimbreScreen {
    public final ScrollView scrollView;

    private final Context context;
    private final TextView channelLabel;
    private final TextView instrumentLabel;
    private final GridLayout moduleGrid;

    private static final String[] MODULE_TITLES = {
            "FONTE SONORA  ·  SAMPLE",
            "FILTRO  ·  FILTER",
            "ENVELOPE",
            "LFO",
            "EFEITOS  ·  EFFECTS"
    };

    private static final String[] MODULE_DETAILS = {
            "Transposição (Transpose)\nAfinação fina (Fine Tuning)",
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
        for (int i = 0; i < MODULE_TITLES.length; i++) {
            moduleViews[i] = createModule(MODULE_TITLES[i], MODULE_DETAILS[i]);
        }
        root.addView(moduleGrid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        updateGridColumns(context.getResources().getConfiguration());

        TextView notice = new TextView(context);
        notice.setText("Estrutura inicial do editor. Os controles sonoros serão habilitados nas próximas etapas, após validação individual.");
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

    public void setChannel(int channel, String instrument, boolean hasSoundFont) {
        int safeChannel = Math.max(0, Math.min(15, channel));
        channelLabel.setText(String.format(java.util.Locale.ROOT, "CANAL %02d", safeChannel + 1));
        instrumentLabel.setText(instrument == null || instrument.trim().isEmpty()
                ? "Nenhum instrumento selecionado" : instrument);
        instrumentLabel.setTextColor(hasSoundFont ? AppUi.TEXT : AppUi.MUTED);
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
                    GridLayout.spec(i / columns), GridLayout.spec(i % columns));
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(AppUi.dp(context, 4), AppUi.dp(context, 4),
                    AppUi.dp(context, 4), AppUi.dp(context, 4));
            moduleGrid.addView(moduleViews[i], params);
        }
        moduleGrid.requestLayout();
    }
}
