package com.rodolphofriedrich.fluidsynth;

/**
 * Thin Java boundary for the native FluidSynth API.
 *
 * UI and audio-output classes call this bridge instead of declaring JNI
 * methods themselves. The native engine implementation remains in C++.
 */
public final class NativeBridge {
    static {
        System.loadLibrary("player");
    }

    native boolean nativeInit();
    native boolean nativeSetPolyphony(int voices);
    native void nativeSetReverb(float roomSize, float damping, float width, float level);
    native void nativeSetChorus(int voices, float level, float speed, float depthMs);
    native void nativeSetReverbActive(boolean active);
    native void nativeSetChorusActive(boolean active);
    native int nativeLoad(String path);
    native int nativeLoadTrack(int channel, String path);
    native String[] nativeListPresets(String path);
    native boolean nativeSelectTrackPreset(int channel, int bank, int program);
    native void nativeSetTrackEnabled(int channel, boolean enabled);
    native void nativeSetTrackVolume(int channel, int volume);
    native void nativeSetActiveTrack(int channel);
    native void nativeNote(int note, int velocity, boolean on);
    native void nativeNoteOnTrack(int channel, int note, int velocity, boolean on);
    native boolean nativeMidiEvent(int status, int data1, int data2);
    native int nativeGetTrackBank(int channel);
    native void nativeRender(float[] out, int frames);
    native void nativeShutdown();
}
