package com.rodolphofriedrich.fluidsynth;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Single entry point for MIDI events coming from the virtual keyboard,
 * USB MIDI and BLE MIDI.
 *
 * Tracks notes and sustain by input source so a disconnected controller
 * cannot leave notes held in the shared FluidSynth instance.
 */
public final class MidiRouter implements AutoCloseable {
    private final NativeBridge nativeBridge;
    private final Map<Object, SourceState> sources = new IdentityHashMap<>();
    @SuppressWarnings("unchecked")
    private final IdentityHashMap<Object, Boolean>[] sustainOwners =
            new IdentityHashMap[16];
    private boolean closed;

    public MidiRouter(NativeBridge nativeBridge) {
        if (nativeBridge == null) {
            throw new IllegalArgumentException("NativeBridge nulo");
        }
        this.nativeBridge = nativeBridge;
        for (int channel = 0; channel < sustainOwners.length; channel++) {
            sustainOwners[channel] = new IdentityHashMap<>();
        }
    }

    /** Routes one MIDI channel message while updating source-owned note state. */
    public synchronized void send(Object source, int status, int data1, int data2) {
        if (closed || source == null || status < 0x80 || status > 0xEF) return;

        int command = status & 0xF0;
        int channel = status & 0x0F;
        int first = clampMidiValue(data1);
        int second = clampMidiValue(data2);
        SourceState state = sources.get(source);
        if (state == null) {
            state = new SourceState();
            sources.put(source, state);
        }

        if (command == 0x90 && second > 0) {
            state.activeNotes[channel][first]++;
            nativeBridge.nativeMidiEvent(status, first, second);
            return;
        }

        if (command == 0x80 || (command == 0x90 && second == 0)) {
            if (state.activeNotes[channel][first] > 0) {
                state.activeNotes[channel][first]--;
            }
            nativeBridge.nativeMidiEvent(status, first, second);
            return;
        }

        if (command == 0xB0 && first == 64) {
            updateSustain(source, state, channel, second);
            return;
        }

        if (command == 0xB0 && (first == 120 || first == 123)) {
            clearChannelNotes(channel);
            nativeBridge.nativeMidiEvent(status, first, second);
            return;
        }

        if (command == 0xB0 && first == 121) {
            clearChannelSustain(channel);
            nativeBridge.nativeMidiEvent(status, first, second);
            return;
        }

        nativeBridge.nativeMidiEvent(status, first, second);
    }

    private void updateSustain(Object source, SourceState state, int channel, int value) {
        IdentityHashMap<Object, Boolean> owners = sustainOwners[channel];
        if (value >= 64) {
            if (!state.sustain[channel]) {
                state.sustain[channel] = true;
                boolean firstOwner = owners.isEmpty();
                owners.put(source, Boolean.TRUE);
                if (firstOwner) nativeBridge.nativeMidiEvent(0xB0 | channel, 64, value);
            } else if (owners.size() == 1) {
                nativeBridge.nativeMidiEvent(0xB0 | channel, 64, value);
            }
            return;
        }

        if (state.sustain[channel]) {
            state.sustain[channel] = false;
            owners.remove(source);
            if (owners.isEmpty()) nativeBridge.nativeMidiEvent(0xB0 | channel, 64, 0);
        } else if (owners.isEmpty()) {
            nativeBridge.nativeMidiEvent(0xB0 | channel, 64, 0);
        }
    }

    /** Releases all notes and sustain still owned by a source being disconnected. */
    public synchronized void releaseSource(Object source) {
        if (source == null) return;
        SourceState state = sources.remove(source);
        if (state == null) return;

        for (int channel = 0; channel < 16; channel++) {
            if (state.sustain[channel]) {
                state.sustain[channel] = false;
                IdentityHashMap<Object, Boolean> owners = sustainOwners[channel];
                owners.remove(source);
                if (owners.isEmpty()) {
                    nativeBridge.nativeMidiEvent(0xB0 | channel, 64, 0);
                }
            }

            for (int note = 0; note < 128; note++) {
                int pending = state.activeNotes[channel][note];
                while (pending-- > 0) {
                    nativeBridge.nativeMidiEvent(0x80 | channel, note, 0);
                }
                state.activeNotes[channel][note] = 0;
            }
        }
    }

    private void clearChannelNotes(int channel) {
        for (SourceState state : sources.values()) {
            for (int note = 0; note < 128; note++) {
                state.activeNotes[channel][note] = 0;
            }
        }
    }

    private void clearChannelSustain(int channel) {
        for (SourceState state : sources.values()) {
            state.sustain[channel] = false;
        }
        sustainOwners[channel].clear();
    }

    private static int clampMidiValue(int value) {
        return Math.max(0, Math.min(127, value));
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        Object[] activeSources = sources.keySet().toArray();
        for (Object source : activeSources) releaseSource(source);
        closed = true;
    }

    private static final class SourceState {
        final int[][] activeNotes = new int[16][128];
        final boolean[] sustain = new boolean[16];
    }
}
