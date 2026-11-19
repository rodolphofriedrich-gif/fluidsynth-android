package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists master output volume independently from MIDI velocity. */
public final class MasterVolumeSettingsStore {
    private static final String PREFS = "master_audio_settings";
    private static final String KEY_VOLUME = "master_volume_percent";
    public static final int DEFAULT = 100;

    private MasterVolumeSettingsStore() {}

    public static int load(Context context) {
        return Math.max(0, Math.min(100, prefs(context).getInt(KEY_VOLUME, DEFAULT)));
    }

    public static void save(Context context, int percent) {
        prefs(context).edit().putInt(KEY_VOLUME, Math.max(0, Math.min(100, percent))).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
