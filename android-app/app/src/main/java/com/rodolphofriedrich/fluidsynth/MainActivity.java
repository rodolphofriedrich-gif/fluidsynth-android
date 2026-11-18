package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.content.Intent;
import android.media.midi.MidiDeviceInfo;
import android.os.Bundle;
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
import android.widget.ProgressBar;
import android.widget.Switch;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private final NativeBridge nativeBridge = new NativeBridge();
    private MidiScreen midiScreen;
    private SettingsScreen settingsScreen;
    private final Object keyboardMidiSource = new Object();
    private MidiRouter midiRouter;
    private AudioEngine audioEngine;
    private TextView status;
    private TextView audioDiagnostics;
    private TextView operationStatus;
    private TextView usbStatus;
    private TextView bleStatus;
    private UsbMidiInput usbMidiInput;
    private BleMidiInput bleMidiInput;
    private Button choose;
    private Button trackEnableButton;
    private Button selectInstrumentButton;
    private Button bleSearchButton;
    private Button[] trackButtons = new Button[16];
    private SeekBar polyphonySeekBar;
    private TextView polyphonyValue;
    private SeekBar velocitySeekBar;
    private TextView velocityValue;
    private int currentVelocity = 100;
    private ProgressBar importProgressBar;
    private TextView importProgressText;
    private LinearLayout libraryListContainer;
    private Spinner deviceSelector;
    private int currentPolyphony = 512;
    private int appliedPolyphony = 512;
    private final List<MidiDeviceInfo> availableMidiDevices = new ArrayList<>();
    private TrackManager trackManager;
    private SoundFontManager soundFontManager;
    private SoundFontLibraryStore soundFontLibraryStore;
    private boolean initialized;
    private boolean internalKeyboardSelected = true;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private static final int PICK_SF2 = 71;

    private static final int BG = AppUi.BG;
    private static final int PANEL = AppUi.PANEL;
    private static final int CONTROL = AppUi.CONTROL;
    private static final int ACCENT = AppUi.ACCENT;
    private static final int TEXT = AppUi.TEXT;
    private static final int MUTED = AppUi.MUTED;

    private NativeEngineLifecycle.Bridge createNativeEngineBridge() {
        return new NativeEngineLifecycle.Bridge() {
            @Override public boolean initialize() { return nativeBridge.nativeInit(); }
            @Override public void shutdown() { nativeBridge.nativeShutdown(); }
        };
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        return AppUi.rounded(this, color, radiusDp, strokeColor);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        midiScreen = new MidiScreen(this);
        settingsScreen = new SettingsScreen(this);

        status = midiScreen.status;
        audioDiagnostics = midiScreen.audioDiagnostics;
        trackButtons = midiScreen.trackButtons;
        selectInstrumentButton = midiScreen.selectInstrumentButton;
        trackEnableButton = midiScreen.trackEnableButton;
        velocitySeekBar = midiScreen.velocitySeekBar;
        velocityValue = midiScreen.velocityValue;

        deviceSelector = settingsScreen.deviceSelector;
        usbStatus = settingsScreen.usbStatus;
        bleStatus = settingsScreen.bleStatus;
        polyphonySeekBar = settingsScreen.polyphonySeekBar;
        polyphonyValue = settingsScreen.polyphonyValue;
        bleSearchButton = settingsScreen.bleSearchButton;
        choose = settingsScreen.chooseButton;
        importProgressText = settingsScreen.importProgressText;
        importProgressBar = settingsScreen.importProgressBar;
        libraryListContainer = settingsScreen.libraryListContainer;

        operationStatus = new TextView(this);
        operationStatus.setText("");
        operationStatus.setTextColor(MUTED);
        operationStatus.setTextSize(12);

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

        for (int i = 0; i < trackButtons.length; i++) {
            final int channel = i;
            trackButtons[i].setOnClickListener(v -> {
                if (trackManager != null) {
                    trackManager.select(channel);
                    updateTrackControls();
                }
            });
        }

        selectInstrumentButton.setOnClickListener(v -> {
            if (trackManager != null) showSoundFontLibraryPicker(trackManager.getActiveChannel());
        });
        trackEnableButton.setOnClickListener(v -> {
            if (trackManager == null) return;
            int channel = trackManager.getActiveChannel();
            boolean enabled = !trackManager.get(channel).isEnabled();
            trackManager.setEnabled(channel, enabled);
            updateTrackControls();
        });

        polyphonySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPolyphony = (progress + 1) * PolyphonySettingsStore.STEP;
                polyphonyValue.setText(currentPolyphony + " vozes");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (initialized && !nativeBridge.nativeSetPolyphony(currentPolyphony)) {
                    currentPolyphony = appliedPolyphony;
                    polyphonySeekBar.setProgress((appliedPolyphony / PolyphonySettingsStore.STEP) - 1);
                    polyphonyValue.setText(appliedPolyphony + " vozes");
                    operationStatus.setText("Não foi possível aplicar a polifonia. O valor anterior foi mantido.");
                    return;
                }
                appliedPolyphony = currentPolyphony;
                PolyphonySettingsStore.save(MainActivity.this, currentPolyphony);
                operationStatus.setText("Polifonia definida para " + currentPolyphony + " vozes.");
            }
        });

        bleSearchButton.setOnClickListener(v -> {
            if (bleMidiInput == null) return;
            boolean enabled = !bleMidiInput.isScanningEnabled();
            bleMidiInput.setScanningEnabled(enabled);
            updateBleSearchButton();
        });
        choose.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_SF2);
        });

        velocitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVelocity = progress + 1;
                velocityValue.setText("Intensidade: " + currentVelocity);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                VelocitySettingsStore.save(MainActivity.this, currentVelocity);
                operationStatus.setText("Velocity definida para " + currentVelocity + ".");
            }
        });

        for (int i = 0; i < midiScreen.keyboardKeys.length; i++) {
            final int note = MidiScreen.NOTES[i];
            Button key = midiScreen.keyboardKeys[i];
            final int[] pressedChannel = {-1};
            key.setOnTouchListener((v, event) -> {
                if (!initialized || audioEngine == null || !audioEngine.isPlaying()) return true;
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (!internalKeyboardSelected) return true;
                    int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
                    pressedChannel[0] = channel;
                    if (midiRouter != null) midiRouter.send(keyboardMidiSource,
                            0x90 | channel, note, currentVelocity);
                    v.setBackground(rounded(ACCENT, 10, 0));
                    return true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (pressedChannel[0] >= 0) {
                        if (midiRouter != null) midiRouter.send(keyboardMidiSource,
                                0x80 | pressedChannel[0], note, 0);
                        pressedChannel[0] = -1;
                    }
                    v.setBackground(rounded(CONTROL, 10, Color.rgb(65, 82, 110)));
                    return true;
                }
                return true;
            });
        }

        ScrollView midiScroll = midiScreen.scrollView;
        ScrollView settingsScroll = settingsScreen.scrollView;
        settingsScroll.setVisibility(View.GONE);

        LinearLayout screenHost = new LinearLayout(this);
        screenHost.setOrientation(LinearLayout.VERTICAL);
        screenHost.setBackgroundColor(BG);
        screenHost.addView(midiScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        screenHost.addView(settingsScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        operationStatus.setPadding(16, 8, 16, 8);
        operationStatus.setBackground(rounded(PANEL, 10, Color.rgb(42, 54, 75)));
        LinearLayout.LayoutParams operationStatusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        operationStatusParams.setMargins(16, 4, 16, 8);
        screenHost.addView(operationStatus, operationStatusParams);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(12, 8, 12, 8);
        navigation.setBackground(rounded(PANEL, 14, Color.rgb(42, 54, 75)));

        Button midiTab = new Button(this);
        midiTab.setText("MIDI");
        midiTab.setAllCaps(false);
        midiTab.setTextColor(Color.WHITE);
        midiTab.setBackground(rounded(ACCENT, 10, 0));

        Button settingsTab = new Button(this);
        settingsTab.setText("CONFIGURAÇÕES");
        settingsTab.setAllCaps(false);
        settingsTab.setTextColor(TEXT);
        settingsTab.setBackground(rounded(CONTROL, 10, 0));

        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0,
                (int) (48 * getResources().getDisplayMetrics().density), 1f);
        tabParams.setMargins(4, 0, 4, 0);
        navigation.addView(midiTab, tabParams);
        navigation.addView(settingsTab, new LinearLayout.LayoutParams(tabParams));

        midiTab.setOnClickListener(v -> {
            ScreenNavigator.show(midiScroll, settingsScroll, false);
            midiTab.setBackground(rounded(ACCENT, 10, 0));
            settingsTab.setBackground(rounded(CONTROL, 10, 0));
        });
        settingsTab.setOnClickListener(v -> {
            ScreenNavigator.show(midiScroll, settingsScroll, true);
            settingsTab.setBackground(rounded(ACCENT, 10, 0));
            midiTab.setBackground(rounded(CONTROL, 10, 0));
        });

        screenHost.addView(navigation, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(screenHost);

        initialized = NativeEngineLifecycle.acquire(createNativeEngineBridge());
        if (initialized) {
            midiRouter = new MidiRouter(nativeBridge, (channel, bank, program) ->
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && trackManager != null
                                && trackManager.updatePresetFromMidi(channel, bank, program,
                                        "Programa MIDI " + (program + 1))) {
                            updateTrackControls();
                        }
                    }));
            currentPolyphony = PolyphonySettingsStore.load(this);
            polyphonySeekBar.setProgress((currentPolyphony / PolyphonySettingsStore.STEP) - 1);
            if (nativeBridge.nativeSetPolyphony(currentPolyphony)) {
                appliedPolyphony = currentPolyphony;
            } else {
                currentPolyphony = PolyphonySettingsStore.DEFAULT;
                appliedPolyphony = PolyphonySettingsStore.DEFAULT;
                polyphonySeekBar.setProgress((appliedPolyphony / PolyphonySettingsStore.STEP) - 1);
                polyphonyValue.setText(appliedPolyphony + " vozes");
                operationStatus.setText("Não foi possível restaurar a polifonia salva. Usando o padrão.");
            }
            currentVelocity = VelocitySettingsStore.load(this);
            velocitySeekBar.setProgress(currentVelocity - 1);
            velocityValue.setText("Intensidade: " + currentVelocity);
            soundFontManager = new SoundFontManager(this);
            soundFontLibraryStore = new SoundFontLibraryStore(this, soundFontManager);
            refreshSoundFontLibraryList();
            trackManager = new TrackManager(this, new TrackManager.Engine() {
                @Override public int loadSoundFont(int channel, String path) { return nativeBridge.nativeLoadTrack(channel, path); }
                @Override public boolean selectPreset(int channel, int bank, int program) { return nativeBridge.nativeSelectTrackPreset(channel, bank, program); }
                @Override public void setEnabled(int channel, boolean enabled) { nativeBridge.nativeSetTrackEnabled(channel, enabled); }
                @Override public void setActiveChannel(int channel) { nativeBridge.nativeSetActiveTrack(channel); }
            });
            updateTrackControls();
            setTrackControlsEnabled(false);
            operationStatus.setText("Restaurando SoundFonts e configurações...");
            trackManager.restoreAsync(loader, () -> runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    updateTrackControls();
                    setTrackControlsEnabled(true);
                    int restoreFailures = trackManager.getRestoreFailureCount();
                    operationStatus.setText(restoreFailures == 0
                            ? "Configurações das faixas restauradas."
                            : "Restauração concluída com " + restoreFailures
                                    + " falha(s). Verifique os canais com SoundFont ausente ou inválido.");
                }
            }));
            audioEngine = new AudioEngine(nativeBridge, new AudioEngine.Listener() {
                @Override public void onAudioError(String message) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && operationStatus != null) {
                            if (midiRouter != null) midiRouter.panic();
                            audioDiagnostics.setText("Áudio: falha na saída");
                            operationStatus.setText(message);
                        }
                    });
                }

                @Override public void onAudioUnderruns(int totalUnderruns) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && audioDiagnostics != null) {
                            audioDiagnostics.setText("Áudio: " + totalUnderruns + " underruns");
                        }
                    });
                }
            });
            audioEngine.start();
            if (audioEngine.isPlaying()) audioDiagnostics.setText("Áudio: saída ativa");
            usbMidiInput = new UsbMidiInput(this, new UsbMidiInput.Listener() {
                @Override public void onMidiMessage(Object source, int midiStatus, int data1, int data2) {
                    if (initialized && midiRouter != null) midiRouter.send(source, midiStatus, data1, data2);
                }
                @Override public void onSourceDisconnected(Object source) {
                    if (midiRouter != null) midiRouter.releaseSource(source);
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
                @Override public void onMidiMessage(Object source, int midiStatus, int data1, int data2) {
                    if (initialized && midiRouter != null) midiRouter.send(source, midiStatus, data1, data2);
                }
                @Override public void onSourceDisconnected(Object source) {
                    if (midiRouter != null) midiRouter.releaseSource(source);
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

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (bleMidiInput != null && bleMidiInput.handlesPermissionRequest(requestCode)) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) granted &= result == android.content.pm.PackageManager.PERMISSION_GRANTED;
            bleMidiInput.onPermissionResult(granted);
            updateBleSearchButton();
        }
    }

    private void showImportProgress() {
        if (importProgressBar == null || importProgressText == null) return;
        importProgressBar.setIndeterminate(true);
        importProgressBar.setProgress(0);
        importProgressBar.setVisibility(View.VISIBLE);
        importProgressText.setText("Preparando importação...");
        importProgressText.setVisibility(View.VISIBLE);
    }

    private void updateImportProgress(long copiedBytes, long totalBytes) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || importProgressBar == null) return;
            if (totalBytes > 0) {
                int percent = (int)Math.min(100, (copiedBytes * 100.0) / totalBytes);
                importProgressBar.setIndeterminate(false);
                importProgressBar.setProgress(percent * 10);
                importProgressText.setText("Importando biblioteca: " + percent + "% ("
                        + android.text.format.Formatter.formatShortFileSize(this, copiedBytes)
                        + " de " + android.text.format.Formatter.formatShortFileSize(this, totalBytes) + ")");
            } else {
                importProgressBar.setIndeterminate(true);
                importProgressText.setText("Importando biblioteca — "
                        + android.text.format.Formatter.formatShortFileSize(this, copiedBytes) + " copiados");
            }
        });
    }

    private void hideImportProgress() {
        if (importProgressBar == null || importProgressText == null) return;
        importProgressBar.setIndeterminate(false);
        importProgressBar.setProgress(0);
        importProgressBar.setVisibility(View.GONE);
        importProgressText.setVisibility(View.GONE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SF2 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        choose.setEnabled(false);
        showImportProgress();
        final int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
        operationStatus.setText("Importando SF2 para CH " + (channel + 1) + "...");
        final android.net.Uri uri = data.getData();
        loader.execute(() -> {
            String message;
            try {
                String displayName = getSoundFontDisplayName(uri);
                File file = soundFontManager.importForTrack(uri, channel, displayName,
                        (copiedBytes, totalBytes) -> updateImportProgress(copiedBytes, totalBytes));
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed() && importProgressText != null) {
                        importProgressText.setText("Biblioteca importada. Carregando instrumento...");
                        importProgressBar.setIndeterminate(true);
                    }
                });
                boolean loaded = trackManager.loadSoundFont(channel, file.getAbsolutePath(), displayName);
                message = loaded ? "CH " + (channel + 1) + " carregado: " + displayName
                        : "Biblioteca importada, mas não foi possível carregá-la. O instrumento anterior foi preservado.";
            } catch (Exception error) {
                message = "Erro ao importar SF2: " + error.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    operationStatus.setText(result);
                    choose.setEnabled(true);
                    hideImportProgress();
                    refreshSoundFontLibraryList();
                    updateTrackControls();
                }
            });
        });
    }

    private void refreshSoundFontLibraryList() {
        if (libraryListContainer == null || soundFontLibraryStore == null) return;
        libraryListContainer.removeAllViews();
        List<File> libraries = soundFontLibraryStore.getLibraries();
        if (libraries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Nenhuma biblioteca SF2 importada.");
            empty.setTextColor(MUTED);
            empty.setTextSize(13);
            empty.setPadding(0, 8, 0, 8);
            libraryListContainer.addView(empty);
            return;
        }

        for (int i = 0; i < libraries.size(); i++) {
            File library = libraries.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            TextView name = new TextView(this);
            name.setText(soundFontManager.getDisplayName(library));
            name.setTextColor(TEXT);
            name.setTextSize(14);
            name.setMaxLines(2);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Switch enabledSwitch = new Switch(this);
            boolean enabled = soundFontLibraryStore.isEnabled(library);
            enabledSwitch.setText(enabled ? "Ativa" : "Desativada");
            enabledSwitch.setTextColor(enabled ? TEXT : MUTED);
            enabledSwitch.setChecked(enabled);
            enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
                soundFontLibraryStore.setEnabled(library, checked);
                button.setText(checked ? "Ativa" : "Desativada");
                button.setTextColor(checked ? TEXT : MUTED);
                operationStatus.setText(checked
                        ? "Biblioteca ativada para novas seleções."
                        : "Biblioteca desativada para novas seleções. Canais em uso foram preservados.");
            });
            row.addView(enabledSwitch, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            libraryListContainer.addView(row);

            if (i < libraries.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(55, 69, 94));
                libraryListContainer.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (int)(1 * getResources().getDisplayMetrics().density)));
            }
        }
    }

    private void showSoundFontLibraryPicker(int channel) {
        if (soundFontLibraryStore == null) return;
        List<File> availableLibraries = new ArrayList<>();
        for (File library : soundFontLibraryStore.getLibraries()) {
            if (soundFontLibraryStore.isEnabled(library)) availableLibraries.add(library);
        }
        if (availableLibraries.isEmpty()) {
            operationStatus.setText("Não há bibliotecas ativas. Ative uma biblioteca nas Configurações ou importe uma nova.");
            return;
        }
        File[] files = availableLibraries.toArray(new File[0]);
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = soundFontManager.getDisplayName(files[i]);
        new android.app.AlertDialog.Builder(this).setTitle("Bibliotecas SoundFont")
            .setItems(names, (dialog, which) -> showPresetPicker(channel, files[which]))
            .setNegativeButton("Cancelar", null).show();
    }

    private void showPresetPicker(int channel, File library) {
        operationStatus.setText("Lendo instrumentos...");
        loader.execute(() -> {
            String[] presets = nativeBridge.nativeListPresets(library.getAbsolutePath());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (presets == null || presets.length == 0) {
                    operationStatus.setText("Nenhum instrumento encontrado.");
                    return;
                }
                String[] labels = new String[presets.length];
                for (int i = 0; i < presets.length; i++) {
                    String[] fields = presets[i].split("\\|", 3);
                    labels[i] = fields.length == 3 ? fields[2] + " (Banco " + fields[0] + ", Programa " + fields[1] + ")" : presets[i];
                }
                new android.app.AlertDialog.Builder(this).setTitle(soundFontManager.getDisplayName(library))
                    .setItems(labels, (dialog, which) -> {
                        try {
                            String[] fields = presets[which].split("\\|", 3);
                            int bank = Integer.parseInt(fields[0]);
                            int program = Integer.parseInt(fields[1]);
                            String presetName = fields[2];
                            loader.execute(() -> {
                                boolean loaded = trackManager.loadSoundFont(channel, library.getAbsolutePath(),
                                        soundFontManager.getDisplayName(library));
                                boolean selected = loaded && nativeBridge.nativeSelectTrackPreset(channel, bank, program);
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    if (selected) {
                                        trackManager.updatePreset(channel, bank, program, presetName);
                                        operationStatus.setText("Instrumento selecionado: " + presetName);
                                        updateTrackControls();
                                    } else {
                                        operationStatus.setText("Não foi possível carregar o instrumento.");
                                    }
                                });
                            });
                        } catch (Exception error) {
                            operationStatus.setText("Erro ao selecionar instrumento: " + error.getMessage());
                        }
                    }).setNegativeButton("Voltar", (dialog, which) -> showSoundFontLibraryPicker(channel)).show();
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
        // setSelection(..., false) suppresses the listener. Keep keyboard routing
        // synchronized when a selected USB device disappears from the list.
        internalKeyboardSelected = selectedPosition == 0;
    }

    private void updateTrackControls() {
        if (trackManager == null || trackEnableButton == null) return;
        int channel = trackManager.getActiveChannel();
        Track selected = trackManager.get(channel);
        trackEnableButton.setText("CH " + (channel + 1) + (selected.isEnabled() ? " — ATIVA" : " — MUTADA"));
        for (int i = 0; i < trackButtons.length; i++) {
            Track item = trackManager.get(i);
            String state = item.hasSoundFont() ? compactTrackName(item.getPresetName() == null ? item.getSoundFontName() : item.getPresetName()) : "SEM SF2";
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
        String preset = selected.getPresetName();
        String instrument = preset == null || preset.trim().isEmpty()
                ? soundFont : preset + " — " + soundFont;
        status.setText("CH " + (channel + 1) + ": " + instrument);
        if (selectInstrumentButton != null) {
            selectInstrumentButton.setText(selected.hasSoundFont() ? "TROCAR INSTRUMENTO" : "ESCOLHER INSTRUMENTO");
        }
    }

    private void setTrackControlsEnabled(boolean enabled) {
        if (choose != null) choose.setEnabled(enabled);
        if (selectInstrumentButton != null) selectInstrumentButton.setEnabled(enabled);
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

    @Override protected void onDestroy() {
        if (bleMidiInput != null) {
            bleMidiInput.close();
            bleMidiInput = null;
        }
        if (usbMidiInput != null) {
            usbMidiInput.close();
            usbMidiInput = null;
        }

        if (midiRouter != null) {
            midiRouter.releaseSource(keyboardMidiSource);
            midiRouter.close();
            midiRouter = null;
        }

        // Stop audio immediately; wait for native workers away from the UI thread.
        if (audioEngine != null) audioEngine.stop();
        loader.shutdown();
        final AudioEngine pendingAudioEngine = audioEngine;
        audioEngine = null;
        final boolean releaseEngine = initialized;
        initialized = false;
        Thread cleanup = new Thread(() -> {
            boolean interrupted = false;
            try {
                while (!loader.isTerminated()) {
                    try {
                        loader.awaitTermination(1, TimeUnit.DAYS);
                    } catch (InterruptedException error) {
                        interrupted = true;
                    }
                }
                if (pendingAudioEngine != null) {
                    pendingAudioEngine.awaitStoppedAndRelease();
                }
                if (releaseEngine) NativeEngineLifecycle.release(createNativeEngineBridge());
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }, "FluidSynth-Shutdown");
        cleanup.start();
        super.onDestroy();
    }
}