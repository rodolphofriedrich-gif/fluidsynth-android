package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste os controles MIDI CC independentes de cada canal. */
public final class ChannelMidiSettingsStore {
    private static final String PREFS = "channel_midi_controls";
    public static final int[] CONTROLLERS = {1, 11, 10, 91, 93, 74, 71};
    private static final int[] DEFAULTS = {0, 127, 64, 40, 0, 64, 64};

    private ChannelMidiSettingsStore() {}

    public static int[] load(Context context, int channel) {
        int[] values = new int[CONTROLLERS.length];
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (int i = 0; i < CONTROLLERS.length; i++) {
            values[i] = prefs.getInt(key(channel, CONTROLLERS[i]), DEFAULTS[i]);
        }
        return values;
    }

    public static void save(Context context, int channel, int controller, int value) {
        if (channel < 0 || channel > 15 || !isSupported(controller)) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(key(channel, controller), Math.max(0, Math.min(127, value)))
                .apply();
    }

    private static boolean isSupported(int controller) {
        for (int supported : CONTROLLERS) if (supported == controller) return true;
        return false;
    }

    private static String key(int channel, int controller) {
        return "ch_" + channel + "_cc_" + controller;
    }
}
