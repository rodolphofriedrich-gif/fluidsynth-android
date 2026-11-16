package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns the state of the 16 independent MIDI channel strips. */
public final class TrackManager {
    public interface Engine {
        int loadSoundFont(int channel, String path);
        void setEnabled(int channel, boolean enabled);
        void setActiveChannel(int channel);
    }
    private final Track[] tracks = new Track[16];
    private final Engine engine;
    private int activeChannel;
    private final SharedPreferences preferences;
    public TrackManager(Context context, Engine engine) {
        this.engine = engine;
        this.preferences = context.getApplicationContext().getSharedPreferences("track_state", Context.MODE_PRIVATE);
        for (int i = 0; i < tracks.length; i++) tracks[i] = new Track(i);
        restore();
    }
    private void restore() {
        activeChannel = Math.max(0, Math.min(15, preferences.getInt("active_channel", 0)));
        for (int i = 0; i < 16; i++) {
            String path = preferences.getString("track_" + i + "_path", null);
            String name = preferences.getString("track_" + i + "_name", null);
            boolean enabled = preferences.getBoolean("track_" + i + "_enabled", true);
            if (path != null && !path.isEmpty() && new java.io.File(path).isFile()) {
                if (engine.loadSoundFont(i, path) >= 0) tracks[i].setSoundFont(path, name == null ? new java.io.File(path).getName() : name);
            }
            tracks[i].setEnabled(enabled);
            engine.setEnabled(i, enabled);
        }
        engine.setActiveChannel(activeChannel);
    }
    private void persistTrack(int channel) {
        Track item = tracks[channel];
        preferences.edit()
            .putString("track_" + channel + "_path", item.getSoundFontPath())
            .putString("track_" + channel + "_name", item.getSoundFontName())
            .putBoolean("track_" + channel + "_enabled", item.isEnabled())
            .putInt("active_channel", activeChannel)
            .apply();
    }
    public synchronized Track get(int channel) { checkChannel(channel); return tracks[channel]; }
    public synchronized List<Track> getAll() {
        List<Track> result = new ArrayList<>(16);
        Collections.addAll(result, tracks);
        return Collections.unmodifiableList(result);
    }
    public synchronized int getActiveChannel() { return activeChannel; }
    public synchronized void select(int channel) {
        checkChannel(channel); activeChannel = channel; engine.setActiveChannel(channel); preferences.edit().putInt("active_channel", channel).apply();
    }
    public synchronized boolean loadSoundFont(int channel, String path, String displayName) {
        checkChannel(channel);
        if (engine.loadSoundFont(channel, path) < 0) return false;
        tracks[channel].setSoundFont(path, displayName);
        tracks[channel].setEnabled(true);
        engine.setEnabled(channel, true);
        persistTrack(channel);
        return true;
    }
    public synchronized void setEnabled(int channel, boolean enabled) {
        checkChannel(channel); tracks[channel].setEnabled(enabled); engine.setEnabled(channel, enabled); persistTrack(channel);
    }
    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= 16) throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
    }
}