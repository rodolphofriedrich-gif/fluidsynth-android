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
    private EffectsScreen effectsScreen;
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
    private Button trackEnableButton;
    private Button soloButton;
    private Button selectInstrumentButton;
    private SeekBar masterVolumeSeekBar;
    private TextView masterVolumeValue;
    private ChannelMixController channelMixController;
    private Button bleSearchButton;
    private Button[] trackButtons = new Button[16];
    private Button[] trackMuteButtons = new Button[16];
    private Button[] trackSoloButtons = new Button[16];
    private SeekBar[] trackVolumeSeekBars = new SeekBar[16];
    private TextView[] trackVolumeValues = new TextView[16];
    private LinearLayout workspace;
    private EffectsWorkspaceController effectsWorkspaceController;
    private SeekBar polyphonySeekBar;
    private TextView polyphonyValue;
    private SeekBar velocitySeekBar;
    private TextView velocityValue;
    private int currentVelocity = 100;
    private ProgressBar importProgressBar;
    private TextView importProgressText;
    private View importProgressPanel;
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
    private int pendingImportChannel = -1;
    private static final String STATE_SHOWING_SETTINGS = "showing_settings";
    private static final String STATE_SHOWING_EFFECTS = "showing_effects";
    private static final int IMPORT_LIBRARY_ONLY = -2;
    private boolean showingSettings;
    private boolean showingEffects;
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
        showingEffects = state != null && state.getBoolean(STATE_SHOWING_EFFECTS, false);
        if (showingEffects) showingSettings = false;

        midiScreen = new MidiScreen(this);
        settingsScreen = new SettingsScreen(this);
        effectsScreen = new EffectsScreen(this);

        status = midiScreen.status;
        audioDiagnostics = midiScreen.audioDiagnostics;
        trackButtons = midiScreen.trackButtons;
        trackMuteButtons = midiScreen.trackMuteButtons;
        trackSoloButtons = midiScreen.trackSoloButtons;
        trackVolumeSeekBars = midiScreen.trackVolumeSeekBars;
        trackVolumeValues = midiScreen.trackVolumeValues;
        selectInstrumentButton = midiScreen.selectInstrumentButton;
        trackEnableButton = midiScreen.trackEnableButton;
        soloButton = midiScreen.soloButton;
        masterVolumeSeekBar = midiScreen.masterVolumeSeekBar;
        masterVolumeValue = midiScreen.masterVolumeValue;
        velocitySeekBar = midiScreen.velocitySeekBar;
        velocityValue = midiScreen.velocityValue;

        deviceSelector = settingsScreen.deviceSelector;
        usbStatus = settingsScreen.usbStatus;
        bleStatus = settingsScreen.bleStatus;
        polyphonySeekBar = settingsScreen.polyphonySeekBar;
        polyphonyValue = settingsScreen.polyphonyValue;
        bleSearchButton = settingsScreen.bleSearchButton;
        importProgressText = midiScreen.importProgressText;
        importProgressBar = midiScreen.importProgressBar;
        importProgressPanel = midiScreen.importProgressPanel;
        libraryListContainer = settingsScreen.libraryListContainer;
        settingsScreen.importLibraryButton.setOnClickListener(v -> openLibraryDocument());

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
                    showSoundFontLibraryPicker(channel);
                }
            });
            trackMuteButtons[i].setOnClickListener(v -> {
                if (trackManager == null) return;
                boolean enabled = trackManager.get(channel).isEnabled();
                if (channelMixController != null) channelMixController.setMuted(channel, enabled);
                else trackManager.setEnabled(channel, !enabled);
                updateTrackControls();
            });
            trackSoloButtons[i].setOnClickListener(v -> {
                if (channelMixController == null) return;
                channelMixController.toggleSolo(channel);
                updateTrackControls();
            });
            trackVolumeSeekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    trackVolumeValues[channel].setText(progress + "%");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    int volume = seekBar.getProgress();
                    if (initialized) nativeBridge.nativeSetTrackVolume(channel, volume);
                    ChannelVolumeSettingsStore.save(MainActivity.this, channel, volume);
                    showOperationStatus(getString(R.string.channel_volume_saved, channel + 1, volume));
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
            if (channelMixController != null) channelMixController.setMuted(channel, !enabled);
            else trackManager.setEnabled(channel, enabled);
            updateTrackControls();
        });
        soloButton.setOnClickListener(v -> {
            if (trackManager == null || channelMixController == null) return;
            channelMixController.toggleSolo(trackManager.getActiveChannel());
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

        int savedMasterVolume = MasterVolumeSettingsStore.load(this);
        masterVolumeSeekBar.setProgress(savedMasterVolume);
        masterVolumeValue.setText(getString(R.string.master_volume_value, savedMasterVolume));
        masterVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                masterVolumeValue.setText(getString(R.string.master_volume_value, progress));
                if (fromUser && audioEngine != null) audioEngine.setMasterVolume(progress / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                MasterVolumeSettingsStore.save(MainActivity.this, seekBar.getProgress());
                showOperationStatus(getString(R.string.master_volume_saved, seekBar.getProgress()));
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
        ScrollView effectsScroll = effectsScreen.scrollView;
        workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.HORIZONTAL);
        workspace.setBackgroundColor(BG);
        workspace.addView(midiScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        workspace.addView(effectsScroll, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 0f));
        effectsWorkspaceController = new EffectsWorkspaceController(this, workspace,
                midiScroll, effectsScroll, midiScreen.effectsToggleButton);
        effectsWorkspaceController.setOpen(showingEffects);
        midiScreen.effectsToggleButton.setOnClickListener(v -> {
            if (showingSettings) return;
            effectsWorkspaceController.toggle();
            showingEffects = effectsWorkspaceController.isOpen();
        });

        LinearLayout screenHost = new LinearLayout(this);
        screenHost.setOrientation(LinearLayout.VERTICAL);
        screenHost.setBackgroundColor(BG);
        screenHost.addView(midiScreen.topControls, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        screenHost.addView(workspace, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        screenHost.addView(settingsScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        settingsScroll.setVisibility(showingSettings ? View.VISIBLE : View.GONE);
        workspace.setVisibility(showingSettings ? View.GONE : View.VISIBLE);
        midiScreen.topControls.setVisibility(showingSettings ? View.GONE : View.VISIBLE);

        operationStatus.setPadding(AppUi.dp(this, 16), AppUi.dp(this, 8), AppUi.dp(this, 16), AppUi.dp(this, 8));
        operationStatus.setBackground(rounded(PANEL, 10, AppUi.BORDER));
        operationStatus.setVisibility(View.GONE);
        LinearLayout.LayoutParams operationStatusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        operationStatusParams.setMargins(AppUi.dp(this, 16), AppUi.dp(this, 4), AppUi.dp(this, 16), AppUi.dp(this, 8));
        operationStatus.setMaxLines(2);
        operationStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        operationStatus.setGravity(Gravity.CENTER_VERTICAL);
        operationStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        screenHost.addView(operationStatus, 0, operationStatusParams);

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

        Button effectsTab = new Button(this);
        effectsTab.setText(getString(R.string.tab_effects));
        effectsTab.setAllCaps(false);
        effectsTab.setTextColor(TEXT);
        effectsTab.setBackground(AppUi.ripple(this, showingEffects ? ACCENT : CONTROL, 10, 0));

        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0,
                AppUi.dp(this, 48), 1f);
        tabParams.setMargins(AppUi.dp(this, 3), 0, AppUi.dp(this, 3), 0);
        navigation.addView(midiTab, tabParams);
        navigation.addView(settingsTab, new LinearLayout.LayoutParams(tabParams));
        effectsTab.setVisibility(View.GONE);
        midiTab.setOnClickListener(v -> showMainScreen(0, workspace, settingsScroll,
                midiScreen.topControls, midiTab, settingsTab));
        settingsTab.setOnClickListener(v -> showMainScreen(1, workspace, settingsScroll,
                midiScreen.topControls, midiTab, settingsTab));

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
            midiRouter.setActivityListener((channel, command) ->
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed() && midiScreen != null) {
                            midiScreen.showMidiActivity(channel);
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
                polyphonyValue.setText(getString(R.string.polyphony_value, appliedPolyphony));
                showOperationStatus(getString(R.string.operation_polyphony_restore_failed));
            }
            restoreEffectsSettings();
            effectsScreen.setListener(new EffectsScreen.Listener() {
                @Override public void onEffectActive(int effect, boolean active) {
                    EffectsSettingsStore.saveActive(MainActivity.this, effect, active);
                    if (effect == EffectsScreen.REVERB) nativeBridge.nativeSetReverbActive(active);
                    else nativeBridge.nativeSetChorusActive(active);
                }
                @Override public void onParamChanged(int effect, int param, float value) {
                    float[] values = effectsScreen.values(effect);
                    if (effect == EffectsScreen.REVERB) nativeBridge.nativeSetReverb(values[0], values[1], values[2], values[3]);
                    else nativeBridge.nativeSetChorus(Math.round(values[0]), values[1], values[2], values[3]);
                    EffectsSettingsStore.saveValues(MainActivity.this, effect, values);
                }
            });
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
            channelMixController = new ChannelMixController(trackManager, nativeBridge);
            for (int channel = 0; channel < 16; channel++) {
                nativeBridge.nativeSetTrackVolume(channel, ChannelVolumeSettingsStore.load(this, channel));
            }
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
            audioEngine.setMasterVolume(MasterVolumeSettingsStore.load(this) / 100f);
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
                        if (!isFinishing() && !isDestroyed()) {
                            usbStatus.setText(getString(R.string.usb_status, message));
                            usbStatus.setTextColor((message.startsWith("Conectado") || message.startsWith("Controlador conectado")) ? Color.rgb(16, 185, 129) : MUTED);
                        }
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
                        if (!isFinishing() && !isDestroyed()) {
                            bleStatus.setText(getString(R.string.ble_status, message));
                            bleStatus.setTextColor((message.startsWith("Conectado") || message.startsWith("Controlador conectado")) ? Color.rgb(16, 185, 129) : MUTED);
                        }
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
        if (importProgressPanel != null) importProgressPanel.setVisibility(View.VISIBLE);
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
        if (importProgressPanel != null) importProgressPanel.setVisibility(View.GONE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SF2 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        showImportProgress();
        final int requestedChannel = pendingImportChannel;
        final int channel = requestedChannel >= 0 ? requestedChannel
                : (trackManager == null ? 0 : trackManager.getActiveChannel());
        pendingImportChannel = -1;
        if (requestedChannel == IMPORT_LIBRARY_ONLY) {
            loader.execute(() -> {
                String result;
                try {
                    String displayName = getSoundFontDisplayName(data.getData());
                    soundFontManager.importForTrack(data.getData(), 0, displayName,
                            (copiedBytes, totalBytes) -> updateImportProgress(copiedBytes, totalBytes));
                    result = getString(R.string.library_imported, displayName);
                } catch (Exception error) {
                    result = getString(R.string.import_error, error.getMessage());
                }
                final String message = result;
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        hideImportProgress();
                        refreshSoundFontLibraryList();
                        showOperationStatus(message);
                    }
                });
            });
            return;
        }
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

            Button deleteButton = new Button(this);
            deleteButton.setText(R.string.library_delete);
            deleteButton.setTextSize(10);
            deleteButton.setAllCaps(false);
            deleteButton.setTextColor(Color.WHITE);
            deleteButton.setPadding(AppUi.dp(this, 6), 0, AppUi.dp(this, 6), 0);
            deleteButton.setBackground(AppUi.ripple(this, Color.rgb(145, 55, 65), 8, 0));
            deleteButton.setContentDescription(getString(R.string.library_delete_accessibility,
                    soundFontManager.getDisplayName(library)));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    AppUi.dp(this, 72), AppUi.dp(this, 38));
            deleteParams.leftMargin = AppUi.dp(this, 6);
            row.addView(deleteButton, deleteParams);
            deleteButton.setOnClickListener(v -> new android.app.AlertDialog.Builder(this,
                    android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(R.string.library_delete_title)
                    .setMessage(getString(R.string.library_delete_confirm,
                            soundFontManager.getDisplayName(library)))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.library_delete, (dialog, which) -> {
                        if (soundFontLibraryStore.isLibraryInUse(library, trackManager)) {
                            showOperationStatus(getString(R.string.library_in_use));
                        } else if (soundFontLibraryStore.deleteLibrary(library, trackManager)) {
                            showOperationStatus(getString(R.string.library_deleted));
                        } else {
                            showOperationStatus(getString(R.string.library_delete_failed));
                        }
                        refreshSoundFontLibraryList();
                    }).show());
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

    private void openLibraryDocument() {
        pendingImportChannel = IMPORT_LIBRARY_ONLY;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_SF2);
    }

    private void openSoundFontDocument(int channel) {
        pendingImportChannel = Math.max(0, Math.min(15, channel));
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_SF2);
    }

    private void showSoundFontLibraryPicker(int channel) {
        if (soundFontLibraryStore == null) return;
        List<File> availableLibraries = new ArrayList<>();
        for (File library : soundFontLibraryStore.getLibraries()) {
            if (soundFontLibraryStore.isEnabled(library)) availableLibraries.add(library);
        }
        File[] files = availableLibraries.toArray(new File[0]);
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = soundFontManager.getDisplayName(files[i]);
        Track selected = trackManager == null ? null : trackManager.get(channel);
        String subtitle = selected == null || !selected.hasSoundFont()
                ? getString(R.string.track_empty_state)
                : selected.getPresetName() == null ? selected.getSoundFontName()
                : selected.getPresetName() + " — " + selected.getSoundFontName();
        ChannelInstrumentSheet.show(this, channel, subtitle, names,
                new ChannelInstrumentSheet.Listener() {
                    @Override public void onLibrarySelected(int index) {
                        if (index >= 0 && index < files.length) showPresetPicker(channel, files[index]);
                    }
                    @Override public void onImportRequested() {
                        openSoundFontDocument(channel);
                    }
                });
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
        if (soloButton != null) {
            boolean solo = channelMixController != null && channelMixController.isSolo(channel);
            soloButton.setText(getString(solo ? R.string.solo_active : R.string.solo_channel));
            soloButton.setBackground(AppUi.ripple(this, solo ? ACCENT : CONTROL, 12, 0));
            soloButton.setContentDescription(getString(solo ? R.string.solo_accessibility_active : R.string.solo_accessibility, channel + 1));
        }
        if (channelMixController != null) channelMixController.applyIfSoloActive();
        for (int i = 0; i < trackButtons.length; i++) {
            Track item = trackManager.get(i);
            String state = item.hasSoundFont() ? compactTrackName(item.getPresetName() == null ? item.getSoundFontName() : item.getPresetName()) : getString(R.string.track_empty_state);
            if (!item.isEnabled()) state += getString(R.string.mute_suffix);
            if (channelMixController != null && channelMixController.isSolo(i)) state += getString(R.string.solo_suffix);
            String prefix = i == channel ? "● " : "";
            trackButtons[i].setText(getString(R.string.track_button_label, prefix, i + 1, state));
            trackButtons[i].setMaxLines(2);
            trackButtons[i].setEllipsize(android.text.TextUtils.TruncateAt.END);
            trackButtons[i].setTextColor(TEXT);
            trackButtons[i].setContentDescription(getString(R.string.track_accessibility, i + 1) + ": " + state);
            trackButtons[i].setBackground(AppUi.ripple(this, i == channel ? ACCENT : CONTROL, 10,
                    i == channel ? ACCENT : AppUi.CONTROL_BORDER));
            boolean enabled = item.isEnabled();
            trackMuteButtons[i].setText(enabled ? "CH ✓" : "CH ×");
            trackMuteButtons[i].setBackground(AppUi.ripple(this, enabled ? CONTROL : Color.rgb(145, 55, 65), 7, 0));
            trackMuteButtons[i].setContentDescription(getString(R.string.channel_toggle_accessibility, i + 1)
                    + (enabled ? getString(R.string.enabled_state) : getString(R.string.muted_state)));
            boolean solo = channelMixController != null && channelMixController.isSolo(i);
            trackSoloButtons[i].setText(solo ? "SOLO ✓" : "SOLO");
            trackSoloButtons[i].setBackground(AppUi.ripple(this, solo ? ACCENT : CONTROL, 7, 0));
            trackSoloButtons[i].setContentDescription(getString(solo
                    ? R.string.solo_accessibility_active : R.string.solo_accessibility, i + 1));
            int volume = ChannelVolumeSettingsStore.load(this, i);
            if (trackVolumeSeekBars[i].getProgress() != volume) trackVolumeSeekBars[i].setProgress(volume);
            trackVolumeValues[i].setText(volume + "%");
        }
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
        AppUi.setButtonEnabled(selectInstrumentButton, enabled);
        AppUi.setButtonEnabled(trackEnableButton, enabled);
        AppUi.setButtonEnabled(soloButton, enabled);
        if (trackButtons != null) {
            for (int i = 0; i < trackButtons.length; i++) {
                AppUi.setButtonEnabled(trackButtons[i], enabled);
                AppUi.setButtonEnabled(trackMuteButtons[i], enabled);
                AppUi.setButtonEnabled(trackSoloButtons[i], enabled);
                trackVolumeSeekBars[i].setEnabled(enabled);
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

    private void showMainScreen(int screen, View midiWorkspace, View settings,
            View topControls, Button midiTab, Button settingsTab) {
        showingSettings = screen == 1;
        effectsWorkspaceController.setOpen(false);
        showingEffects = false;
        if (showingSettings) {
            midiWorkspace.setVisibility(View.GONE);
            topControls.setVisibility(View.GONE);
            settings.setVisibility(View.VISIBLE);
        } else {
            settings.setVisibility(View.GONE);
            midiWorkspace.setVisibility(View.VISIBLE);
            topControls.setVisibility(View.VISIBLE);
        }
        midiTab.setBackground(AppUi.ripple(this, screen == 0 ? ACCENT : CONTROL, 10, 0));
        settingsTab.setBackground(AppUi.ripple(this, screen == 1 ? ACCENT : CONTROL, 10, 0));
        midiTab.setSelected(screen == 0);
        settingsTab.setSelected(screen == 1);
        midiTab.setContentDescription(getString(R.string.tab_midi_accessibility,
                screen == 0 ? getString(R.string.tab_selected) : ""));
        settingsTab.setContentDescription(getString(R.string.tab_settings_accessibility,
                screen == 1 ? getString(R.string.tab_selected) : ""));
    }

    private void restoreEffectsSettings() {
        for (int effect = 0; effect < 2; effect++) {
            float[] values = EffectsSettingsStore.loadValues(this, effect);
            for (int param = 0; param < 4; param++) effectsScreen.setParam(effect, param, values[param]);
            boolean active = EffectsSettingsStore.loadActive(this, effect);
            effectsScreen.setActive(effect, active);
            if (effect == EffectsScreen.REVERB) {
                nativeBridge.nativeSetReverb(values[0], values[1], values[2], values[3]);
                nativeBridge.nativeSetReverbActive(active);
            } else {
                nativeBridge.nativeSetChorus(Math.round(values[0]), values[1], values[2], values[3]);
                nativeBridge.nativeSetChorusActive(active);
            }
        }
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
        outState.putBoolean(STATE_SHOWING_EFFECTS,
                effectsWorkspaceController != null && effectsWorkspaceController.isOpen());
        super.onSaveInstanceState(outState);
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (midiScreen != null) midiScreen.updateTrackGridOrientation(newConfig);
        if (effectsWorkspaceController != null) effectsWorkspaceController.onConfigurationChanged();
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