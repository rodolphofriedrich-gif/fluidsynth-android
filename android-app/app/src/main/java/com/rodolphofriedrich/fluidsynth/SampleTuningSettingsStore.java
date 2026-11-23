package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste transposição e afinação fina independentemente nos 16 canais MIDI. */
public final class SampleTuningSettingsStore {
    public static final int DEFAULT_COARSE_SEMITONES = 0;
    public static final int MIN_COARSE_SEMITONES = -24;
    public static final int MAX_COARSE_SEMITONES = 24;
    public static final int DEFAULT_FINE_CENTS = 0;
    public static final int MIN_FINE_CENTS = -100;
    public static final int MAX_FINE_CENTS = 100;
    private static final String PREFS = "sample_tuning";

    private SampleTuningSettingsStore() {}

    public static int loadCoarse(Context context, int channel) {
        checkChannel(channel);
        return clampCoarse(preferences(context).getInt("channel_" + channel + "_coarse",
                DEFAULT_COARSE_SEMITONES));
    }

    public static int loadFine(Context context, int channel) {
        checkChannel(channel);
        return clampFine(preferences(context).getInt("channel_" + channel + "_fine",
                DEFAULT_FINE_CENTS));
    }

    public static void save(Context context, int channel, int coarseSemitones, int fineCents) {
        checkChannel(channel);
        preferences(context).edit()
                .putInt("channel_" + channel + "_coarse", clampCoarse(coarseSemitones))
                .putInt("channel_" + channel + "_fine", clampFine(fineCents))
                .apply();
    }

    public static int clampCoarse(int value) {
        return Math.max(MIN_COARSE_SEMITONES, Math.min(MAX_COARSE_SEMITONES, value));
    }

    public static int clampFine(int value) {
        return Math.max(MIN_FINE_CENTS, Math.min(MAX_FINE_CENTS, value));
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
