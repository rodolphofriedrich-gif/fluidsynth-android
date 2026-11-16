package com.rodolphofriedrich.fluidsynth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** BLE-MIDI central/input. Scans for standard BLE-MIDI peripherals and routes their MIDI stream. */
public final class BleMidiInput implements AutoCloseable {
    private static final String TAG = "BleMidiInput";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 8204;
    private static final UUID BLE_MIDI_SERVICE =
            UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");

    public interface Listener {
        void onMidiMessage(int status, int data1, int data2);
        void onDeviceStatus(String message);
    }

    private final Activity activity;
    private final Listener listener;
    private final MidiManager midiManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, OpenDevice> openDevices = new HashMap<>();
    private final Set<String> openingAddresses = new HashSet<>();
    private BluetoothLeScanner scanner;
    private boolean started;
    private boolean scanning;

    public BleMidiInput(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        this.midiManager = (MidiManager) activity.getSystemService(Context.MIDI_SERVICE);
        BluetoothManager manager = (BluetoothManager)
                activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = manager == null ? null : manager.getAdapter();
    }

    public void start() {
        if (started) return;
        started = true;
        if (Build.VERSION.SDK_INT < 23 || midiManager == null || bluetoothAdapter == null
                || !activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            listener.onDeviceStatus("BLE MIDI indisponível neste aparelho");
            return;
        }
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }
        startScan();
    }

    public void onPermissionResult(boolean granted) {
        if (!started) return;
        if (granted) startScan();
        else listener.onDeviceStatus("Permissão Bluetooth negada");
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
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

    public boolean handlesPermissionRequest(int requestCode) {
        return requestCode == REQUEST_BLUETOOTH_PERMISSIONS;
    }

    private void startScan() {
        if (!started || scanning) return;
        try {
            if (!bluetoothAdapter.isEnabled()) {
                listener.onDeviceStatus("Ative o Bluetooth para procurar dispositivos MIDI");
                return;
            }
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                listener.onDeviceStatus("Scanner Bluetooth indisponível");
                return;
            }
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(BLE_MIDI_SERVICE)).build();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan(java.util.Collections.singletonList(filter), settings, scanCallback);
            scanning = true;
            listener.onDeviceStatus("Procurando controladores BLE MIDI...");
        } catch (SecurityException error) {
            Log.e(TAG, "Permissão Bluetooth ausente durante a busca", error);
            listener.onDeviceStatus("Permissão Bluetooth necessária");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String address;
            try {
                address = device.getAddress();
            } catch (SecurityException error) {
                return;
            }
            if (openDevices.containsKey(address) || !openingAddresses.add(address)) return;
            stopScan();
            listener.onDeviceStatus("Dispositivo BLE MIDI encontrado; conectando...");
            midiManager.openBluetoothDevice(device, midiDevice -> {
                openingAddresses.remove(address);
                if (!started) {
                    if (midiDevice != null) midiDevice.close();
                    return;
                }
                if (midiDevice == null) {
                    listener.onDeviceStatus("Não foi possível abrir o dispositivo BLE MIDI");
                    startScan();
                    return;
                }
                connectMidiDevice(address, midiDevice);
            }, mainHandler);
        }

        @Override public void onScanFailed(int errorCode) {
            scanning = false;
            listener.onDeviceStatus("Falha na busca BLE (código " + errorCode + ")");
        }
    };

    private void connectMidiDevice(String address, MidiDevice device) {
        try {
            MidiDeviceInfo.PortInfo selected = null;
            for (MidiDeviceInfo.PortInfo port : device.getInfo().getPorts()) {
                if (port.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                    selected = port;
                    break;
                }
            }
            if (selected == null) {
                device.close();
                listener.onDeviceStatus("Dispositivo BLE sem porta de entrada MIDI");
                startScan();
                return;
            }
            MidiReceiver receiver = new MidiStreamParser((status, d1, d2) ->
                    listener.onMidiMessage(status, d1, d2));
            MidiDevice.MidiOutputPort output = device.openOutputPort(selected.getPortNumber());
            if (output == null) {
                device.close();
                listener.onDeviceStatus("Porta BLE MIDI indisponível");
                startScan();
                return;
            }
            output.connect(receiver);
            openDevices.put(address, new OpenDevice(device, output));
            String name = "controlador BLE";
            try {
                String deviceName = device.getInfo().getProperties()
                        .getString(MidiDeviceInfo.PROPERTY_NAME);
                if (deviceName != null && !deviceName.isEmpty()) name = deviceName;
            } catch (Exception ignored) {}
            listener.onDeviceStatus("Conectado: " + name);
        } catch (Exception error) {
            try { device.close(); } catch (IOException ignored) {}
            Log.e(TAG, "Falha ao conectar BLE MIDI", error);
            listener.onDeviceStatus("Erro ao conectar BLE MIDI");
            startScan();
        }
    }

    private void stopScan() {
        if (!scanning || scanner == null) return;
        try { scanner.stopScan(scanCallback); }
        catch (SecurityException error) { Log.w(TAG, "Não foi possível parar a busca BLE", error); }
        scanning = false;
    }

    private void releaseAllNotes() {
        for (int channel = 0; channel < 16; channel++) {
            listener.onMidiMessage(0xB0 | channel, 123, 0);
            listener.onMidiMessage(0xB0 | channel, 120, 0);
        }
    }

    @Override public void close() {
        if (!started) return;
        started = false;
        stopScan();
        for (OpenDevice opened : openDevices.values()) {
            try { opened.output.close(); } catch (IOException ignored) {}
            try { opened.device.close(); } catch (IOException ignored) {}
        }
        openDevices.clear();
        openingAddresses.clear();
        releaseAllNotes();
    }

    private static final class OpenDevice {
        final MidiDevice device;
        final MidiDevice.MidiOutputPort output;
        OpenDevice(MidiDevice device, MidiDevice.MidiOutputPort output) {
            this.device = device;
            this.output = output;
        }
    }

    private interface MessageSink { void accept(int status, int data1, int data2); }

    /** Decodes BLE-MIDI payloads, including timestamp bytes and running-status messages. */
    private static final class MidiStreamParser extends MidiReceiver {
        private final MessageSink sink;
        private int runningStatus = -1;
        private int pendingStatus = -1;
        private int expectedData;
        private int dataCount;
        private final int[] data = new int[2];

        MidiStreamParser(MessageSink sink) { this.sink = sink; }

        @Override public void onSend(byte[] msg, int offset, int count, long timestamp)
                throws IOException {
            int end = offset + count;
            for (int i = offset; i < end; i++) {
                int value = msg[i] & 0xFF;
                // BLE-MIDI timestamp headers have bit 7 set and follow the packet header.
                if ((value & 0x80) != 0) {
                    if (value >= 0xF8) continue;
                    if (value >= 0xF0) {
                        runningStatus = -1;
                        pendingStatus = -1;
                        dataCount = 0;
                        continue;
                    }
                    if (value >= 0x80 && value <= 0xEF) {
                        runningStatus = value;
                        beginMessage(value);
                    }
                    // Other high-bit bytes are BLE timestamp headers, not MIDI status.
                    continue;
                }
                if (pendingStatus < 0) {
                    if (runningStatus < 0) continue;
                    beginMessage(runningStatus);
                }
                if (dataCount < data.length) data[dataCount++] = value;
                if (dataCount == expectedData) {
                    sink.accept(pendingStatus, data[0], expectedData > 1 ? data[1] : 0);
                    beginMessage(runningStatus);
                }
            }
        }

        private void beginMessage(int status) {
            pendingStatus = status;
            int command = status & 0xF0;
            expectedData = (command == 0xC0 || command == 0xD0) ? 1 : 2;
            dataCount = 0;
        }
    }
}