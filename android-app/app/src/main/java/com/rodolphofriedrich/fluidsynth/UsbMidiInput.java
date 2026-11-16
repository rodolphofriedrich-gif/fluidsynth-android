package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * USB MIDI input only. The Android MIDI service owns USB enumeration and permission handling.
 * A controller's output port is opened because it sends MIDI data to this application.
 */
public final class UsbMidiInput implements AutoCloseable {
    private static final String TAG = "UsbMidiInput";

    public interface Listener {
        void onMidiMessage(int status, int data1, int data2);
        void onDeviceStatus(String message);
    }

    private final MidiManager midiManager;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Integer, OpenDevice> openDevices = new HashMap<>();
    private boolean started;

    private final MidiManager.DeviceCallback deviceCallback = new MidiManager.DeviceCallback() {
        @Override public void onDeviceAdded(MidiDeviceInfo info) {
            openIfMidiInput(info);
        }

        @Override public void onDeviceRemoved(MidiDeviceInfo info) {
            closeDevice(info.getId());
            listener.onDeviceStatus("Controlador USB desconectado");
        }
    };

    public UsbMidiInput(Context context, Listener listener) {
        this.midiManager = (MidiManager) context.getApplicationContext()
                .getSystemService(Context.MIDI_SERVICE);
        this.listener = listener;
    }

    public void start() {
        if (started) return;
        started = true;
        if (midiManager == null) {
            listener.onDeviceStatus("Serviço MIDI indisponível neste Android");
            return;
        }
        midiManager.registerDeviceCallback(deviceCallback, mainHandler);
        for (MidiDeviceInfo info : midiManager.getDevices()) {
            openIfMidiInput(info);
        }
    }

    private void openIfMidiInput(MidiDeviceInfo info) {
        if (!started || info.getType() != MidiDeviceInfo.TYPE_USB
                || info.getOutputPortCount() == 0 || openDevices.containsKey(info.getId())) {
            return;
        }

        midiManager.openDevice(info, device -> {
            if (device == null) {
                listener.onDeviceStatus("Não foi possível abrir o controlador USB");
                return;
            }
            if (!started) {
                closeMidiDeviceQuietly(device);
                return;
            }
            int deviceId = info.getId();
            try {
                MidiDeviceInfo.PortInfo[] ports = info.getPorts();
                MidiDeviceInfo.PortInfo selected = null;
                for (MidiDeviceInfo.PortInfo port : ports) {
                    if (port.getType() == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                        selected = port;
                        break;
                    }
                }
                if (selected == null) {
                    closeMidiDeviceQuietly(device);
                    return;
                }

                MidiReceiver receiver = new MidiStreamParser((status, d1, d2) ->
                        listener.onMidiMessage(status, d1, d2));
                MidiOutputPort output = device.openOutputPort(selected.getPortNumber());
                if (output == null) {
                    closeMidiDeviceQuietly(device);
                    listener.onDeviceStatus("Porta MIDI USB indisponível");
                    return;
                }
                output.connect(receiver);
                openDevices.put(deviceId, new OpenDevice(device, output));
                listener.onDeviceStatus("Controlador conectado: " + device.getInfo().getProperties()
                        .getString(MidiDeviceInfo.PROPERTY_NAME));
            } catch (Exception error) {
                closeMidiDeviceQuietly(device);
                Log.e(TAG, "Falha ao abrir porta MIDI USB", error);
                listener.onDeviceStatus("Falha ao abrir controlador USB");
            }
        }, mainHandler);
    }

    private static void closeMidiDeviceQuietly(MidiDevice device) {
        try {
            device.close();
        } catch (IOException error) {
            Log.w(TAG, "Falha ao fechar dispositivo MIDI USB", error);
        }
    }

    private void closeDevice(int id) {
        OpenDevice opened = openDevices.remove(id);
        if (opened == null) return;
        try { opened.output.close(); } catch (IOException ignored) {}
        try { opened.closeMidiDeviceQuietly(device); } catch (IOException ignored) {}
        // Release any notes that may have remained active after a cable/device removal.
        for (int channel = 0; channel < 16; channel++) {
            listener.onMidiMessage(0xB0 | channel, 123, 0);
            listener.onMidiMessage(0xB0 | channel, 120, 0);
        }
    }

    @Override public void close() {
        if (!started) return;
        started = false;
        if (midiManager != null) midiManager.unregisterDeviceCallback(deviceCallback);
        for (Integer id : openDevices.keySet().toArray(new Integer[0])) closeDevice(id);
    }

    private static final class OpenDevice {
        final MidiDevice device;
        final MidiOutputPort output;
        OpenDevice(MidiDevice device, MidiOutputPort output) {
            this.device = device;
            this.output = output;
        }
    }

    private interface MessageSink {
        void accept(int status, int data1, int data2);
    }

    /** Handles MIDI channel messages, including running status and messages split across USB packets. */
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
                if (value >= 0xF8) continue; // MIDI real-time bytes may occur anywhere.
                if ((value & 0x80) != 0) {
                    if (value >= 0xF0) {
                        runningStatus = -1;
                        pendingStatus = -1;
                        dataCount = 0;
                        continue;
                    }
                    runningStatus = value;
                    beginMessage(value);
                } else {
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
        }

        private void beginMessage(int status) {
            pendingStatus = status;
            int command = status & 0xF0;
            expectedData = (command == 0xC0 || command == 0xD0) ? 1 : 2;
            dataCount = 0;
        }
    }
}
