package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste os envios MIDI de Reverb (CC91) e Chorus (CC93) por canal. */
public final class TimbreEffectSendSettingsStore {
    public static final int DEFAULT_REVERB_SEND = 0;
    public static final int DEFAULT_CHORUS_SEND = 0;
    private static final String PREFS = "timbre_effect_sends";

    private TimbreEffectSendSettingsStore() {}

    public static int loadReverb(Context context, int channel) {
        checkChannel(channel);
        return loadOrMigrate(context, channel, "reverb", 91, DEFAULT_REVERB_SEND);
    }

    public static int loadChorus(Context context, int channel) {
        checkChannel(channel);
        return loadOrMigrate(context, channel, "chorus", 93, DEFAULT_CHORUS_SEND);
    }

    /** Preserves values saved by the previous MIDI CC page before Phase 4 introduced this store. */
    private static int loadOrMigrate(Context context, int channel, String effect,
            int controller, int fallback) {
        SharedPreferences current = preferences(context);
        String currentKey = "channel_" + channel + "_" + effect;
        if (current.contains(currentKey)) return clamp(current.getInt(currentKey, fallback));

        SharedPreferences legacy = context.getApplicationContext()
                .getSharedPreferences("channel_midi_controls", Context.MODE_PRIVATE);
        String legacyKey = "ch_" + channel + "_cc_" + controller;
        int value = clamp(legacy.getInt(legacyKey, fallback));
        current.edit().putInt(currentKey, value).apply();
        return value;
    }

    public static void save(Context context, int channel, int reverbSend, int chorusSend) {
        checkChannel(channel);
        preferences(context).edit()
                .putInt("channel_" + channel + "_reverb", clamp(reverbSend))
                .putInt("channel_" + channel + "_chorus", clamp(chorusSend))
                .apply();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(127, value));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= 16) {
            throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
        }
    }
}
