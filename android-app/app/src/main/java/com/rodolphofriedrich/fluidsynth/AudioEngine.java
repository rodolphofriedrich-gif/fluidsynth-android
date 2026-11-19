package com.rodolphofriedrich.fluidsynth;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Process;
import android.os.Build;

/**
 * Owns Android audio output and its render thread.
 *
 * FluidSynth calls are delegated to NativeBridge. This class deliberately
 * contains no UI or track-selection logic.
 */
public final class AudioEngine {
    public interface Listener {
        void onAudioError(String message);
        void onAudioUnderruns(int totalUnderruns);
    }

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAMES_PER_BLOCK = 256;

    private final NativeBridge nativeBridge;
    private final Listener listener;
    private volatile boolean playing;
    private volatile float masterVolume = 1f;
    private AudioTrack outputTrack;
    private Thread renderThread;

    public AudioEngine(NativeBridge nativeBridge, Listener listener) {
        if (nativeBridge == null) throw new IllegalArgumentException("NativeBridge nulo");
        this.nativeBridge = nativeBridge;
        this.listener = listener;
    }

    public synchronized void start() {
        if (playing || (renderThread != null && renderThread.isAlive())) return;

        int minimumBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT);
        if (minimumBuffer <= 0) {
            reportError("Dispositivo não oferece saída PCM float.");
            return;
        }

        int bufferBytes = Math.max(minimumBuffer, FRAMES_PER_BLOCK * 2 * 4 * 4);
        AudioTrack candidate = null;
        try {
            AudioTrack.Builder builder = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build())
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
                } catch (RuntimeException ignored) {
                    // Keep the standard AudioTrack configuration if the device rejects the hint.
                }
            }
            candidate = builder.build();

            if (candidate.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioTrack não inicializado");
            }

            outputTrack = candidate;
            candidate.setVolume(masterVolume);
            playing = true;
            candidate.play();

            AudioTrack activeTrack = candidate;
            renderThread = new Thread(() -> renderLoop(activeTrack),
                    "FluidSynth-Audio");
            renderThread.start();
        } catch (Exception error) {
            playing = false;
            if (candidate != null) {
                try { candidate.release(); } catch (RuntimeException ignored) {}
            }
            outputTrack = null;
            reportError("Falha na saída de áudio: " + error.getMessage());
        }
    }

    private void renderLoop(AudioTrack activeTrack) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        float[] buffer = new float[FRAMES_PER_BLOCK * 2];
        boolean outputFailed = false;
        int lastReportedUnderruns = 0;
        int blocksSinceUnderrunCheck = 0;

        while (playing) {
            try {
                nativeBridge.nativeRender(buffer, FRAMES_PER_BLOCK);
                int offset = 0;
                while (playing && offset < buffer.length) {
                    int written = activeTrack.write(
                            buffer, offset, buffer.length - offset,
                            AudioTrack.WRITE_BLOCKING);
                    if (written <= 0) {
                        outputFailed = true;
                        break;
                    }
                    offset += written;
                }
                if (outputFailed) break;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                        && ++blocksSinceUnderrunCheck >= 20) {
                    blocksSinceUnderrunCheck = 0;
                    try {
                        int underruns = activeTrack.getUnderrunCount();
                        if (underruns > lastReportedUnderruns
                                && (lastReportedUnderruns == 0 || underruns - lastReportedUnderruns >= 10)) {
                            lastReportedUnderruns = underruns;
                            reportUnderruns(underruns);
                        }
                    } catch (RuntimeException ignored) {
                        // Diagnostics must never interrupt the audio render loop.
                    }
                }
            } catch (Throwable error) {
                outputFailed = true;
                break;
            }
        }

        playing = false;
        if (outputFailed) {
            reportError("A saída de áudio foi interrompida. Verifique o dispositivo de áudio.");
        }
    }

    /** Sets output gain on AudioTrack only; MIDI velocity and FluidSynth state are untouched. */
    public synchronized void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
        AudioTrack current = outputTrack;
        if (current != null) {
            try { current.setVolume(masterVolume); }
            catch (RuntimeException ignored) {
                // The output thread must not be affected by a UI volume adjustment.
            }
        }
    }

    /** Returns whether the audio render loop is currently active. */
    public boolean isPlaying() {
        return playing;
    }

    /** Requests an immediate stop without waiting on the calling thread. */
    public void stop() {
        playing = false;
        AudioTrack current;
        synchronized (this) {
            current = outputTrack;
        }
        if (current == null) return;

        try { current.pause(); } catch (IllegalStateException ignored) {}
        try { current.flush(); } catch (IllegalStateException ignored) {}
        try { current.stop(); } catch (IllegalStateException ignored) {}
    }

    /** Waits for the render thread and releases AudioTrack; call off the UI thread. */
    public void awaitStoppedAndRelease() {
        final Thread pendingThread;
        final AudioTrack pendingTrack;
        synchronized (this) {
            pendingThread = renderThread;
            pendingTrack = outputTrack;
        }

        boolean interrupted = false;
        if (pendingThread != null && pendingThread != Thread.currentThread()) {
            while (pendingThread.isAlive()) {
                try {
                    pendingThread.join();
                } catch (InterruptedException error) {
                    interrupted = true;
                }
            }
        }

        if (pendingTrack != null) {
            try { pendingTrack.release(); } catch (RuntimeException ignored) {}
        }

        synchronized (this) {
            if (renderThread == pendingThread) renderThread = null;
            if (outputTrack == pendingTrack) outputTrack = null;
        }

        if (interrupted) Thread.currentThread().interrupt();
    }

    /** Returns AudioTrack underruns, or -1 on Android versions without this metric. */
    public int getUnderrunCount() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return -1;
        AudioTrack current;
        synchronized (this) {
            current = outputTrack;
        }
        if (current == null) return -1;
        try {
            return current.getUnderrunCount();
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private void reportUnderruns(int totalUnderruns) {
        if (listener != null) listener.onAudioUnderruns(totalUnderruns);
    }

    private void reportError(String message) {
        if (listener != null) listener.onAudioError(message);
    }
}
