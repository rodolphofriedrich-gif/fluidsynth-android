package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;

/** Persiste controles MIDI CC e parâmetros RPN independentes por canal. */
public final class ChannelMidiSettingsStore {
    private static final String PREFS = "channel_midi_controls";
    public static final int[] CONTROLLERS = {1, 11, 10, 91, 93, 74, 71};
    private static final int[] DEFAULTS = {0, 127, 64, 0, 0, 64, 64};
    public static final int DEFAULT_PITCH_BEND_RANGE = 2;
    public static final int MIN_PITCH_BEND_RANGE = 1;
    public static final int MAX_PITCH_BEND_RANGE = 24;

    private ChannelMidiSettingsStore() {}

    public static int[] load(Context context, int channel) {
        checkChannel(channel);
        int[] values = new int[CONTROLLERS.length];
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (int i = 0; i < CONTROLLERS.length; i++) {
            int controller = CONTROLLERS[i];
            if (controller == 91) {
                values[i] = TimbreEffectSendSettingsStore.loadReverb(context, channel);
            } else if (controller == 93) {
                values[i] = TimbreEffectSendSettingsStore.loadChorus(context, channel);
            } else {
                values[i] = clampMidiValue(prefs.getInt(key(channel, controller), DEFAULTS[i]));
            }
        }
        return values;
    }

    public static void save(Context context, int channel, int controller, int value) {
        if (channel < 0 || channel > 15 || !isSupported(controller)) return;
        int safeValue = clampMidiValue(value);
        if (controller == 91) {
            TimbreEffectSendSettingsStore.save(context, channel, safeValue,
                    TimbreEffectSendSettingsStore.loadChorus(context, channel));
            return;
        }
        if (controller == 93) {
            TimbreEffectSendSettingsStore.save(context, channel,
                    TimbreEffectSendSettingsStore.loadReverb(context, channel), safeValue);
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(key(channel, controller), safeValue).apply();
    }

    public static int loadPitchBendRange(Context context, int channel) {
        if (channel < 0 || channel > 15) return DEFAULT_PITCH_BEND_RANGE;
        int value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(pitchBendKey(channel), DEFAULT_PITCH_BEND_RANGE);
        return clampPitchBendRange(value);
    }

    public static void savePitchBendRange(Context context, int channel, int semitones) {
        if (channel < 0 || channel > 15) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(pitchBendKey(channel), clampPitchBendRange(semitones))
                .apply();
    }

    public static int clampPitchBendRange(int semitones) {
        return Math.max(MIN_PITCH_BEND_RANGE,
                Math.min(MAX_PITCH_BEND_RANGE, semitones));
    }

    private static boolean isSupported(int controller) {
        for (int supported : CONTROLLERS) if (supported == controller) return true;
        return false;
    }

    private static int clampMidiValue(int value) {
        return Math.max(0, Math.min(127, value));
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
        }
    }

    private static String key(int channel, int controller) {
        return "ch_" + channel + "_cc_" + controller;
    }

    private static String pitchBendKey(int channel) {
        return "ch_" + channel + "_pitch_bend_range";
    }
}
