package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
    private Listener listener;
    private boolean silent;

    public EffectsScreen(Context context) {
        this.context = context;
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(24));
        root.setBackgroundColor(AppUi.BG);

        LinearLayout header = AppUi.panel(context, root);
        header.addView(AppUi.sectionLabel(context, "EFEITOS"));
        TextView hint = new TextView(context);
        hint.setText("Aplicados a todo o sintetizador");
        hint.setTextColor(AppUi.MUTED);
        hint.setTextSize(12);
        header.addView(hint);

        LinearLayout tabRow = new LinearLayout(context);
        header.addView(tabRow);
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

        LinearLayout activeRow = new LinearLayout(context);
        activeRow.setGravity(Gravity.CENTER_VERTICAL);
        activeRow.setBackground(AppUi.ripple(context, AppUi.CONTROL, 10, AppUi.BORDER));
        activeRow.setPadding(dp(12), dp(8), dp(8), dp(8));
        TextView activeLabel = new TextView(context);
        activeLabel.setText("Ativo");
        activeLabel.setTextColor(AppUi.TEXT);
        activeLabel.setTextSize(15);
        activeRow.addView(activeLabel, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(context);
        toggle.setThumbTintList(ColorStateList.valueOf(AppUi.ACCENT));
        toggle.setTrackTintList(ColorStateList.valueOf(Color.argb(110, 79, 140, 255)));
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
        page.addView(activeRow);

        TextView presetsTitle = AppUi.sectionLabel(context, context.getString(R.string.effect_presets));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(12);
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
            button.setTextColor(AppUi.TEXT);
            button.setPadding(dp(3), dp(4), dp(3), dp(4));
            button.setBackground(AppUi.ripple(context, AppUi.CONTROL, 9, AppUi.CONTROL_BORDER));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
            params.setMargins(dp(2), dp(4), dp(2), dp(4));
            presetsRow.addView(button, params);
            presetButtons[effect][i] = button;
            button.setOnClickListener(v -> applyPreset(effect, preset));
        }
        page.addView(presetsRow);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);
        for (int param = 0; param < 4; param++) {
            final int parameter = param;
            Spec spec = SPECS[effect][param];
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView name = new TextView(context);
            name.setText(spec.name);
            name.setTextColor(AppUi.MUTED);
            name.setTextSize(12);
            cell.addView(name);

            KnobView knob = new KnobView(context);
            knob.setContentDescription(spec.name);
            knob.setValue(spec.norm(spec.def));
            knob.setListener((view, value, tracking) -> onKnob(effect, parameter, value, tracking));
            cell.addView(knob, new LinearLayout.LayoutParams(dp(88), dp(88)));

            TextView value = new TextView(context);
            value.setText(spec.format(spec.def));
            value.setTextColor(AppUi.TEXT);
            value.setTextSize(14);
            value.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            cell.addView(value);

            values[effect][param] = spec.def;
            knobs[effect][param] = knob;
            labels[effect][param] = value;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(param / 2, 1f), GridLayout.spec(param % 2, 1f));
            params.width = 0;
            grid.addView(cell, params);
        }
        page.addView(grid);

        Button reset = new Button(context);
        reset.setText(R.string.effect_restore_default);
        AppUi.styleButton(context, reset, AppUi.CONTROL, AppUi.TEXT, 48, 14);
        reset.setOnClickListener(v -> restoreDefaults(effect));
        page.addView(reset);
        resetButtons[effect] = reset;
        updateResetButton(effect);
    }

    private void applyPreset(int effect, int preset) {
        float[] values = PRESETS[effect][preset];
        for (int param = 0; param < 4; param++) setParam(effect, param, values[param]);
        notifyPresetApplied(effect);
    }

    private void restoreDefaults(int effect) {
        for (int param = 0; param < 4; param++) setParam(effect, param, SPECS[effect][param].def);
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
        updateResetButton(effect);
        // As APIs globais de efeitos não são realtime-safe: aplica ao soltar o knob.
        if (tracking) return;
        if (listener != null) listener.onParamChanged(effect, param, value);
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
        button.setEnabled(changed);
        button.setAlpha(changed ? 1f : .4f);
    }

    private int resourceId(String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }

    private void selectTab(int selected) {
        for (int i = 0; i < 2; i++) {
            boolean on = i == selected;
            pages[i].setVisibility(on ? View.VISIBLE : View.GONE);
            tabs[i].setBackground(AppUi.rounded(context, on ? AppUi.ACCENT : AppUi.CONTROL, 12, 0));
            tabs[i].setTextColor(on ? Color.WHITE : AppUi.TEXT);
        }
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
