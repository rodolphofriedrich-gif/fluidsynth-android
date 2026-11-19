package com.rodolphofriedrich.fluidsynth;

/**
 * Applies channel mute and solo without changing SoundFont or preset state.
 * TrackManager remains the source of persisted per-channel mute preferences.
 */
public final class ChannelMixController {
    private final TrackManager tracks;
    private final NativeBridge nativeBridge;
    private int soloChannel = -1;

    public ChannelMixController(TrackManager tracks, NativeBridge nativeBridge) {
        if (tracks == null || nativeBridge == null) {
            throw new IllegalArgumentException("TrackManager e NativeBridge são obrigatórios");
        }
        this.tracks = tracks;
        this.nativeBridge = nativeBridge;
    }

    public synchronized void setMuted(int channel, boolean muted) {
        tracks.setEnabled(channel, !muted);
        applyIfSoloActive();
    }

    public synchronized void toggleSolo(int channel) {
        if (channel < 0 || channel >= 16) throw new IllegalArgumentException("Canal MIDI inválido");
        soloChannel = soloChannel == channel ? -1 : channel;
        applyAll();
    }

    public synchronized boolean isSolo(int channel) {
        return soloChannel == channel;
    }

    public synchronized boolean hasSolo() {
        return soloChannel >= 0;
    }

    public synchronized void applyIfSoloActive() {
        if (soloChannel >= 0) applyAll();
    }

    private void applyAll() {
        for (int channel = 0; channel < 16; channel++) {
            boolean enabled = tracks.get(channel).isEnabled()
                    && (soloChannel < 0 || soloChannel == channel);
            nativeBridge.nativeSetTrackEnabled(channel, enabled);
        }
    }
}
