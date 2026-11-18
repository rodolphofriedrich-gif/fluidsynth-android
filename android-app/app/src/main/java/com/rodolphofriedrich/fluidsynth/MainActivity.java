package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.content.Intent;
import android.media.midi.MidiDeviceInfo;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.content.res.Configuration;
import android.view.WindowInsets;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ScrollView;
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
    private TextView importTargetText;
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
    private static final String STATE_SHOWING_SETTINGS = "showing_settings";
    private boolean showingSettings;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideOperationStatus = () -> {
        if (operationStatus != null) operationStatus.setVisibility(View.GONE);
    };

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        showingSettings = state != null && state.getBoolean(STATE_SHOWING_SETTINGS, false);

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
        importTargetText = settingsScreen.importTargetText;

        operationStatus = new TextView(this);
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
                polyphonyValue.setText(getString(R.string.polyphony_value, currentPolyphony));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (initialized && !nativeBridge.nativeSetPolyphony(currentPolyphony)) {
                    currentPolyphony = appliedPolyphony;
                    polyphonySeekBar.setProgress((appliedPolyphony / PolyphonySettingsStore.STEP) - 1);
                    polyphonyValue.setText(getString(R.string.polyphony_value, appliedPolyphony));
                    showOperationStatus(getString(R.string.operation_polyphony_failed));
                    return;
                }
                appliedPolyphony = currentPolyphony;
                PolyphonySettingsStore.save(MainActivity.this, currentPolyphony);
                showOperationStatus(getString(R.string.operation_polyphony_saved, currentPolyphony));
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
                velocityValue.setText(getString(R.string.velocity_value, currentVelocity));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                VelocitySettingsStore.save(MainActivity.this, currentVelocity);
                showOperationStatus(getString(R.string.operation_velocity_saved, currentVelocity));
            }
        });

        for (int i = 0; i < midiScreen.keyboardKeys.length; i++) {
            final int note = MidiScreen.NOTES[i];
            Button key = midiScreen.keyboardKeys[i];
            final int[] pressedChannel = {-1};
            key.setOnTouchListener((v, event) -> {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (!initialized || audioEngine == null || !audioEngine.isPlaying()
                            || !internalKeyboardSelected) return true;
                    int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
                    pressedChannel[0] = channel;
                    if (midiRouter != null) midiRouter.send(keyboardMidiSource,
                            0x90 | channel, note, currentVelocity);
                    v.setBackground(AppUi.ripple(this, ACCENT, 10, 0));
                    return true;
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (pressedChannel[0] >= 0) {
                        if (midiRouter != null) midiRouter.send(keyboardMidiSource,
                                0x80 | pressedChannel[0], note, 0);
                        pressedChannel[0] = -1;
                    }
                    v.setBackground(AppUi.ripple(this, CONTROL, 10, AppUi.KEY_BORDER));
                    if (action == MotionEvent.ACTION_UP) v.performClick();
                    return true;
                }
                return true;
            });
        }

        ScrollView midiScroll = midiScreen.scrollView;
        ScrollView settingsScroll = settingsScreen.scrollView;
        ScreenNavigator.show(midiScroll, settingsScroll, showingSettings);

        LinearLayout screenHost = new LinearLayout(this);
        screenHost.setOrientation(LinearLayout.VERTICAL);
        screenHost.setBackgroundColor(BG);
        screenHost.addView(midiScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        screenHost.addView(settingsScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        operationStatus.setPadding(AppUi.dp(this, 16), AppUi.dp(this, 8), AppUi.dp(this, 16), AppUi.dp(this, 8));
        operationStatus.setBackground(rounded(PANEL, 10, AppUi.BORDER));
        operationStatus.setVisibility(View.GONE);
        LinearLayout.LayoutParams operationStatusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        operationStatusParams.setMargins(AppUi.dp(this, 16), AppUi.dp(this, 4), AppUi.dp(this, 16), AppUi.dp(this, 8));
        screenHost.addView(operationStatus, operationStatusParams);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(AppUi.dp(this, 12), AppUi.dp(this, 8), AppUi.dp(this, 12), AppUi.dp(this, 8));
        navigation.setBackground(rounded(PANEL, 14, AppUi.BORDER));

        Button midiTab = new Button(this);
        midiTab.setText(getString(R.string.tab_midi));
        midiTab.setAllCaps(false);
        midiTab.setTextColor(Color.WHITE);
        midiTab.setBackground(AppUi.ripple(this, showingSettings ? CONTROL : ACCENT, 10, 0));

        Button settingsTab = new Button(this);
        settingsTab.setText(getString(R.string.tab_settings));
        settingsTab.setAllCaps(false);
        settingsTab.setTextColor(TEXT);
        settingsTab.setBackground(AppUi.ripple(this, showingSettings ? ACCENT : CONTROL, 10, 0));

        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0,
                AppUi.dp(this, 48), 1f);
        tabParams.setMargins(AppUi.dp(this, 4), 0, AppUi.dp(this, 4), 0);
        navigation.addView(midiTab, tabParams);
        navigation.addView(settingsTab, new LinearLayout.LayoutParams(tabParams));

        midiTab.setOnClickListener(v -> {
            showingSettings = false;
            ScreenNavigator.show(midiScroll, settingsScroll, false);
            midiTab.setBackground(AppUi.ripple(this, ACCENT, 10, 0));
            settingsTab.setBackground(AppUi.ripple(this, CONTROL, 10, 0));
        });
        settingsTab.setOnClickListener(v -> {
            showingSettings = true;
            ScreenNavigator.show(midiScroll, settingsScroll, true);
            settingsTab.setBackground(AppUi.ripple(this, ACCENT, 10, 0));
            midiTab.setBackground(AppUi.ripple(this, CONTROL, 10, 0));
        });

        screenHost.addView(navigation, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        screenHost.setBackgroundColor(BG);
        applySystemInsets(screenHost);
        setContentView(screenHost);
        screenHost.requestApplyInsets();

        initialized = NativeEngineLifecycle.acquire(createNativeEngineBridge());
        if (initialized) {
            midiRouter = new MidiRouter(nativeBridge, (channel, bank, program) ->
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && trackManager != null
                                && trackManager.updatePresetFromMidi(channel, bank, program,
                                        getString(R.string.midi_program_fallback, program + 1))) {
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
                showOperationStatus(getString(R.string.operation_polyphony_restore_failed));
            }
            currentVelocity = VelocitySettingsStore.load(this);
            velocitySeekBar.setProgress(currentVelocity - 1);
            velocityValue.setText(getString(R.string.velocity_value, currentVelocity));
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
            showOperationStatus(getString(R.string.operation_restoring));
            trackManager.restoreAsync(loader, () -> runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    updateTrackControls();
                    setTrackControlsEnabled(true);
                    int restoreFailures = trackManager.getRestoreFailureCount();
                    showOperationStatus(restoreFailures == 0
                            ? getString(R.string.operation_restore_ok)
                            : getString(R.string.operation_restore_failures, restoreFailures));
                }
            }));
            audioEngine = new AudioEngine(nativeBridge, new AudioEngine.Listener() {
                @Override public void onAudioError(String message) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && operationStatus != null) {
                            if (midiRouter != null) midiRouter.panic();
                            audioDiagnostics.setText(getString(R.string.audio_output_failed));
                            showOperationStatus(message);
                        }
                    });
                }

                @Override public void onAudioUnderruns(int totalUnderruns) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && audioDiagnostics != null) {
                            audioDiagnostics.setText(getString(R.string.audio_underruns, totalUnderruns));
                        }
                    });
                }
            });
            audioEngine.start();
            if (audioEngine.isPlaying()) audioDiagnostics.setText(getString(R.string.audio_output_active));
            usbMidiInput = new UsbMidiInput(this, new UsbMidiInput.Listener() {
                @Override public void onMidiMessage(Object source, int midiStatus, int data1, int data2) {
                    if (initialized && midiRouter != null) midiRouter.send(source, midiStatus, data1, data2);
                }
                @Override public void onSourceDisconnected(Object source) {
                    if (midiRouter != null) midiRouter.releaseSource(source);
                }
                @Override public void onDeviceStatus(String message) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) usbStatus.setText(getString(R.string.usb_status, message));
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
                        if (!isFinishing() && !isDestroyed()) bleStatus.setText(getString(R.string.ble_status, message));
                    });
                }
            });
            bleMidiInput.start();
            updateBleSearchButton();
        } else status.setText(getString(R.string.engine_start_failed));
    }

    private String compactTrackName(String name) {
        if (name == null || name.trim().isEmpty()) return getString(R.string.soundfont_loaded);
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
        return fallback == null || fallback.trim().isEmpty() ? getString(R.string.soundfont_default_filename) : fallback;
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
        importProgressText.setText(getString(R.string.import_preparing));
        importProgressText.setVisibility(View.VISIBLE);
    }

    private void updateImportProgress(long copiedBytes, long totalBytes) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || importProgressBar == null) return;
            if (totalBytes > 0) {
                int percent = (int)Math.min(100, (copiedBytes * 100.0) / totalBytes);
                importProgressBar.setIndeterminate(false);
                importProgressBar.setProgress(percent * 10);
                importProgressText.setText(getString(R.string.import_copy_progress, percent,
                        android.text.format.Formatter.formatShortFileSize(this, copiedBytes),
                        android.text.format.Formatter.formatShortFileSize(this, totalBytes)));
            } else {
                importProgressBar.setIndeterminate(true);
                importProgressText.setText(getString(R.string.import_copy_progress_unknown,
                        android.text.format.Formatter.formatShortFileSize(this, copiedBytes)));
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
        AppUi.setButtonEnabled(choose, false);
        showImportProgress();
        final int channel = trackManager == null ? 0 : trackManager.getActiveChannel();
        showOperationStatus(getString(R.string.import_channel_started, channel + 1));
        final android.net.Uri uri = data.getData();
        loader.execute(() -> {
            String message;
            try {
                String displayName = getSoundFontDisplayName(uri);
                File file = soundFontManager.importForTrack(uri, channel, displayName,
                        (copiedBytes, totalBytes) -> updateImportProgress(copiedBytes, totalBytes));
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed() && importProgressText != null) {
                        importProgressText.setText(getString(R.string.import_loaded));
                        importProgressBar.setIndeterminate(true);
                    }
                });
                boolean loaded = trackManager.loadSoundFont(channel, file.getAbsolutePath(), displayName);
                message = loaded ? getString(R.string.import_channel_success, channel + 1, displayName)
                        : getString(R.string.import_channel_failed);
            } catch (Exception error) {
                message = getString(R.string.import_error, error.getMessage());
            }
            final String result = message;
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    showOperationStatus(result);
                    AppUi.setButtonEnabled(choose, true);
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
            empty.setText(getString(R.string.libraries_empty));
            empty.setTextColor(MUTED);
            empty.setTextSize(13);
            empty.setPadding(0, AppUi.dp(this, 8), 0, AppUi.dp(this, 8));
            libraryListContainer.addView(empty);
            return;
        }

        for (int i = 0; i < libraries.size(); i++) {
            File library = libraries.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, AppUi.dp(this, 8), 0, AppUi.dp(this, 8));

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
            enabledSwitch.setText(enabled ? getString(R.string.library_active) : getString(R.string.library_disabled_label));
            AppUi.tint(enabledSwitch);
            enabledSwitch.setContentDescription(getString(R.string.library_toggle_accessibility, soundFontManager.getDisplayName(library)));
            enabledSwitch.setTextColor(enabled ? TEXT : MUTED);
            enabledSwitch.setChecked(enabled);
            enabledSwitch.setOnCheckedChangeListener((button, checked) -> {
                soundFontLibraryStore.setEnabled(library, checked);
                button.setText(checked ? getString(R.string.library_active) : getString(R.string.library_disabled_label));
                button.setTextColor(checked ? TEXT : MUTED);
                showOperationStatus(getString(checked ? R.string.library_enabled : R.string.library_disabled));
            });
            row.addView(enabledSwitch, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            libraryListContainer.addView(row);

            if (i < libraries.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.rgb(55, 69, 94));
                libraryListContainer.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        AppUi.dp(this, 1)));
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
            showOperationStatus(getString(R.string.no_active_libraries));
            return;
        }
        File[] files = availableLibraries.toArray(new File[0]);
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = soundFontManager.getDisplayName(files[i]);
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.soundfont_libraries_title)
            .setItems(names, (dialog, which) -> showPresetPicker(channel, files[which]))
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void showPresetPicker(int channel, File library) {
        showOperationStatus(getString(R.string.reading_instruments));
        loader.execute(() -> {
            String[] presets = nativeBridge.nativeListPresets(library.getAbsolutePath());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (presets == null || presets.length == 0) {
                    showOperationStatus(getString(R.string.no_presets));
                    return;
                }
                String[] labels = new String[presets.length];
                for (int i = 0; i < presets.length; i++) {
                    String[] fields = presets[i].split("\\|", 3);
                    labels[i] = fields.length == 3
                            ? getString(R.string.preset_bank_program, fields[2], fields[0], fields[1])
                            : presets[i];
                }
                new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(soundFontManager.getDisplayName(library))
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
                                        showOperationStatus(getString(R.string.preset_selected, presetName));
                                        updateTrackControls();
                                    } else {
                                        showOperationStatus(getString(R.string.preset_load_failed));
                                    }
                                });
                            });
                        } catch (Exception error) {
                            showOperationStatus(getString(R.string.preset_select_error, error.getMessage()));
                        }
                    }).setNegativeButton(R.string.back, (dialog, which) -> showSoundFontLibraryPicker(channel)).show();
            });
        });
    }

    private void updateDeviceSelector(List<MidiDeviceInfo> devices) {
        if (deviceSelector == null || isFinishing() || isDestroyed()) return;
        int previousId = usbMidiInput == null ? -1 : usbMidiInput.getSelectedDeviceId();
        availableMidiDevices.clear();
        availableMidiDevices.addAll(devices);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.internal_keyboard));
        for (MidiDeviceInfo info : devices) {
            String name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
            if (name == null || name.trim().isEmpty()) name = getString(R.string.device_name_fallback, info.getId());
            String type = info.getType() == MidiDeviceInfo.TYPE_USB ? getString(R.string.device_usb) : getString(R.string.device_internal_virtual);
            labels.add(getString(R.string.device_label, type, name));
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
        trackEnableButton.setText(getString(selected.isEnabled() ? R.string.channel_enabled : R.string.channel_muted, channel + 1));
        trackEnableButton.setContentDescription(getString(R.string.track_enable_accessibility, channel + 1, selected.isEnabled() ? getString(R.string.enabled_state) : getString(R.string.muted_state)));
        for (int i = 0; i < trackButtons.length; i++) {
            Track item = trackManager.get(i);
            String state = item.hasSoundFont() ? compactTrackName(item.getPresetName() == null ? item.getSoundFontName() : item.getPresetName()) : getString(R.string.track_empty_state);
            if (!item.isEnabled()) state += getString(R.string.mute_suffix);
            String prefix = i == channel ? "● " : "";
            trackButtons[i].setText(getString(R.string.track_button_label, prefix, i + 1, state));
            trackButtons[i].setMaxLines(2);
            trackButtons[i].setEllipsize(android.text.TextUtils.TruncateAt.END);
            trackButtons[i].setTextColor(TEXT);
            trackButtons[i].setContentDescription(getString(R.string.track_accessibility, i + 1) + ": " + state);
            trackButtons[i].setBackground(AppUi.ripple(this, i == channel ? ACCENT : CONTROL, 10,
                    i == channel ? ACCENT : AppUi.CONTROL_BORDER));
        }
        importTargetText.setText(getString(R.string.import_target_channel, channel + 1));
        importTargetText.setContentDescription(getString(R.string.import_target_accessibility, channel + 1));
        choose.setText(getString(selected.hasSoundFont() ? R.string.replace_sf2_channel : R.string.import_sf2_channel, channel + 1));
        choose.setContentDescription(getString(selected.hasSoundFont() ? R.string.replace_sf2_accessibility : R.string.import_sf2_accessibility, channel + 1));
        String soundFont = selected.hasSoundFont() ? selected.getSoundFontName() : getString(R.string.soundfont_missing);
        String preset = selected.getPresetName();
        String instrument = preset == null || preset.trim().isEmpty()
                ? soundFont : preset + " — " + soundFont;
        status.setText(getString(R.string.channel_instrument_status, channel + 1, instrument));
        if (selectInstrumentButton != null) {
            selectInstrumentButton.setText(getString(selected.hasSoundFont() ? R.string.replace_instrument : R.string.choose_instrument));
        }
    }

    private void setTrackControlsEnabled(boolean enabled) {
        AppUi.setButtonEnabled(choose, enabled);
        AppUi.setButtonEnabled(selectInstrumentButton, enabled);
        AppUi.setButtonEnabled(trackEnableButton, enabled);
        if (trackButtons != null) {
            for (Button button : trackButtons) {
                AppUi.setButtonEnabled(button, enabled);
            }
        }
    }

    private void updateBleSearchButton() {
        if (bleSearchButton == null) return;
        boolean enabled = bleMidiInput != null && bleMidiInput.isScanningEnabled();
        bleSearchButton.setText(getString(enabled ? R.string.ble_search_on : R.string.ble_search_off));
        bleSearchButton.setContentDescription(getString(enabled ? R.string.ble_search_accessibility_on : R.string.ble_search_accessibility_off));
    }

    private void showOperationStatus(String message) {
        if (operationStatus == null) return;
        statusHandler.removeCallbacks(hideOperationStatus);
        operationStatus.setText(message);
        operationStatus.setVisibility(View.VISIBLE);
        statusHandler.postDelayed(hideOperationStatus, 4000L);
    }

    private void applySystemInsets(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets safe = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = safe.left;
                top = safe.top;
                right = safe.right;
                bottom = safe.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && insets.getDisplayCutout() != null) {
                    android.view.DisplayCutout cutout = insets.getDisplayCutout();
                    left = Math.max(left, cutout.getSafeInsetLeft());
                    top = Math.max(top, cutout.getSafeInsetTop());
                    right = Math.max(right, cutout.getSafeInsetRight());
                    bottom = Math.max(bottom, cutout.getSafeInsetBottom());
                }
            }
            view.setPadding(left, top, right, bottom);
            return insets;
        });
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_SHOWING_SETTINGS, showingSettings);
        super.onSaveInstanceState(outState);
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().getDecorView().requestApplyInsets();
    }

    @Override protected void onDestroy() {
        statusHandler.removeCallbacks(hideOperationStatus);
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