package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

/** Interface global de Reverb e Chorus; não acessa o motor nativo. */
public final class EffectsScreen {
    public static final int REVERB = 0;
    public static final int CHORUS = 1;

    public interface Listener {
        void onEffectActive(int effect, boolean active);
        void onParamChanged(int effect, int param, float value);
        void onPresetApplied(int effect);
    }

    private static final class Spec {
        final String name;
        final String unit;
        final float min;
        final float max;
        final float def;
        final int decimals;

        Spec(String name, String unit, float min, float max, float def, int decimals) {
            this.name = name;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.def = def;
            this.decimals = decimals;
        }

        float toValue(float normalized) {
            float value = min + (max - min) * Math.max(0f, Math.min(1f, normalized));
            float precision = (float) Math.pow(10, decimals);
            return Math.round(value * precision) / precision;
        }

        float norm(float value) {
            return Math.max(0f, Math.min(1f, (value - min) / (max - min)));
        }

        String format(float value) {
            return String.format(Locale.getDefault(), "%." + decimals + "f%s", value, unit);
        }
    }

    private static final String[] NAMES = {"REVERB", "CHORUS"};
    private static final Spec[][] SPECS = {
        {
            new Spec("Tamanho da sala", "", EffectsSettingsStore.minimum(REVERB, 0), EffectsSettingsStore.maximum(REVERB, 0), EffectsSettingsStore.defaults(REVERB)[0], EffectsSettingsStore.decimals(REVERB, 0)),
            new Spec("Amortecimento", "", EffectsSettingsStore.minimum(REVERB, 1), EffectsSettingsStore.maximum(REVERB, 1), EffectsSettingsStore.defaults(REVERB)[1], EffectsSettingsStore.decimals(REVERB, 1)),
            new Spec("Largura", "", EffectsSettingsStore.minimum(REVERB, 2), EffectsSettingsStore.maximum(REVERB, 2), EffectsSettingsStore.defaults(REVERB)[2], EffectsSettingsStore.decimals(REVERB, 2)),
            new Spec("Nível", "", EffectsSettingsStore.minimum(REVERB, 3), EffectsSettingsStore.maximum(REVERB, 3), EffectsSettingsStore.defaults(REVERB)[3], EffectsSettingsStore.decimals(REVERB, 3))
        },
        {
            new Spec("Vozes", "", EffectsSettingsStore.minimum(CHORUS, 0), EffectsSettingsStore.maximum(CHORUS, 0), EffectsSettingsStore.defaults(CHORUS)[0], EffectsSettingsStore.decimals(CHORUS, 0)),
            new Spec("Nível", "", EffectsSettingsStore.minimum(CHORUS, 1), EffectsSettingsStore.maximum(CHORUS, 1), EffectsSettingsStore.defaults(CHORUS)[1], EffectsSettingsStore.decimals(CHORUS, 1)),
            new Spec("Velocidade", " Hz", EffectsSettingsStore.minimum(CHORUS, 2), EffectsSettingsStore.maximum(CHORUS, 2), EffectsSettingsStore.defaults(CHORUS)[2], EffectsSettingsStore.decimals(CHORUS, 2)),
            new Spec("Profundidade", " ms", EffectsSettingsStore.minimum(CHORUS, 3), EffectsSettingsStore.maximum(CHORUS, 3), EffectsSettingsStore.defaults(CHORUS)[3], EffectsSettingsStore.decimals(CHORUS, 3))
        }
    };

    private static final String[][] PRESET_NAMES = {
        {"preset_room_small", "preset_hall", "preset_plate"},
        {"preset_chorus_light", "preset_chorus_deep"}
    };

    private static final float[][][] PRESETS = {
        {
            {.16f, .28f, .5f, .68f},
            {.92f, .32f, 100f, .9f},
            {.58f, .14f, 72f, .82f}
        },
        {
            {3f, 1.4f, .25f, 5f},
            {8f, 4.5f, .65f, 15f}
        }
    };

    public final ScrollView scrollView;
    private final Context context;
    private final float[][] values = new float[2][4];
    private final KnobView[][] knobs = new KnobView[2][4];
    private final TextView[][] labels = new TextView[2][4];
    private final Switch[] active = new Switch[2];
    private final View[] pages = new View[2];
    private final Button[] tabs = new Button[2];
    private final Button[] resetButtons = new Button[2];
    private final Button[][] presetButtons = new Button[2][];
    private final int[] selectedPreset = {-1, -1};
    private Listener listener;
    private boolean silent;

    public EffectsScreen(Context context) {
        this.context = context;
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(20));
        root.setBackgroundColor(AppUi.BG);

        LinearLayout header = AppUi.panel(context, root);
        TextView title = AppUi.sectionLabel(context, "PROCESSAMENTO DE ÁUDIO");
        title.setTextColor(VisualTheme.EFFECTS);
        header.addView(title);
        TextView hint = new TextView(context);
        hint.setText("Efeitos globais aplicados a todos os canais");
        hint.setTextColor(AppUi.MUTED);
        hint.setTextSize(12);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(4);
        header.addView(hint, hintParams);

        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tabRowParams.topMargin = dp(12);
        header.addView(tabRow, tabRowParams);
        for (int i = 0; i < 2; i++) {
            final int effect = i;
            Button tab = new Button(context);
            tab.setText(NAMES[i]);
            AppUi.styleButton(context, tab, AppUi.CONTROL, AppUi.TEXT, 44, 13);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
            params.setMargins(dp(3), 0, dp(3), 0);
            tabRow.addView(tab, params);
            tabs[i] = tab;
            tab.setOnClickListener(v -> selectTab(effect));
        }

        for (int i = 0; i < 2; i++) buildPage(root, i);
        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(root);
        selectTab(REVERB);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public float[] values(int effect) {
        return values[effect].clone();
    }

    public void setParam(int effect, int param, float value) {
        checkParameter(effect, param);
        Spec spec = SPECS[effect][param];
        float safe = EffectsSettingsStore.normalize(effect, param, value);
        values[effect][param] = safe;
        knobs[effect][param].setValue(spec.norm(safe));
        labels[effect][param].setText(spec.format(safe));
        selectedPreset[effect] = -1;
        updatePresetButtons(effect);
        updateResetButton(effect);
    }

    public void setActive(int effect, boolean enabled) {
        checkEffect(effect);
        silent = true;
        active[effect].setChecked(enabled);
        silent = false;
        for (KnobView knob : knobs[effect]) knob.setEnabled(enabled);
    }

    private static void checkEffect(int effect) {
        if (effect < 0 || effect >= SPECS.length) {
            throw new IllegalArgumentException("Efeito inválido: " + effect);
        }
    }

    private static void checkParameter(int effect, int param) {
        checkEffect(effect);
        if (param < 0 || param >= SPECS[effect].length) {
            throw new IllegalArgumentException("Parâmetro inválido: " + param);
        }
    }

    private void buildPage(LinearLayout root, int effect) {
        LinearLayout page = AppUi.panel(context, root);
        pages[effect] = page;

        TextView pageTitle = new TextView(context);
        pageTitle.setText(effect == REVERB ? "Ambiência e espaço" : "Espessura e movimento");
        AppUi.styleHeading(pageTitle);
        pageTitle.setTextSize(17);
        page.addView(pageTitle);

        LinearLayout activeRow = new LinearLayout(context);
        activeRow.setGravity(Gravity.CENTER_VERTICAL);
        activeRow.setBackground(AppUi.ripple(context, AppUi.CONTROL, 12, AppUi.CONTROL_BORDER));
        activeRow.setPadding(dp(14), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams activeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        activeParams.topMargin = dp(12);
        page.addView(activeRow, activeParams);

        LinearLayout activeText = new LinearLayout(context);
        activeText.setOrientation(LinearLayout.VERTICAL);
        TextView activeLabel = new TextView(context);
        activeLabel.setText(effect == REVERB ? "Reverb ativo" : "Chorus ativo");
        activeLabel.setTextColor(AppUi.TEXT);
        activeLabel.setTextSize(14);
        activeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        activeText.addView(activeLabel);
        TextView activeHint = new TextView(context);
        activeHint.setText("Aplicado globalmente");
        activeHint.setTextColor(AppUi.MUTED);
        activeHint.setTextSize(10);
        activeText.addView(activeHint);
        activeRow.addView(activeText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(context);
        toggle.setThumbTintList(new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {VisualTheme.EFFECTS, AppUi.MUTED}));
        toggle.setTrackTintList(new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_checked}, new int[] {}},
                new int[] {Color.argb(95, 152, 116, 232), AppUi.CONTROL_BORDER}));
        toggle.setChecked(true);
        toggle.setContentDescription("Ativar " + NAMES[effect]);
        activeRow.addView(toggle);
        active[effect] = toggle;
        toggle.setOnCheckedChangeListener((button, enabled) -> {
            for (KnobView knob : knobs[effect]) if (knob != null) knob.setEnabled(enabled);
            if (!silent && listener != null) listener.onEffectActive(effect, enabled);
        });
        activeRow.setClickable(true);
        activeRow.setFocusable(true);
        activeRow.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));

        TextView presetsTitle = AppUi.sectionLabel(context, "PREDEFINIÇÕES");
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(18);
        page.addView(presetsTitle, titleParams);

        LinearLayout presetsRow = new LinearLayout(context);
        presetsRow.setGravity(Gravity.CENTER_VERTICAL);
        presetButtons[effect] = new Button[PRESETS[effect].length];
        for (int i = 0; i < PRESETS[effect].length; i++) {
            final int preset = i;
            Button button = new Button(context);
            button.setText(context.getString(resourceId(PRESET_NAMES[effect][i])));
            button.setTextSize(11);
            button.setAllCaps(false);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setMinHeight(0);
            button.setMinimumHeight(0);
            button.setPadding(dp(4), dp(4), dp(4), dp(4));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1f);
            params.setMargins(dp(3), dp(6), dp(3), dp(2));
            presetsRow.addView(button, params);
            presetButtons[effect][i] = button;
            button.setOnClickListener(v -> applyPreset(effect, preset));
        }
        page.addView(presetsRow);
        updatePresetButtons(effect);

        GridLayout grid = new GridLayout(context);
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        grid.setColumnCount(landscape ? 4 : 2);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = dp(16);
        page.addView(grid, gridParams);

        for (int param = 0; param < 4; param++) {
            final int parameter = param;
            Spec spec = SPECS[effect][param];
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(dp(4), dp(8), dp(4), dp(10));

            TextView name = new TextView(context);
            name.setText(spec.name);
            name.setTextColor(AppUi.MUTED);
            name.setTextSize(11);
            name.setGravity(Gravity.CENTER);
            cell.addView(name);

            KnobView knob = new KnobView(context);
            knob.setContentDescription(spec.name);
            knob.setValue(spec.norm(spec.def));
            knob.setListener((view, value, tracking) -> onKnob(effect, parameter, value, tracking));
            LinearLayout.LayoutParams knobParams = new LinearLayout.LayoutParams(dp(92), dp(92));
            knobParams.topMargin = dp(2);
            cell.addView(knob, knobParams);

            TextView value = new TextView(context);
            value.setText(spec.format(spec.def));
            AppUi.styleValue(value);
            value.setTextSize(14);
            value.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            valueParams.topMargin = dp(2);
            cell.addView(value, valueParams);

            values[effect][param] = spec.def;
            knobs[effect][param] = knob;
            labels[effect][param] = value;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(param / (landscape ? 4 : 2), 1f),
                    GridLayout.spec(param % (landscape ? 4 : 2), 1f));
            params.width = 0;
            grid.addView(cell, params);
        }

        Button reset = new Button(context);
        reset.setText(R.string.effect_restore_default);
        reset.setAllCaps(false);
        reset.setTextSize(12);
        reset.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        reset.setTextColor(VisualTheme.EFFECTS);
        reset.setBackground(AppUi.ripple(context, AppUi.PANEL, 12, VisualTheme.EFFECTS));
        reset.setMinHeight(0);
        reset.setMinimumHeight(0);
        reset.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        resetParams.topMargin = dp(8);
        page.addView(reset, resetParams);
        reset.setOnClickListener(v -> restoreDefaults(effect));
        resetButtons[effect] = reset;
        updateResetButton(effect);
    }

    private void applyPreset(int effect, int preset) {
        float[] presetValues = PRESETS[effect][preset];
        for (int param = 0; param < 4; param++) {
            setParam(effect, param, presetValues[param]);
        }
        selectedPreset[effect] = preset;
        updatePresetButtons(effect);
        notifyPresetApplied(effect);
    }

    private void restoreDefaults(int effect) {
        for (int param = 0; param < 4; param++) {
            setParam(effect, param, SPECS[effect][param].def);
        }
        selectedPreset[effect] = -1;
        updatePresetButtons(effect);
        notifyPresetApplied(effect);
    }

    private void notifyPresetApplied(int effect) {
        if (listener != null) listener.onPresetApplied(effect);
    }

    private void onKnob(int effect, int param, float normalized, boolean tracking) {
        float value = EffectsSettingsStore.normalize(effect, param,
                SPECS[effect][param].toValue(normalized));
        labels[effect][param].setText(SPECS[effect][param].format(value));
        values[effect][param] = value;
        selectedPreset[effect] = -1;
        updatePresetButtons(effect);
        updateResetButton(effect);
        // As APIs globais de efeitos não são realtime-safe: aplica ao soltar o knob.
        if (tracking) return;
        if (listener != null) listener.onParamChanged(effect, param, value);
    }

    private void updatePresetButtons(int effect) {
        Button[] buttons = presetButtons[effect];
        if (buttons == null) return;
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = i == selectedPreset[effect];
            buttons[i].setTextColor(selected ? Color.WHITE : AppUi.TEXT);
            buttons[i].setBackground(AppUi.ripple(context,
                    selected ? Color.argb(58, 152, 116, 232) : AppUi.CONTROL,
                    10, selected ? VisualTheme.EFFECTS : AppUi.CONTROL_BORDER));
            buttons[i].setSelected(selected);
            buttons[i].setContentDescription(buttons[i].getText()
                    + (selected ? ", predefinição selecionada" : ""));
        }
    }

    private void updateResetButton(int effect) {
        Button button = resetButtons[effect];
        if (button == null) return;
        boolean changed = false;
        for (int param = 0; param < 4; param++) {
            if (Math.abs(values[effect][param] - SPECS[effect][param].def) > .0001f) {
                changed = true;
                break;
            }
        }
        AppUi.setButtonEnabled(button, changed);
        button.setAlpha(changed ? 1f : .62f);
    }

    private int resourceId(String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }

    private void selectTab(int selected) {
        for (int i = 0; i < 2; i++) {
            boolean on = i == selected;
            pages[i].setVisibility(on ? View.VISIBLE : View.GONE);
            tabs[i].setBackground(AppUi.ripple(context,
                    on ? Color.argb(58, 152, 116, 232) : AppUi.CONTROL,
                    12, on ? VisualTheme.EFFECTS : AppUi.CONTROL_BORDER));
            tabs[i].setTextColor(on ? VisualTheme.EFFECTS : AppUi.TEXT);
            tabs[i].setSelected(on);
        }
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
