package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Owns the state of the 16 independent MIDI channel strips. */
public final class TrackManager {
    public interface Engine {
        int loadSoundFont(int channel, String path);
        boolean selectPreset(int channel, int bank, int program);
        void setEnabled(int channel, boolean enabled);
        void setActiveChannel(int channel);
    }

    private final Track[] tracks = new Track[16];
    private final Engine engine;
    private final SharedPreferences preferences;
    private final int[] loadGeneration = new int[16];
    private int activeChannel;
    private volatile int restoreFailureCount;

    public TrackManager(Context context, Engine engine) {
        this.engine = engine;
        this.preferences = context.getApplicationContext()
                .getSharedPreferences("track_state", Context.MODE_PRIVATE);
        for (int i = 0; i < tracks.length; i++) tracks[i] = new Track(i);
        activeChannel = Math.max(0, Math.min(15, preferences.getInt("active_channel", 0)));
    }

    /** Restores persisted SoundFonts away from the UI thread and always completes. */
    public void restoreAsync(Executor executor, Runnable completion) {
        executor.execute(() -> {
            int failures = 0;
            try {
                for (int channel = 0; channel < tracks.length; channel++) {
                    String path = preferences.getString("track_" + channel + "_path", null);
                    String name = preferences.getString("track_" + channel + "_name", null);
                    int bank = preferences.getInt("track_" + channel + "_bank", 0);
                    int program = preferences.getInt("track_" + channel + "_program", 0);
                    String presetName = preferences.getString("track_" + channel + "_preset_name", null);
                    boolean enabled = preferences.getBoolean("track_" + channel + "_enabled", true);

                    if (path != null && !path.isEmpty()) {
                        try {
                            if (new File(path).isFile()) {
                                int loaded = engine.loadSoundFont(channel, path);
                                if (loaded >= 0) {
                                    boolean presetSelected = engine.selectPreset(channel, bank, program);
                                    if (!presetSelected) {
                                        // The SF2 is already loaded with its default preset.
                                        // Fall back to that known-good state instead of leaving
                                        // native and Java track state out of sync.
                                        presetSelected = engine.selectPreset(channel, 0, 0);
                                        if (presetSelected) {
                                            bank = 0;
                                            program = 0;
                                            presetName = "Preset padrão";
                                            preferences.edit()
                                                    .putInt("track_" + channel + "_bank", bank)
                                                    .putInt("track_" + channel + "_program", program)
                                                    .putString("track_" + channel + "_preset_name", presetName)
                                                    .apply();
                                            failures++;
                                        }
                                    }

                                    if (presetSelected) {
                                        synchronized (this) {
                                            tracks[channel].setSoundFont(path,
                                                    name == null ? new File(path).getName() : name);
                                            tracks[channel].setPreset(bank, program,
                                                    presetName == null ? "Preset " + program : presetName);
                                        }
                                    } else {
                                        failures++;
                                    }
                                } else {
                                    failures++;
                                }
                            } else {
                                failures++;
                            }
                        } catch (RuntimeException error) {
                            failures++;
                        }
                    }

                    synchronized (this) {
                        tracks[channel].setEnabled(enabled);
                    }
                    try {
                        engine.setEnabled(channel, enabled);
                    } catch (RuntimeException error) {
                        failures++;
                    }
                }
            } finally {
                try {
                    engine.setActiveChannel(getActiveChannel());
                } catch (RuntimeException error) {
                    failures++;
                }
                restoreFailureCount = failures;
                if (completion != null) completion.run();
            }
        });
    }

    public int getRestoreFailureCount() {
        return restoreFailureCount;
    }

    public synchronized Track get(int channel) {
        checkChannel(channel);
        return tracks[channel];
    }

    public synchronized List<Track> getAll() {
        List<Track> result = new ArrayList<>(16);
        Collections.addAll(result, tracks);
        return Collections.unmodifiableList(result);
    }

    public synchronized int getActiveChannel() {
        return activeChannel;
    }

    public void select(int channel) {
        checkChannel(channel);
        synchronized (this) {
            activeChannel = channel;
            preferences.edit().putInt("active_channel", channel).apply();
        }
        engine.setActiveChannel(channel);
    }

    /** The potentially slow native load is deliberately outside this object's monitor. */
    public boolean loadSoundFont(int channel, String path, String displayName) {
        checkChannel(channel);
        final int generation;
        synchronized (this) {
            generation = ++loadGeneration[channel];
        }

        int loadedId = engine.loadSoundFont(channel, path);
        if (loadedId < 0) return false;

        boolean enabled;
        synchronized (this) {
            if (generation != loadGeneration[channel]) return false;
            tracks[channel].setSoundFont(path, displayName);
            tracks[channel].setPreset(0, 0, null);
            tracks[channel].setEnabled(true);
            enabled = tracks[channel].isEnabled();
            preferences.edit()
                    .putString("track_" + channel + "_path", path)
                    .putString("track_" + channel + "_name", displayName)
                    .putInt("track_" + channel + "_bank", 0)
                    .putInt("track_" + channel + "_program", 0)
                    .remove("track_" + channel + "_preset_name")
                    .putBoolean("track_" + channel + "_enabled", enabled)
                    .apply();
        }
        // loadSoundFontForChannel already enables the channel atomically with the new preset.
        // Do not issue a second native call here: it could race with a user mute operation.
        return true;
    }

    public void updatePreset(int channel, int bank, int program, String name) {
        checkChannel(channel);
        synchronized (this) {
            tracks[channel].setPreset(bank, program, name);
            preferences.edit().putInt("track_" + channel + "_bank", bank)
                    .putInt("track_" + channel + "_program", program)
                    .putString("track_" + channel + "_preset_name", name).apply();
        }
    }

    public void setEnabled(int channel, boolean enabled) {
        checkChannel(channel);
        synchronized (this) {
            tracks[channel].setEnabled(enabled);
            preferences.edit()
                    .putBoolean("track_" + channel + "_enabled", enabled)
                    .apply();
        }
        engine.setEnabled(channel, enabled);
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= 16)
            throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
    }
}