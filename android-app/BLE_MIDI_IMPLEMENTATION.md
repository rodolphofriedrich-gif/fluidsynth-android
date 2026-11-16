# BLE MIDI input

## Scope

This feature adds BLE MIDI controller input to the Android FluidSynth player. It is isolated in `BleMidiInput.java`; the existing audio render thread, FluidSynth engine, USB input class, and virtual keyboard remain unchanged.

The app acts as a BLE MIDI **Central**: it scans for peripherals advertising the standard BLE-MIDI service UUID, opens the device through Android's MIDI service, and connects the device's MIDI output port to a receiver. MIDI channel messages are forwarded through the existing JNI MIDI-event entry point to FluidSynth.

## Implementation choices

- Uses Android `BluetoothLeScanner` for discovery and `MidiManager.openBluetoothDevice()` for the BLE-MIDI transport.
- Uses the platform MIDI service rather than embedding a second BLE protocol stack.
- Requests `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` at runtime on Android 12+; Android 6–11 use the location permission required for BLE scanning.
- Scans specifically for BLE-MIDI service UUID `03B80E5A-EDE8-4B33-A751-6CE34EC4C700`.
- Opens the peripheral's MIDI output port (the controller sends data from its output to the app).
- Keeps the USB MIDI implementation independent.

## Files

- `BleMidiInput.java`: BLE discovery, permission checks, connection lifecycle, MIDI receiver.
- `MainActivity.java`: small integration, BLE status line, permission-result forwarding.
- `AndroidManifest.xml`: optional BLE feature and runtime permissions.
- `.github/workflows/android-apk.yml`: builds this feature branch.

## Current limitations / validation

- Hardware validation is intentionally pending until a BLE MIDI controller is available.
- First implementation connects to one discovered BLE MIDI controller at a time.
- The app must be in the foreground while scanning; background/foreground service behavior is not included.
- Devices that do not advertise the standard BLE-MIDI service UUID will not be discovered by this first implementation.
- Reconnection after a peripheral disconnect and long-session stability require physical testing.
- Verify Note On/Off, velocity, chords, sustain/CC, repeated notes, disconnect while notes are held, and reconnection.
- Measure latency separately from USB. BLE adds radio and packet scheduling latency; do not interpret it as FluidSynth render latency.

## Reference projects and documentation

- [BLE-MIDI-for-Android](https://github.com/kshoji/BLE-MIDI-for-Android) — Apache-2.0 reference implementation with Central and Peripheral roles. Its recent versions include low-latency timestamp scheduling and packet congestion handling.
- [Android MIDI overview](https://source.android.com/docs/core/audio/midi) — confirms Android platform support for BLE MIDI.
- [Android MidiManager API](https://developer.android.com/reference/android/media/midi/MidiManager) — documents `openBluetoothDevice()`.
- [Android MIDI package guide](https://developer.android.com/reference/android/media/midi/package-summary) — documents receiving MIDI data through `MidiReceiver`.

No merge into `android-apk-player` until build validation and physical BLE controller tests pass.
