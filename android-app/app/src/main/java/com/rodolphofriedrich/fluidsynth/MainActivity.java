package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    static { System.loadLibrary("player"); }
    private native boolean nativeInit();
    private native int nativeLoad(String path);
    private native void nativeNote(int note, int velocity, boolean on);
    private native void nativeMidiEvent(int status, int data1, int data2);
    private native void nativeRender(float[] out, int frames);
    private native void nativeShutdown();

    private volatile boolean playing;
    private AudioTrack track;
    private Thread audioThread;
    private TextView status;
    private TextView usbStatus;
    private UsbMidiInput usbMidiInput;
    private Button choose;
    private boolean initialized;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private static final int PICK_SF2 = 71;
    private static final int SAMPLE_RATE = 48000;
    private static final int FRAMES = 256;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 28, 20, 20);
        status = new TextView(this);
        status.setText("FluidSynth — selecione um arquivo SF2");
        status.setTextSize(18);
        root.addView(status);
        usbStatus = new TextView(this);
        usbStatus.setText("USB MIDI: aguardando controlador");
        root.addView(usbStatus);
        choose = new Button(this);
        choose.setText("ESCOLHER SOUNDFONT (.sf2)");
        choose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_SF2);
        });
        root.addView(choose);
        TextView hint = new TextView(this);
        hint.setText("Carregue um SF2 para liberar o teclado.");
        root.addView(hint);

        int[] notes = {60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79};
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER);
            root.addView(line);
            for (int col = 0; col < 6; col++) {
                final int note = notes[row * 6 + col];
                Button key = new Button(this);
                key.setText(noteName(note));
                key.setTextSize(11);
                line.addView(key, new LinearLayout.LayoutParams(0, 150, 1));
                key.setOnTouchListener((v, event) -> {
                    if (!initialized || !playing) return true;
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        nativeNote(note, 100, true);
                        return true;
                    }
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        nativeNote(note, 0, false);
                        return true;
                    }
                    return true;
                });
            }
        }
        setContentView(root);
        initialized = nativeInit();
        if (initialized) {
            startAudio();
            usbMidiInput = new UsbMidiInput(this, new UsbMidiInput.Listener() {
                @Override public void onMidiMessage(int midiStatus, int data1, int data2) {
                    if (initialized) nativeMidiEvent(midiStatus, data1, data2);
                }
                @Override public void onDeviceStatus(String message) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) usbStatus.setText("USB MIDI: " + message);
                    });
                }
            });
            usbMidiInput.start();
        } else status.setText("Falha ao iniciar o motor FluidSynth.");
    }

    private String noteName(int note) {
        return new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"}[note % 12] + (note / 12 - 1);
    }

    private void startAudio() {
        int min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT);
        if (min <= 0) {
            status.setText("Dispositivo não oferece saída PCM float.");
            return;
        }
        int bufferBytes = Math.max(min, FRAMES * 2 * 4 * 4);
        AudioTrack candidate = null;
        try {
            candidate = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM).build();
            if (candidate.getState() != AudioTrack.STATE_INITIALIZED) throw new IllegalStateException("AudioTrack não inicializado");
            track = candidate;
            playing = true;
            track.play();
            audioThread = new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                float[] data = new float[FRAMES * 2];
                while (playing) {
                    try {
                        nativeRender(data, FRAMES);
                        AudioTrack current = track;
                        if (current == null) break;
                        int written = current.write(data, 0, data.length, AudioTrack.WRITE_BLOCKING);
                        if (written < 0) break;
                    } catch (Throwable error) {
                        break;
                    }
                }
                playing = false;
            }, "FluidSynth-Audio");
            audioThread.start();
        } catch (Exception error) {
            if (candidate != null) candidate.release();
            track = null;
            status.setText("Falha na saída de áudio: " + error.getMessage());
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SF2 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        choose.setEnabled(false);
        status.setText("Copiando e carregando SoundFont...");
        final android.net.Uri uri = data.getData();
        loader.execute(() -> {
            String message;
            try {
                File file = new File(getFilesDir(), "selected.sf2");
                File temp = new File(getFilesDir(), "selected.sf2.tmp");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(temp)) {
                    if (in == null) throw new IllegalStateException("Não foi possível abrir o arquivo.");
                    byte[] buffer = new byte[65536];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    out.getFD().sync();
                }
                if (!temp.renameTo(file)) {
                    try (InputStream in = new java.io.FileInputStream(temp);
                         FileOutputStream out = new FileOutputStream(file)) {
                        byte[] buffer = new byte[65536];
                        int count;
                        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    }
                    temp.delete();
                }
                int id = nativeLoad(file.getAbsolutePath());
                message = id >= 0 ? "SoundFont carregado: " + file.getName() : "Falha ao carregar SF2. O instrumento anterior foi preservado.";
            } catch (Exception error) {
                message = "Erro ao importar SF2: " + error.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    status.setText(result);
                    choose.setEnabled(true);
                }
            });
        });
    }

    private void stopAudio() {
        playing = false;
        AudioTrack current = track;
        if (current != null) {
            try { current.pause(); } catch (IllegalStateException ignored) {}
            try { current.flush(); } catch (IllegalStateException ignored) {}
            try { current.stop(); } catch (IllegalStateException ignored) {}
        }
        Thread thread = audioThread;
        if (thread != null && thread != Thread.currentThread()) {
            try { thread.join(2000); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
        if (thread != null && thread.isAlive()) return;
        audioThread = null;
        track = null;
        if (current != null) current.release();
    }

    @Override protected void onDestroy() {
        if (usbMidiInput != null) {
            usbMidiInput.close();
            usbMidiInput = null;
        }
        loader.shutdown();
        try { loader.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        stopAudio();
        if (audioThread == null) {
            if (initialized) nativeShutdown();
            initialized = false;
        }
        super.onDestroy();
    }
}