package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.midi.MidiDeviceInfo;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    static { System.loadLibrary("player"); }
    private native boolean nativeInit();
    private native int nativeLoad(String path);
    private native int nativeLoadTrack(int channel, String path);
    private native void nativeSetTrackEnabled(int channel, boolean enabled);
    private native void nativeSetActiveTrack(int channel);
    private native void nativeNote(int note, int velocity, boolean on);
    private native void nativeNoteOnTrack(int channel, int note, int velocity, boolean on);
    private native void nativeMidiEvent(int status, int data1, int data2);
    private native void nativeRender(float[] out, int frames);
    private native void nativeShutdown();

    private volatile boolean playing;
    private AudioTrack track;
    private Thread audioThread;
    private TextView status;
    private TextView usbStatus;
    private TextView bleStatus;
    private UsbMidiInput usbMidiInput;
    private BleMidiInput bleMidiInput;
    private Button choose;
    private Button trackEnableButton;
    private Button bleSearchButton;
    private Spinner trackSelector;
    private Spinner deviceSelector;
    private final List<MidiDeviceInfo> availableMidiDevices = new ArrayList<>();
    private TrackManager trackManager;
    private SoundFontManager soundFontManager;
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
        TextView deviceLabel = new TextView(this);
        deviceLabel.setText("CONTROLADOR USB / MIDI INTERNO");
        root.addView(deviceLabel);
        deviceSelector = new Spinner(this);
        deviceSelector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Nenhum controlador selecionado"}));
        deviceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (usbMidiInput == null) return;
                if (position == 0) usbMidiInput.selectDevice(-1);
                else if (position - 1 < availableMidiDevices.size())
                    usbMidiInput.selectDevice(availableMidiDevices.get(position - 1).getId());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(deviceSelector);
        usbStatus = new TextView(this);
        usbStatus.setText("MIDI: aguardando controlador");
        root.addView(usbStatus);
        bleStatus = new TextView(this);
        bleStatus.setText("BLE MIDI: aguardando busca");
        root.addView(bleStatus);

        TextView trackLabel = new TextView(this);
        trackLabel.setText("FAIXA MIDI ATIVA");
        root.addView(trackLabel);
        trackSelector = new Spinner(this);
        String[] trackNames = new String[16];
        for (int i = 0; i < 16; i++) trackNames[i] = "CH " + String.format(java.util.Locale.ROOT, "%02d", i + 1);
        trackSelector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, trackNames));
        root.addView(trackSelector);

        trackEnableButton = new Button(this);
        trackEnableButton.setText("FAIXA ATIVA");
        trackEnableButton.setOnClickListener(v -> {
            if (trackManager == null) return;
            int channel = trackManager.getActiveChannel();
            boolean enabled = !trackManager.get(channel).isEnabled();
            trackManager.setEnabled(channel, enabled);
            updateTrackControls();
        });
        root.addView(trackEnableButton);

        bleSearchButton = new Button(this);
        bleSearchButton.setText("BUSCA BLE: LIGADA");
        bleSearchButton.setOnClickListener(v -> {
            if (bleMidiInput == null) return;
            boolean enabled = !bleMidiInput.isScanningEnabled();
            bleMidiInput.setScanningEnabled(enabled);
            updateBleSearchButton();
        });
        root.addView(bleSearchButton);

        choose = new Button(this);
        choose.setText("IMPORTAR SF2 PARA ESTA FAIXA");
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
                        nativeNoteOnTrack(trackManager == null ? 0 : trackManager.getActiveChannel(), note, 100, true);
                        return true;
                    }
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        nativeNoteOnTrack(trackManager == null ? 0 : trackManager.getActiveChannel(), note, 0, false);
                        return true;
                    }
                    return true;
                });
            }
        }
        setContentView(root);
        initialized = nativeInit();
        if (initialized) {
            soundFontManager = new SoundFontManager(this);
            trackManager = new TrackManager(new TrackManager.Engine() {
                @Override public int loadSoundFont(int channel, String path) { return nativeLoadTrack(channel, path); }
                @Override public void setEnabled(int channel, boolean enabled) { nativeSetTrackEnabled(channel, enabled); }
                @Override public void setActiveChannel(int channel) { nativeSetActiveTrack(channel); }
            });
            trackSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (trackManager != null) { trackManager.select(position); updateTrackControls(); }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
            trackSelector.setSelection(0);
            startAudio();
            usbMidiInput = new UsbMidiInput(this, new UsbMidiInput.Listener() {
                @Override public void onMidiMessage(int midiStatus, int data1, int data2) {
                    if (initialized) nativeMidiEvent(midiStatus, data1, data2);
                }
                @Override public void onDeviceStatus(String message) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) usbStatus.setText("MIDI: " + message);
                    });
                }
                @Override public void onDevicesChanged(List<MidiDeviceInfo> devices) {
                    runOnUiThread(() -> updateDeviceSelector(devices));
                }
            });
            usbMidiInput.start();
            bleMidiInput = new BleMidiInput(this, new BleMidiInput.Listener() {
                @Override public void onMidiMessage(int midiStatus, int data1, int data2) {
                    if (initialized) nativeMidiEvent(midiStatus, data1, data2);
                }
                @Override public void onDeviceStatus(String message) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) bleStatus.setText("BLE MIDI: " + message);
                    });
                }
            });
            bleMidiInput.start();
            updateBleSearchButton();
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

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (bleMidiInput != null && bleMidiInput.handlesPermissionRequest(requestCode)) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) granted &= result == android.content.pm.PackageManager.PERMISSION_GRANTED;
            bleMidiInput.onPermissionResult(granted);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SF2 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        choose.setEnabled(false);
        final int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
        status.setText("Importando SF2 para CH " + (channel + 1) + "...");
        final android.net.Uri uri = data.getData();
        loader.execute(() -> {
            String message;
            try {
                File file = soundFontManager.importForTrack(uri, channel);
                boolean loaded = trackManager.loadSoundFont(channel, file.getAbsolutePath(), file.getName());
                message = loaded ? "CH " + (channel + 1) + " carregado: " + file.getName()
                        : "Falha ao carregar SF2. O instrumento anterior foi preservado.";
            } catch (Exception error) {
                message = "Erro ao importar SF2: " + error.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    status.setText(result);
                    choose.setEnabled(true);
                    updateTrackControls();
                }
            });
        });
    }

    private void updateDeviceSelector(List<MidiDeviceInfo> devices) {
        if (deviceSelector == null || isFinishing() || isDestroyed()) return;
        int previousId = usbMidiInput == null ? -1 : usbMidiInput.getSelectedDeviceId();
        availableMidiDevices.clear();
        availableMidiDevices.addAll(devices);
        List<String> labels = new ArrayList<>();
        labels.add("Selecione um controlador MIDI...");
        for (MidiDeviceInfo info : devices) {
            String name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
            if (name == null || name.trim().isEmpty()) name = "Dispositivo MIDI " + info.getId();
            String type = info.getType() == MidiDeviceInfo.TYPE_USB ? "USB" : "Interno/virtual";
            labels.add(type + " — " + name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        deviceSelector.setAdapter(adapter);
        int selectedPosition = 0;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getId() == previousId) { selectedPosition = i + 1; break; }
        }
        deviceSelector.setSelection(selectedPosition);
    }

    private void updateTrackControls() {
        if (trackManager == null || trackEnableButton == null) return;
        int channel = trackManager.getActiveChannel();
        Track selected = trackManager.get(channel);
        trackEnableButton.setText("CH " + (channel + 1) + (selected.isEnabled() ? " — ATIVA" : " — MUTADA"));
        choose.setText(selected.hasSoundFont() ? "TROCAR SF2 DA FAIXA" : "IMPORTAR SF2 PARA ESTA FAIXA");
        String soundFont = selected.hasSoundFont() ? selected.getSoundFontName() : "sem SoundFont";
        status.setText("CH " + (channel + 1) + ": " + soundFont);
    }

    private void updateBleSearchButton() {
        if (bleSearchButton == null) return;
        boolean enabled = bleMidiInput != null && bleMidiInput.isScanningEnabled();
        bleSearchButton.setText(enabled ? "BUSCA BLE: LIGADA" : "BUSCA BLE: DESLIGADA");
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
        if (bleMidiInput != null) {
            bleMidiInput.close();
            bleMidiInput = null;
        }
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