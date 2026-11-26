package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste os envios MIDI de Reverb (CC91) e Chorus (CC93) por canal. */
public final class TimbreEffectSendSettingsStore {
    public static final int DEFAULT_REVERB_SEND = 40;
    public static final int DEFAULT_CHORUS_SEND = 0;
    private static final String PREFS = "timbre_effect_sends";

    private TimbreEffectSendSettingsStore() {}

    public static int loadReverb(Context context, int channel) {
        checkChannel(channel);
        return preferences(context).getInt("channel_" + channel + "_reverb", DEFAULT_REVERB_SEND);
    }

    public static int loadChorus(Context context, int channel) {
        checkChannel(channel);
        return preferences(context).getInt("channel_" + channel + "_chorus", DEFAULT_CHORUS_SEND);
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
