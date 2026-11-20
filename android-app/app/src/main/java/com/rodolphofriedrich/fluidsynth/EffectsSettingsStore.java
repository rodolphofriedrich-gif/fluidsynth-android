package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistência isolada dos parâmetros e estados dos efeitos globais. */
public final class EffectsSettingsStore {
    private static final String PREFS = "audio_effects";
    private static final String[] NAMES = {"reverb", "chorus"};
    private static final float[][] MINIMUMS = {
        {0f, 0f, 0f, 0f},
        {0f, 0f, .1f, 0f}
    };
    private static final float[][] MAXIMUMS = {
        {1.2f, 1f, 100f, 1f},
        {99f, 10f, 5f, 21f}
    };
    private static final float[][] DEFAULTS = {
        {.2f, 0f, .5f, .9f},
        {3f, 2f, .3f, 8f}
    };
    private static final int[][] DECIMALS = {
        {2, 2, 1, 2},
        {0, 1, 1, 1}
    };

    private EffectsSettingsStore() {}

    public static float[] defaults(int effect) {
        checkEffect(effect);
        return DEFAULTS[effect].clone();
    }

    public static float minimum(int effect, int parameter) {
        checkParameter(effect, parameter);
        return MINIMUMS[effect][parameter];
    }

    public static float maximum(int effect, int parameter) {
        checkParameter(effect, parameter);
        return MAXIMUMS[effect][parameter];
    }

    public static int decimals(int effect, int parameter) {
        checkParameter(effect, parameter);
        return DECIMALS[effect][parameter];
    }

    public static float normalize(int effect, int parameter, float value) {
        checkParameter(effect, parameter);
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            value = DEFAULTS[effect][parameter];
        }
        float safe = Math.max(MINIMUMS[effect][parameter],
                Math.min(MAXIMUMS[effect][parameter], value));
        float precision = (float) Math.pow(10, DECIMALS[effect][parameter]);
        return Math.round(safe * precision) / precision;
    }

    public static float[] loadValues(Context context, int effect) {
        checkEffect(effect);
        float[] values = defaults(effect);
        SharedPreferences preferences = prefs(context);
        for (int parameter = 0; parameter < values.length; parameter++) {
            String key = NAMES[effect] + "_" + parameter;
            values[parameter] = normalize(effect, parameter,
                    preferences.getFloat(key, values[parameter]));
        }
        return values;
    }

    public static void saveValues(Context context, int effect, float[] values) {
        checkEffect(effect);
        if (values == null || values.length != 4) {
            throw new IllegalArgumentException("São necessários quatro parâmetros do efeito");
        }
        SharedPreferences.Editor editor = prefs(context).edit();
        for (int parameter = 0; parameter < values.length; parameter++) {
            editor.putFloat(NAMES[effect] + "_" + parameter,
                    normalize(effect, parameter, values[parameter]));
        }
        editor.apply();
    }

    public static boolean loadActive(Context context, int effect) {
        checkEffect(effect);
        return prefs(context).getBoolean(NAMES[effect] + "_active", true);
    }

    public static void saveActive(Context context, int effect, boolean active) {
        checkEffect(effect);
        prefs(context).edit().putBoolean(NAMES[effect] + "_active", active).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void checkEffect(int effect) {
        if (effect < 0 || effect >= NAMES.length) {
            throw new IllegalArgumentException("Efeito inválido: " + effect);
        }
    }

    private static void checkParameter(int effect, int parameter) {
        checkEffect(effect);
        if (parameter < 0 || parameter >= 4) {
            throw new IllegalArgumentException("Parâmetro inválido: " + parameter);
        }
    }
}
