package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste velocidade e profundidade do LFO independentemente nos 16 canais. */
public final class TimbreLfoSettingsStore {
    public static final float DEFAULT_RATE_HZ = 8.176f;
    public static final float DEFAULT_DEPTH_CENTS = 0f;
    private static final String PREFS = "timbre_lfo";

    private TimbreLfoSettingsStore() {}

    public static float loadRate(Context context, int channel) {
        checkChannel(channel);
        return clamp(preferences(context).getFloat("channel_" + channel + "_rate", DEFAULT_RATE_HZ), 0.1f, 20f);
    }

    public static float loadDepth(Context context, int channel) {
        checkChannel(channel);
        return clamp(preferences(context).getFloat("channel_" + channel + "_depth", DEFAULT_DEPTH_CENTS), 0f, 100f);
    }

    public static void save(Context context, int channel, float rateHz, float depthCents) {
        checkChannel(channel);
        preferences(context).edit()
                .putFloat("channel_" + channel + "_rate", clamp(rateHz, 0.1f, 20f))
                .putFloat("channel_" + channel + "_depth", clamp(depthCents, 0f, 100f))
                .apply();
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return min;
        return Math.max(min, Math.min(max, value));
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
