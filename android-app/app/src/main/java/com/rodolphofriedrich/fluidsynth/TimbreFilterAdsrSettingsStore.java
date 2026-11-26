package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste filtro e envelope de volume independentemente nos 16 canais MIDI. */
public final class TimbreFilterAdsrSettingsStore {
    public static final float DEFAULT_CUTOFF_HZ = 20000f;
    public static final float DEFAULT_RESONANCE = 0f;
    public static final float DEFAULT_ATTACK_SECONDS = 0.01f;
    public static final float DEFAULT_DECAY_SECONDS = 0.1f;
    public static final float DEFAULT_SUSTAIN_PERCENT = 100f;
    public static final float DEFAULT_RELEASE_SECONDS = 0.1f;
    private static final String PREFS = "timbre_filter_adsr";

    private TimbreFilterAdsrSettingsStore() {}

    public static float load(Context c, int channel, String key, float fallback) {
        checkChannel(channel);
        float min;
        float max;
        switch (key) {
            case "cutoff": min = 20f; max = 20000f; break;
            case "resonance": min = 0f; max = 100f; break;
            case "attack":
            case "decay":
            case "release": min = 0.001f; max = 10f; break;
            case "sustain": min = 0f; max = 100f; break;
            default: throw new IllegalArgumentException("Parâmetro de timbre inválido: " + key);
        }
        if (Float.isNaN(fallback) || Float.isInfinite(fallback)) fallback = min;
        float value = preferences(c).getFloat("channel_" + channel + "_" + key, fallback);
        return clamp(value, min, max);
    }

    public static void save(Context c, int channel, float cutoff, float resonance,
            float attack, float decay, float sustain, float release) {
        checkChannel(channel);
        preferences(c).edit()
                .putFloat("channel_" + channel + "_cutoff", clamp(cutoff, 20f, 20000f))
                .putFloat("channel_" + channel + "_resonance", clamp(resonance, 0f, 100f))
                .putFloat("channel_" + channel + "_attack", clamp(attack, 0.001f, 10f))
                .putFloat("channel_" + channel + "_decay", clamp(decay, 0.001f, 10f))
                .putFloat("channel_" + channel + "_sustain", clamp(sustain, 0f, 100f))
                .putFloat("channel_" + channel + "_release", clamp(release, 0.001f, 10f))
                .apply();
    }

    public static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static SharedPreferences preferences(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= 16) throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
    }
}
