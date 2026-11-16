package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
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
import android.widget.ScrollView;
import android.widget.GridLayout;
import android.widget.SeekBar;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    static { System.loadLibrary("player"); }
    private native boolean nativeInit();
    private native boolean nativeSetPolyphony(int voices);
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
    private TextView operationStatus;
    private TextView usbStatus;
    private TextView bleStatus;
    private UsbMidiInput usbMidiInput;
    private BleMidiInput bleMidiInput;
    private Button choose;
    private Button trackEnableButton;
    private Button bleSearchButton;
    private Button[] trackButtons = new Button[16];
    private SeekBar polyphonySeekBar;
    private TextView polyphonyValue;
    private Spinner deviceSelector;
    private int currentPolyphony = 512;
    private final List<MidiDeviceInfo> availableMidiDevices = new ArrayList<>();
    private TrackManager trackManager;
    private SoundFontManager soundFontManager;
    private boolean initialized;
    private boolean internalKeyboardSelected = true;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private static final int PICK_SF2 = 71;
    private static final int SAMPLE_RATE = 48000;
    private static final int FRAMES = 256;

    private static final int BG = Color.rgb(13, 17, 27);
    private static final int PANEL = Color.rgb(23, 30, 45);
    private static final int CONTROL = Color.rgb(35, 45, 64);
    private static final int ACCENT = Color.rgb(79, 140, 255);
    private static final int TEXT = Color.rgb(239, 244, 255);
    private static final int MUTED = Color.rgb(164, 178, 201);

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        if (strokeColor != 0) shape.setStroke((int)(1 * getResources().getDisplayMetrics().density), strokeColor);
        return shape;
    }

    private void styleButton(Button button, int background, int textColor, int heightDp, int textSize) {
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(textSize);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(8, 8, 8, 8);
        button.setBackground(rounded(background, 12, 0));
        button.setElevation(0);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int)(heightDp * getResources().getDisplayMetrics().density)));
    }

    private TextView sectionLabel(String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(MUTED);
        label.setTextSize(12);
        label.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        label.setLetterSpacing(0.06f);
        return label;
    }

    private LinearLayout panel(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(14, 12, 14, 12);
        box.setBackground(rounded(PANEL, 16, Color.rgb(42, 54, 75)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 12;
        parent.addView(box, lp);
        return box;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 48, 16, 24);
        root.setBackgroundColor(BG);

        LinearLayout header = panel(root);
        TextView title = new TextView(this);
        title.setText("FLUIDSYNTH PLAYER");
        title.setTextColor(TEXT);
        title.setTextSize(19);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        header.addView(title);
        status = new TextView(this);
        status.setText("Selecione uma faixa e carregue um SoundFont");
        status.setTextColor(MUTED);
        status.setTextSize(13);
        status.setPadding(0, 5, 0, 0);
        header.addView(status);
        operationStatus = new TextView(this);
        operationStatus.setText("");
        operationStatus.setTextColor(MUTED);
        operationStatus.setTextSize(12);
        operationStatus.setPadding(0, 4, 0, 0);
        header.addView(operationStatus);

        LinearLayout controllerPanel = panel(root);
        controllerPanel.addView(sectionLabel("CONTROLADOR MIDI"));
        deviceSelector = new Spinner(this);
        deviceSelector.setPadding(4, 4, 4, 4);
        deviceSelector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Teclado interno do aplicativo"}));
        deviceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (usbMidiInput == null) return;
                int targetId = position == 0 ? -1
                        : (position - 1 < availableMidiDevices.size()
                            ? availableMidiDevices.get(position - 1).getId() : -1);
                internalKeyboardSelected = targetId == -1;
                if (usbMidiInput.getSelectedDeviceId() != targetId) {
                    usbMidiInput.selectDevice(targetId);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        controllerPanel.addView(deviceSelector, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(52 * getResources().getDisplayMetrics().density)));
        usbStatus = new TextView(this);
        usbStatus.setText("MIDI: aguardando controlador");
        usbStatus.setTextColor(MUTED);
        usbStatus.setTextSize(12);
        controllerPanel.addView(usbStatus);
        bleStatus = new TextView(this);
        bleStatus.setText("BLE MIDI: busca desligada");
        bleStatus.setTextColor(MUTED);
        bleStatus.setTextSize(12);
        bleStatus.setPadding(0, 3, 0, 0);
        controllerPanel.addView(bleStatus);

        LinearLayout tracksPanel = panel(root);
        TextView trackLabel = sectionLabel("16 FAIXAS MIDI");
        tracksPanel.addView(trackLabel);
        TextView trackHint = new TextView(this);
        trackHint.setText("Toque em uma faixa para selecioná-la");
        trackHint.setTextColor(MUTED);
        trackHint.setTextSize(12);
        trackHint.setPadding(0, 3, 0, 10);
        tracksPanel.addView(trackHint);

        GridLayout trackGrid = new GridLayout(this);
        trackGrid.setColumnCount(4);
        trackGrid.setRowCount(4);
        for (int i = 0; i < 16; i++) {
            final int channel = i;
            Button trackButton = new Button(this);
            trackButton.setText("CH " + String.format(java.util.Locale.ROOT, "%02d", i + 1) + "\nVAZIA");
            trackButton.setTextSize(11);
            trackButton.setTextColor(TEXT);
            trackButton.setAllCaps(false);
            trackButton.setPadding(2, 2, 2, 2);
            trackButton.setMinHeight(0);
            trackButton.setMinimumHeight(0);
            trackButton.setBackground(rounded(CONTROL, 10, Color.rgb(55, 69, 94)));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 4, 1f), GridLayout.spec(i % 4, 1f));
            params.width = 0;
            params.height = (int)(64 * getResources().getDisplayMetrics().density);
            params.setMargins(3, 3, 3, 3);
            trackGrid.addView(trackButton, params);
            trackButtons[i] = trackButton;
            trackButton.setOnClickListener(v -> {
                if (trackManager != null) {
                    trackManager.select(channel);
                    updateTrackControls();
                }
            });
        }
        tracksPanel.addView(trackGrid);

        LinearLayout audioPanel = panel(root);
        audioPanel.addView(sectionLabel("POLIFONIA"));
        TextView polyphonyHint = new TextView(this);
        polyphonyHint.setText("Limite máximo de vozes simultâneas do sintetizador");
        polyphonyHint.setTextColor(MUTED);
        polyphonyHint.setTextSize(12);
        polyphonyHint.setPadding(0, 3, 0, 8);
        audioPanel.addView(polyphonyHint);
        polyphonyValue = new TextView(this);
        polyphonyValue.setText("512 vozes");
        polyphonyValue.setTextColor(TEXT);
        polyphonyValue.setTextSize(16);
        polyphonyValue.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        audioPanel.addView(polyphonyValue);
        polyphonySeekBar = new SeekBar(this);
        polyphonySeekBar.setMax(255);
        polyphonySeekBar.setProgress((512 / 8) - 1);
        polyphonySeekBar.setContentDescription("Polifonia, de 8 a 2048 vozes, em passos de 8");
        audioPanel.addView(polyphonySeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int)(42 * getResources().getDisplayMetrics().density)));
        polyphonySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int voices = (progress + 1) * 8;
                currentPolyphony = voices;
                polyphonyValue.setText(voices + " vozes");
                if (fromUser && initialized) {
                    boolean applied = nativeSetPolyphony(voices);
                    if (!applied) operationStatus.setText("Não foi possível aplicar a polifonia.");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                PolyphonySettingsStore.save(MainActivity.this, currentPolyphony);
                operationStatus.setText("Polifonia definida para " + currentPolyphony + " vozes.");
            }
        });

        LinearLayout actionsPanel = panel(root);
        trackEnableButton = new Button(this);
        trackEnableButton.setText("FAIXA ATIVA");
        styleButton(trackEnableButton, CONTROL, TEXT, 48, 14);
        trackEnableButton.setOnClickListener(v -> {
            if (trackManager == null) return;
            int channel = trackManager.getActiveChannel();
            boolean enabled = !trackManager.get(channel).isEnabled();
            trackManager.setEnabled(channel, enabled);
            updateTrackControls();
        });
        actionsPanel.addView(trackEnableButton);
        bleSearchButton = new Button(this);
        bleSearchButton.setText("Ativar busca BLE");
        styleButton(bleSearchButton, CONTROL, TEXT, 48, 14);
        LinearLayout.LayoutParams bleLp = (LinearLayout.LayoutParams) bleSearchButton.getLayoutParams();
        bleLp.topMargin = (int)(8 * getResources().getDisplayMetrics().density);
        bleSearchButton.setLayoutParams(bleLp);
        bleSearchButton.setOnClickListener(v -> {
            if (bleMidiInput == null) return;
            boolean enabled = !bleMidiInput.isScanningEnabled();
            bleMidiInput.setScanningEnabled(enabled);
            updateBleSearchButton();
        });
        actionsPanel.addView(bleSearchButton);
        choose = new Button(this);
        choose.setText("＋  Importar SoundFont para a faixa");
        styleButton(choose, ACCENT, Color.WHITE, 54, 14);
        LinearLayout.LayoutParams chooseLp = (LinearLayout.LayoutParams) choose.getLayoutParams();
        chooseLp.topMargin = (int)(8 * getResources().getDisplayMetrics().density);
        choose.setLayoutParams(chooseLp);
        choose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_SF2);
        });
        actionsPanel.addView(choose);

        LinearLayout keyboardPanel = panel(root);
        keyboardPanel.addView(sectionLabel("TECLADO VIRTUAL"));
        TextView keyboardHint = new TextView(this);
        keyboardHint.setText("O teclado toca na faixa selecionada");
        keyboardHint.setTextColor(MUTED);
        keyboardHint.setTextSize(12);
        keyboardHint.setPadding(0, 3, 0, 8);
        keyboardPanel.addView(keyboardHint);
        int[] notes = {60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79};
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = (int)(6 * getResources().getDisplayMetrics().density);
            keyboardPanel.addView(line, rowLp);
            for (int col = 0; col < 6; col++) {
                final int note = notes[row * 6 + col];
                Button key = new Button(this);
                key.setText(noteName(note));
                key.setTextSize(12);
                key.setTextColor(TEXT);
                key.setAllCaps(false);
                key.setPadding(0, 0, 0, 0);
                key.setMinHeight(0);
                key.setMinimumHeight(0);
                key.setBackground(rounded(CONTROL, 10, Color.rgb(65, 82, 110)));
                LinearLayout.LayoutParams keyLp = new LinearLayout.LayoutParams(0,
                        (int)(66 * getResources().getDisplayMetrics().density), 1);
                keyLp.setMargins(2, 2, 2, 2);
                line.addView(key, keyLp);
                final int[] pressedChannel = {-1};
                key.setOnTouchListener((v, event) -> {
                    if (!initialized || !playing) return true;
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        if (!internalKeyboardSelected) return true;
                        int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
                        pressedChannel[0] = channel;
                        nativeNoteOnTrack(channel, note, 100, true);
                        v.setBackground(rounded(ACCENT, 10, 0));
                        return true;
                    }
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (pressedChannel[0] >= 0) {
                            nativeNoteOnTrack(pressedChannel[0], note, 0, false);
                            pressedChannel[0] = -1;
                        }
                        v.setBackground(rounded(CONTROL, 10, Color.rgb(65, 82, 110)));
                        return true;
                    }
                    return true;
                });
            }
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(0, 0, 0, 0);
        scrollView.addView(root);
        setContentView(scrollView);

        initialized = nativeInit();
        if (initialized) {
            currentPolyphony = PolyphonySettingsStore.load(this);
            polyphonySeekBar.setProgress((currentPolyphony / 8) - 1);
            if (!nativeSetPolyphony(currentPolyphony)) {
                operationStatus.setText("Não foi possível restaurar a polifonia salva.");
            }
            soundFontManager = new SoundFontManager(this);
            trackManager = new TrackManager(this, new TrackManager.Engine() {
                @Override public int loadSoundFont(int channel, String path) { return nativeLoadTrack(channel, path); }
                @Override public void setEnabled(int channel, boolean enabled) { nativeSetTrackEnabled(channel, enabled); }
                @Override public void setActiveChannel(int channel) { nativeSetActiveTrack(channel); }
            });
            updateTrackControls();
            setTrackControlsEnabled(false);
            operationStatus.setText("Restaurando SoundFonts e configurações...");
            trackManager.restoreAsync(loader, () -> runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    updateTrackControls();
                    setTrackControlsEnabled(true);
                    operationStatus.setText("Configurações das faixas restauradas.");
                }
            }));
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


    private String compactTrackName(String name) {
        if (name == null || name.trim().isEmpty()) return "SF2 CARREGADO";
        String clean = name.replaceFirst("(?i)\\.sf2$", "").trim();
        return clean.length() > 17 ? clean.substring(0, 16) + "…" : clean;
    }

    private String getSoundFontDisplayName(android.net.Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) return name;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "SoundFont.sf2" : fallback;
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
            updateBleSearchButton();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SF2 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        choose.setEnabled(false);
        final int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
        operationStatus.setText("Importando SF2 para CH " + (channel + 1) + "...");
        final android.net.Uri uri = data.getData();
        loader.execute(() -> {
            String message;
            try {
                String displayName = getSoundFontDisplayName(uri);
                File file = soundFontManager.importForTrack(uri, channel);
                boolean loaded = trackManager.loadSoundFont(channel, file.getAbsolutePath(), displayName);
                message = loaded ? "CH " + (channel + 1) + " carregado: " + file.getName()
                        : "Falha ao carregar SF2. O instrumento anterior foi preservado.";
            } catch (Exception error) {
                message = "Erro ao importar SF2: " + error.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    operationStatus.setText(result);
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
        labels.add("Teclado interno do aplicativo");
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
        deviceSelector.setSelection(selectedPosition, false);
    }

    private void updateTrackControls() {
        if (trackManager == null || trackEnableButton == null) return;
        int channel = trackManager.getActiveChannel();
        Track selected = trackManager.get(channel);
        trackEnableButton.setText("CH " + (channel + 1) + (selected.isEnabled() ? " — ATIVA" : " — MUTADA"));
        for (int i = 0; i < trackButtons.length; i++) {
            Track item = trackManager.get(i);
            String state = item.hasSoundFont() ? compactTrackName(item.getSoundFontName()) : "SEM SF2";
            if (!item.isEnabled()) state += " • MUTE";
            String prefix = i == channel ? "● " : "";
            trackButtons[i].setText(prefix + "CH " + String.format(java.util.Locale.ROOT, "%02d", i + 1) + "\n" + state);
            trackButtons[i].setMaxLines(2);
            trackButtons[i].setEllipsize(android.text.TextUtils.TruncateAt.END);
            trackButtons[i].setTextColor(TEXT);
            trackButtons[i].setBackground(rounded(i == channel ? ACCENT : CONTROL, 10,
                    i == channel ? ACCENT : Color.rgb(55, 69, 94)));
        }
        choose.setText(selected.hasSoundFont() ? "TROCAR SF2 DA FAIXA" : "IMPORTAR SF2 PARA ESTA FAIXA");
        String soundFont = selected.hasSoundFont() ? selected.getSoundFontName() : "sem SoundFont";
        status.setText("CH " + (channel + 1) + ": " + soundFont);
    }

    private void setTrackControlsEnabled(boolean enabled) {
        if (choose != null) choose.setEnabled(enabled);
        if (trackEnableButton != null) trackEnableButton.setEnabled(enabled);
        if (trackButtons != null) {
            for (Button button : trackButtons) {
                if (button != null) button.setEnabled(enabled);
            }
        }
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