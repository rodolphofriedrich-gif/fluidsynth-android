# USB MIDI input implementation

This feature is isolated from the audio engine.

## Files

- `UsbMidiInput.java`: discovers USB MIDI devices through Android's MIDI service, opens controller output ports, parses MIDI channel messages, and closes devices safely.
- `MainActivity.java`: owns the lifecycle of the USB input manager and forwards received MIDI events to JNI. The existing virtual keyboard and audio thread remain unchanged.
- `native_player.cpp`: receives MIDI channel events and routes Note On, Note Off, Control Change, Program Change, and Pitch Bend to FluidSynth under the existing synth mutex.
- `AndroidManifest.xml`: declares USB host and MIDI as optional device features.

## Design choice and reference

The implementation was informed by the architecture and compatibility notes in [kshoji/USB-MIDI-Driver](https://github.com/kshoji/USB-MIDI-Driver), which is Apache-2.0 licensed. It uses Android's built-in `MidiManager` API rather than vendoring the full third-party driver. This avoids adding a separate USB stack or an unnecessary dependency.

Official Android documentation:
- [Android MIDI overview](https://source.android.com/docs/core/audio/midi)
- [Android MIDI API](https://developer.android.com/reference/android/media/midi/package-summary)

## Current scope

- USB MIDI controller input (controller output ports).
- MIDI 1.0 channel voice messages, including running status.
- Note On/Off, velocity, channel, sustain/control changes, program changes, and pitch bend.
- Releases active notes on device removal.
- Audio continues through the phone's existing AudioTrack output.

## Device compatibility

Android's MIDI service exposes USB MIDI devices when the Android build and kernel support USB host MIDI. Some devices or vendor-specific USB implementations may not be exposed by the platform service. If a real controller is not detected on the target phone, evaluate integrating the kshoji driver as a fallback, preserving this class boundary.

## Validation still required

This code must pass the GitHub Actions build and be tested on the target Android phone with a real USB-OTG MIDI controller. No hardware test is claimed by this document.
