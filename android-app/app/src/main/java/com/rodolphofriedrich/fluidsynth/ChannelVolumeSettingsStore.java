package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Armazena separadamente o volume de cada canal MIDI. */
public final class ChannelVolumeSettingsStore {
    private static final int DEFAULT_VOLUME = 100;
    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 100;
    private static final String PREFERENCES = "channel_volume";

    private ChannelVolumeSettingsStore() {}

    public static int load(Context context, int channel) {
        checkChannel(channel);
        int saved = preferences(context).getInt(key(channel), DEFAULT_VOLUME);
        return clamp(saved);
    }

    public static void save(Context context, int channel, int volume) {
        checkChannel(channel);
        preferences(context).edit().putInt(key(channel), clamp(volume)).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    private static String key(int channel) {
        return "channel_" + channel;
    }

    private static int clamp(int volume) {
        return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= 16) {
            throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
        }
    }
}
