package com.rodolpho.fluidsynth;

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

    public TrackManager(Engine engine) {
        this.engine = engine;
        for (int i = 0; i < tracks.length; i++) tracks[i] = new Track(i);
        engine.setActiveChannel(0);
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

    public synchronized int getActiveChannel() { return activeChannel; }

    public synchronized void select(int channel) {
        checkChannel(channel);
        activeChannel = channel;
        engine.setActiveChannel(channel);
    }

    public synchronized boolean loadSoundFont(int channel, String path, String displayName) {
        checkChannel(channel);
        if (engine.loadSoundFont(channel, path) < 0) return false;
        tracks[channel].setSoundFont(path, displayName);
        return true;
    }

    public synchronized void setEnabled(int channel, boolean enabled) {
        checkChannel(channel);
        tracks[channel].setEnabled(enabled);
        engine.setEnabled(channel, enabled);
    }

    private static void checkChannel(int channel) {
        if (channel < 0 || channel >= 16) throw new IllegalArgumentException("Canal MIDI inválido: " + channel);
    }
}