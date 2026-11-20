package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists the selected maximum number of simultaneous FluidSynth voices. */
public final class PolyphonySettingsStore {
    private static final String PREFS = "audio_settings";
    private static final String KEY_POLYPHONY = "polyphony";
    public static final int MIN = 8;
    public static final int MAX = 2048;
    public static final int STEP = 8;
    public static final int DEFAULT = 512;

    private PolyphonySettingsStore() {}

    public static int normalize(int voices) {
        int bounded = Math.max(MIN, Math.min(MAX, voices));
        int steps = Math.round((bounded - MIN) / (float) STEP);
        return MIN + steps * STEP;
    }

    public static int load(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalize(preferences.getInt(KEY_POLYPHONY, DEFAULT));
    }

    public static void save(Context context, int voices) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_POLYPHONY, normalize(voices))
                .apply();
    }
}