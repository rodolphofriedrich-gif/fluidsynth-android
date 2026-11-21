package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste e aplica curvas de resposta de velocidade MIDI independentes por canal. */
public final class VelocityCurveSettingsStore {
    public static final int LINEAR = 0;
    public static final int SOFT = 1;
    public static final int HARD = 2;
    public static final int DEFAULT = LINEAR;

    private static final String PREFS = "channel_midi_controls";

    private VelocityCurveSettingsStore() {}

    public static int normalize(int curve) {
        return Math.max(LINEAR, Math.min(HARD, curve));
    }

    public static int load(Context context, int channel) {
        if (channel < 0 || channel > 15) return DEFAULT;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return normalize(prefs.getInt(key(channel), DEFAULT));
    }

    public static void save(Context context, int channel, int curve) {
        if (channel < 0 || channel > 15) return;
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(key(channel), normalize(curve))
                .apply();
    }

    /** Maps note-on velocity 1..127; velocity zero (note-off) remains untouched. */
    public static int apply(int velocity, int curve) {
        int safeVelocity = Math.max(1, Math.min(127, velocity));
        int safeCurve = normalize(curve);
        if (safeCurve == LINEAR) return safeVelocity;

        double exponent = safeCurve == SOFT ? 0.68d : 1.45d;
        double normalized = (safeVelocity - 1) / 126.0d;
        int mapped = 1 + (int) Math.round(126.0d * Math.pow(normalized, exponent));
        return Math.max(1, Math.min(127, mapped));
    }

    private static String key(int channel) {
        return "ch_" + channel + "_velocity_curve";
    }
}
