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
import java.util.ArrayList;
import java.util.List;

/**
 * USB MIDI input only. The Android MIDI service owns USB enumeration and permission handling.
 * A controller's output port is opened because it sends MIDI data to this application.
 */
public final class UsbMidiInput implements AutoCloseable {
    private static final String TAG = "UsbMidiInput";

    public interface Listener {
        void onMidiMessage(Object source, int status, int data1, int data2);
        void onSourceDisconnected(Object source);
        void onDeviceStatus(String message);
        void onDevicesChanged(List<MidiDeviceInfo> devices);
    }

    private final MidiManager midiManager;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Integer, OpenDevice> openDevices = new HashMap<>();
    private boolean started;
    private int selectedDeviceId = -1;

    private final MidiManager.DeviceCallback deviceCallback = new MidiManager.DeviceCallback() {
        @Override public void onDeviceAdded(MidiDeviceInfo info) {
            publishDevices();
            openIfMidiInput(info);
        }

        @Override public void onDeviceRemoved(MidiDeviceInfo info) {
            boolean wasSelected = info.getId() == selectedDeviceId;
            closeDevice(info.getId());
            if (wasSelected) selectedDeviceId = -1;
            publishDevices();
            if (wasSelected) listener.onDeviceStatus("Controlador desconectado");
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
        publishDevices();
        if (selectedDeviceId >= 0) {
            for (MidiDeviceInfo info : midiManager.getDevices()) openIfMidiInput(info);
        }
    }

    private void openIfMidiInput(MidiDeviceInfo info) {
        if (!started || info.getId() != selectedDeviceId
                || (info.getType() != MidiDeviceInfo.TYPE_USB && info.getType() != MidiDeviceInfo.TYPE_VIRTUAL)
                || info.getOutputPortCount() == 0 || openDevices.containsKey(info.getId())) {
            return;
        }

        midiManager.openDevice(info, device -> {
            if (device == null) {
                listener.onDeviceStatus("Não foi possível abrir o controlador USB");
                return;
            }
            if (!started || selectedDeviceId != info.getId()) {
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

                MidiStreamParser parser = new MidiStreamParser((source, status, d1, d2) ->
                        listener.onMidiMessage(source, status, d1, d2));
                MidiReceiver receiver = parser;
                MidiOutputPort output = device.openOutputPort(selected.getPortNumber());
                if (output == null) {
                    closeMidiDeviceQuietly(device);
                    listener.onDeviceStatus("Porta MIDI USB indisponível");
                    return;
                }
                output.connect(receiver);
                openDevices.put(deviceId, new OpenDevice(device, output, parser));
                listener.onDeviceStatus("Controlador conectado: " + device.getInfo().getProperties()
                        .getString(MidiDeviceInfo.PROPERTY_NAME));
            } catch (Exception error) {
                closeMidiDeviceQuietly(device);
                Log.e(TAG, "Falha ao abrir porta MIDI USB", error);
                listener.onDeviceStatus("Falha ao abrir controlador USB");
            }
        }, mainHandler);
    }

    public List<MidiDeviceInfo> getAvailableDevices() {
        List<MidiDeviceInfo> result = new ArrayList<>();
        if (midiManager == null) return result;
        for (MidiDeviceInfo info : midiManager.getDevices()) {
            if ((info.getType() == MidiDeviceInfo.TYPE_USB || info.getType() == MidiDeviceInfo.TYPE_VIRTUAL)
                    && info.getOutputPortCount() > 0) result.add(info);
        }
        return result;
    }

    public int getSelectedDeviceId() { return selectedDeviceId; }

    public void selectDevice(int deviceId) {
        if (selectedDeviceId == deviceId) return;
        selectedDeviceId = deviceId;
        for (Integer id : openDevices.keySet().toArray(new Integer[0])) closeDevice(id);
        if (deviceId < 0) {
            listener.onDeviceStatus("Teclado interno selecionado");
            return;
        }
        if (midiManager != null) {
            for (MidiDeviceInfo info : getAvailableDevices()) {
                if (info.getId() == deviceId) { openIfMidiInput(info); return; }
            }
        }
        listener.onDeviceStatus("Controlador não disponível");
    }

    private void publishDevices() {
        listener.onDevicesChanged(getAvailableDevices());
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
        opened.parser.releaseActiveNotes();
        listener.onSourceDisconnected(opened.parser);
        try { opened.output.close(); } catch (IOException ignored) {}
        closeMidiDeviceQuietly(opened.device);
        // Device-specific note cleanup is handled by the MIDI source parser; do not
        // send global All Notes Off here because another controller may be playing.
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
        final MidiStreamParser parser;
        OpenDevice(MidiDevice device, MidiOutputPort output, MidiStreamParser parser) {
            this.device = device;
            this.output = output;
            this.parser = parser;
        }
    }

    private interface MessageSink {
        void accept(Object source, int status, int data1, int data2);
    }

    /** Handles MIDI channel messages, including running status and messages split across USB packets. */
    private static final class MidiStreamParser extends MidiReceiver {
        private final MessageSink sink;
        private int runningStatus = -1;
        private int pendingStatus = -1;
        private int expectedData;
        private int dataCount;
        private final int[] data = new int[2];
        private final boolean[][] activeNotes = new boolean[16][128];
        private final boolean[] sustainActive = new boolean[16];

        MidiStreamParser(MessageSink sink) { this.sink = sink; }

        synchronized void releaseActiveNotes() {
            for (int channel = 0; channel < 16; channel++) {
                if (sustainActive[channel]) {
                    sink.accept(0xB0 | channel, 64, 0);
                    sustainActive[channel] = false;
                }
                for (int note = 0; note < 128; note++) {
                    if (activeNotes[channel][note]) {
                        sink.accept(0x80 | channel, note, 0);
                        activeNotes[channel][note] = false;
                    }
                }
            }
        }

        private synchronized void dispatch(int status, int d1, int d2) {
            int command = status & 0xF0;
            int channel = status & 0x0F;
            if (command == 0x90 && d2 > 0) activeNotes[channel][d1] = true;
            else if (command == 0x80 || (command == 0x90 && d2 == 0)) activeNotes[channel][d1] = false;
            else if (command == 0xB0 && d1 == 64) sustainActive[channel] = d2 >= 64;
            sink.accept(this, status, d1, d2);
        }

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
                        dispatch(pendingStatus, data[0], expectedData > 1 ? data[1] : 0);
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
