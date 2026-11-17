package com.rodolphofriedrich.fluidsynth;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Process;

/**
 * Owns Android audio output and its render thread.
 *
 * FluidSynth calls are delegated to NativeBridge. This class deliberately
 * contains no UI or track-selection logic.
 */
public final class AudioEngine {
    public interface Listener {
        void onAudioError(String message);
    }

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAMES_PER_BLOCK = 256;

    private final NativeBridge nativeBridge;
    private final Listener listener;
    private volatile boolean playing;
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
            candidate = new AudioTrack.Builder()
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
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            if (candidate.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioTrack não inicializado");
            }

            outputTrack = candidate;
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

    private void reportError(String message) {
        if (listener != null) listener.onAudioError(message);
    }
}
