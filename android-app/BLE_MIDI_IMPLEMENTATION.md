# BLE MIDI input

## Implementation

The BLE input now uses **Kshoji BLE-MIDI-for-Android 0.0.21** as the transport and protocol implementation. The previous hand-written scanner, Android MidiManager bridge, and custom BLE-MIDI packet parser have been removed.

- BLE role: Central (the app receives MIDI from BLE MIDI peripherals).
- Library: jp.kshoji:ble-midi:0.0.21, resolved from the upstream Kshoji Maven repository.
- License: Apache License 2.0 (upstream library).
- The adapter is BleMidiInput.java; it forwards Note On/Off, Control Change, Program Change, and Pitch Bend to the existing JNI entry point.
- MIDI timestamp handling is configured to LOW_LATENCY with a 40 ms schedule-ahead ceiling. This avoids the unbounded scheduling backlog possible with the library's default SCHEDULED mode.
- The FluidSynth engine, native audio render thread, USB MIDI implementation, SoundFont loading, and virtual keyboard are unchanged.
- Android 12+ requests Scan and Connect permissions; Android 6–11 requests Fine Location for BLE scanning.
- The app does not request the Companion Device setup feature; scanning uses the BLE scanner flow.

## Dependency repositories

The Gradle settings include the upstream Maven repository for BLE-MIDI and the companion javax-sound-midi-for-Android repository required by the library's published AAR metadata. The dependency is pinned to release 0.0.21 rather than a moving development build.

## Current validation status

- Source integration completed; GitHub Actions build is the next validation gate.
- Physical BLE controller tests remain pending, as requested.
- Do not merge this branch into android-apk-player until the Android build succeeds and a physical controller test verifies Note On/Off, velocity, chords, sustain/CC, repeated notes, disconnect while notes are held, and reconnection.
- BLE radio/packet scheduling adds transport latency; assess it separately from FluidSynth rendering latency.
- The native FluidSynth MIDI dispatcher currently consumes Note On/Off, CC, Program Change, and Pitch Bend. SysEx, aftertouch, and system-realtime callbacks are intentionally not forwarded because the current native dispatcher has no handlers for them.

## Upstream references

- https://github.com/kshoji/BLE-MIDI-for-Android
- https://github.com/kshoji/BLE-MIDI-for-Android/releases/tag/v0.0.21
- https://github.com/kshoji/BLE-MIDI-for-Android/blob/develop/README.md
- https://source.android.com/docs/core/audio/midi

No merge into android-apk-player until build validation and physical BLE controller tests pass.
