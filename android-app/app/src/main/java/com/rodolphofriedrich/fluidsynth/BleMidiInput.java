package com.rodolphofriedrich.fluidsynth;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jp.kshoji.blemidi.central.BleMidiCentralProvider;
import jp.kshoji.blemidi.device.MidiInputDevice;
import jp.kshoji.blemidi.device.MidiOutputDevice;
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.blemidi.listener.OnMidiInputEventListener;
import jp.kshoji.blemidi.util.BleMidiTimestampCoordinator;

/** BLE MIDI adapter using Kshoji for discovery, GATT transport and MIDI parsing. */
public final class BleMidiInput implements AutoCloseable {
    private static final String TAG = "BleMidiInput";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 8204;
    private static final int LOW_LATENCY_SCHEDULE_AHEAD_MS = 40;

    public interface Listener {
        void onMidiMessage(Object source, int status, int data1, int data2);
        void onSourceDisconnected(Object source);
        void onDeviceStatus(String message);
    }

    private final Activity activity;
    private final Listener listener;
    private final Set<MidiInputDevice> connectedDevices = ConcurrentHashMap.newKeySet();
    private final Map<MidiInputDevice, Set<Integer>> activeNotesByDevice = new ConcurrentHashMap<>();
    private final Map<MidiInputDevice, Set<Integer>> sustainChannelsByDevice = new ConcurrentHashMap<>();
    private final AtomicLong receivedMidiMessages = new AtomicLong();
    private volatile boolean started;
    private volatile boolean closed;
    private volatile boolean scanningEnabled = false;
    private BleMidiCentralProvider provider;

    public BleMidiInput(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public synchronized void setScanningEnabled(boolean enabled) {
        scanningEnabled = enabled;
        if (closed) return;
        BleMidiCentralProvider current = provider;
        if (enabled && started && current == null) {
            if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                listener.onDeviceStatus("Este aparelho não oferece Bluetooth LE");
            } else if (!hasPermissions()) {
                requestPermissions();
            } else {
                startProvider();
            }
            return;
        }
        if (current == null) return;
        try {
            if (enabled && connectedDevices.isEmpty()) {
                current.startScanDevice(0);
                listener.onDeviceStatus("Procurando controladores BLE MIDI...");
            } else if (!enabled) {
                current.stopScanDevice();
                listener.onDeviceStatus("Busca BLE pausada");
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Falha ao alterar busca BLE", error);
            listener.onDeviceStatus("Não foi possível alterar a busca BLE MIDI");
        }
    }

    public boolean isScanningEnabled() { return scanningEnabled; }

    public synchronized void start() {
        if (started || closed) return;
        started = true;
        if (!scanningEnabled) {
            listener.onDeviceStatus("Busca BLE desativada");
            return;
        }
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            listener.onDeviceStatus("Este aparelho não oferece Bluetooth LE");
            return;
        }
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }
        startProvider();
    }

    public void onPermissionResult(boolean granted) {
        if (!started || closed) return;
        if (!granted) {
            scanningEnabled = false;
            listener.onDeviceStatus("Permissão Bluetooth negada");
            return;
        }
        startProvider();
    }

    public boolean handlesPermissionRequest(int requestCode) {
        return requestCode == REQUEST_BLUETOOTH_PERMISSIONS;
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.requestPermissions(new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            activity.requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private synchronized void startProvider() {
        if (closed || provider != null) return;
        try {
            provider = new BleMidiCentralProvider(activity);
            provider.setAutoStartInputDevice(false);
            provider.setOnMidiDeviceAttachedListener(new OnMidiDeviceAttachedListener() {
                @Override
                public void onMidiInputDeviceAttached(MidiInputDevice device) {
                    attachInputDevice(device);
                }
                @Override
                public void onMidiOutputDeviceAttached(MidiOutputDevice device) {
                    // The app receives MIDI; BLE output endpoints are not used.
                }
            });
            provider.setOnMidiDeviceDetachedListener(new OnMidiDeviceDetachedListener() {
                @Override
                public void onMidiInputDeviceDetached(MidiInputDevice device) {
                    releaseDeviceNotes(device);
                    connectedDevices.remove(device);
                    listener.onSourceDisconnected(device);
                    listener.onDeviceStatus("Controlador desconectado: " + device.getDeviceName());
                    restartScanIfNeeded();
                }
                @Override
                public void onMidiOutputDeviceDetached(MidiOutputDevice device) {
                    // No BLE MIDI output endpoint is opened by this app.
                }
            });
            if (scanningEnabled) {
                provider.startScanDevice(0);
                listener.onDeviceStatus("Procurando controladores BLE MIDI...");
            } else listener.onDeviceStatus("Busca BLE desativada");
        } catch (RuntimeException error) {
            Log.e(TAG, "Não foi possível iniciar o driver BLE MIDI", error);
            provider = null;
            scanningEnabled = false;
            listener.onDeviceStatus("Não foi possível iniciar BLE MIDI: " + safeMessage(error));
        }
    }

    private void attachInputDevice(MidiInputDevice device) {
        if (closed) {
            device.terminate();
            return;
        }
        connectedDevices.add(device);
        activeNotesByDevice.putIfAbsent(device, ConcurrentHashMap.newKeySet());
        sustainChannelsByDevice.putIfAbsent(device, ConcurrentHashMap.newKeySet());
        device.setTimestampSchedulingMode(BleMidiTimestampCoordinator.SchedulingMode.LOW_LATENCY);
        device.setMaxScheduleAheadMs(LOW_LATENCY_SCHEDULE_AHEAD_MS);
        device.setOnMidiInputEventListener(new OnMidiInputEventListener() {
            @Override public void onMidiNoteOff(MidiInputDevice sender, int channel, int note, int velocity) {
                Set<Integer> active = activeNotesByDevice.get(sender);
                if (active != null) active.remove(channel * 128 + note);
                send(sender, 0x80 | channel, note, velocity);
            }
            @Override public void onMidiNoteOn(MidiInputDevice sender, int channel, int note, int velocity) {
                if (velocity <= 0) {
                    Set<Integer> active = activeNotesByDevice.get(sender);
                    if (active != null) active.remove(channel * 128 + note);
                    send(sender, 0x80 | channel, note, 0);
                } else {
                    Set<Integer> active = activeNotesByDevice.get(sender);
                    if (active != null) active.add(channel * 128 + note);
                    send(sender, 0x90 | channel, note, velocity);
                }
            }
            @Override public void onMidiControlChange(MidiInputDevice sender, int channel, int function, int value) {
                if (function == 64) {
                    Set<Integer> sustain = sustainChannelsByDevice.get(sender);
                    if (sustain != null) {
                        if (value >= 64) sustain.add(channel);
                        else sustain.remove(channel);
                    }
                }
                send(sender, 0xB0 | channel, function, value);
            }
            @Override public void onMidiProgramChange(MidiInputDevice sender, int channel, int program) {
                send(sender, 0xC0 | channel, program, 0);
            }
            @Override public void onMidiPitchWheel(MidiInputDevice sender, int channel, int amount) {
                send(sender, 0xE0 | channel, amount & 0x7F, (amount >> 7) & 0x7F);
            }
            @Override public void onMidiSystemExclusive(MidiInputDevice sender, byte[] data) {}
            @Override public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int channel, int note, int pressure) {
                send(sender, 0xA0 | channel, note, pressure);
            }
            @Override public void onMidiChannelAftertouch(MidiInputDevice sender, int channel, int pressure) {
                send(sender, 0xD0 | channel, pressure, 0);
            }
            @Override public void onMidiTimeCodeQuarterFrame(MidiInputDevice sender, int timing) {}
            @Override public void onMidiSongSelect(MidiInputDevice sender, int song) {}
            @Override public void onMidiSongPositionPointer(MidiInputDevice sender, int position) {}
            @Override public void onMidiTuneRequest(MidiInputDevice sender) {}
            @Override public void onMidiTimingClock(MidiInputDevice sender) {}
            @Override public void onMidiStart(MidiInputDevice sender) {}
            @Override public void onMidiContinue(MidiInputDevice sender) {}
            @Override public void onMidiStop(MidiInputDevice sender) {}
            @Override public void onMidiActiveSensing(MidiInputDevice sender) {}
            @Override public void onMidiReset(MidiInputDevice sender) {}
            @Override public void onRPNMessage(MidiInputDevice sender, int channel, int function, int value) {}
            @Override public void onNRPNMessage(MidiInputDevice sender, int channel, int function, int value) {}
        });
        device.start();
        BleMidiCentralProvider current = provider;
        if (current != null) {
            try { current.stopScanDevice(); }
            catch (RuntimeException error) {
                Log.w(TAG, "Não foi possível encerrar a busca após conexão", error);
            }
        }
        Log.i(TAG, "BLE_INPUT_STARTED device=" + device.getDeviceName());
        listener.onDeviceStatus("Conectado: " + device.getDeviceName());
    }

    private void send(MidiInputDevice source, int status, int data1, int data2) {
        if (closed) return;
        long count = receivedMidiMessages.incrementAndGet();
        int messageType = status & 0xF0;
        if (count <= 10 || count % 32 == 0 || messageType == 0x90 || messageType == 0x80) {
            Log.i(TAG, "BLE_MIDI_RX count=" + count
                    + " type=0x" + Integer.toHexString(messageType)
                    + " channel=" + ((status & 0x0F) + 1)
                    + " data1=" + data1 + " data2=" + data2);
        }
        listener.onMidiMessage(source, status, data1, data2);
    }

    private void restartScanIfNeeded() {
        BleMidiCentralProvider current = provider;
        if (!closed && scanningEnabled && connectedDevices.isEmpty() && current != null) {
            try {
                current.startScanDevice(0);
                listener.onDeviceStatus("Procurando controlador BLE MIDI novamente...");
            } catch (RuntimeException error) {
                Log.w(TAG, "Falha ao reiniciar a busca BLE", error);
                listener.onDeviceStatus("Não foi possível reiniciar a busca BLE MIDI");
            }
        }
    }

    private void releaseDeviceNotes(MidiInputDevice device) {
        Set<Integer> sustain = sustainChannelsByDevice.remove(device);
        if (sustain != null) {
            for (Integer channel : sustain) send(device, 0xB0 | channel, 64, 0);
        }
        Set<Integer> active = activeNotesByDevice.remove(device);
        if (active != null) {
            for (Integer encoded : active) {
                int channel = encoded / 128;
                int note = encoded % 128;
                send(device, 0x80 | channel, note, 0);
            }
        }
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName() : message;
    }

    @Override public synchronized void close() {
        if (closed) return;
        for (MidiInputDevice device : connectedDevices) {
            releaseDeviceNotes(device);
            listener.onSourceDisconnected(device);
        }
        closed = true;
        started = false;
        BleMidiCentralProvider current = provider;
        provider = null;
        if (current != null) {
            try {
                for (MidiInputDevice device : current.getMidiInputDevices()) {
                    try { device.stop(); } catch (RuntimeException ignored) {}
                }
                current.terminate();
            } catch (RuntimeException error) {
                Log.w(TAG, "Erro ao encerrar o driver BLE MIDI", error);
            }
        }
        connectedDevices.clear();
        activeNotesByDevice.clear();
        sustainChannelsByDevice.clear();
    }
}
