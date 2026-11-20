package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste a intensidade padrão das notas do teclado virtual. */
public final class VelocitySettingsStore {
    private static final String PREFS = "midi_settings";
    private static final String KEY_VELOCITY = "keyboard_velocity";
    public static final int MIN = 1;
    public static final int MAX = 127;
    public static final int DEFAULT = 100;

    private VelocitySettingsStore() {}

    public static int normalize(int velocity) {
        return Math.max(MIN, Math.min(MAX, velocity));
    }

    public static int load(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalize(preferences.getInt(KEY_VELOCITY, DEFAULT));
    }

    public static void save(Context context, int velocity) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_VELOCITY, normalize(velocity))
                .apply();
    }
}
