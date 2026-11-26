#include "synth_engine.h"
#include <algorithm>
#include <cstring>
#include <cmath>

SynthEngine& SynthEngine::instance() {
    static SynthEngine engine;
    return engine;
}

bool SynthEngine::initialize() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) return true;
    soundfontIds_.fill(FLUID_FAILED);
    enabled_.fill(true);
    bankMsb_.fill(0);
    bankLsb_.fill(0);
    programs_.fill(0);
    channelVolumes_.fill(127);
    coarseTune_.fill(0);
    fineTune_.fill(0.0f);
    filterCutoffHz_.fill(20000.0f); filterResonance_.fill(0.0f);
    envelopeAttack_.fill(0.01f); envelopeDecay_.fill(0.1f);
    envelopeSustain_.fill(100.0f); envelopeRelease_.fill(0.1f);
    lfoRateHz_.fill(8.176f); lfoDepthCents_.fill(0.0f);
    reverbSend_.fill(0); chorusSend_.fill(0);
    settings_ = new_fluid_settings();
    if (!settings_) return false;
    fluid_settings_setnum(settings_, "synth.sample-rate", 48000.0);
    fluid_settings_setint(settings_, "synth.polyphony", 512);
    fluid_settings_setnum(settings_, "synth.gain", 0.7);
    fluid_settings_setint(settings_, "synth.reverb.active", 1);
    fluid_settings_setint(settings_, "synth.chorus.active", 1);
    synth_ = new_fluid_synth(settings_);
    if (!synth_) {
        delete_fluid_settings(settings_);
        settings_ = nullptr;
        return false;
    }
    return true;
}

void SynthEngine::setReverb(double roomSize, double damping, double width, double level) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) fluid_synth_set_reverb(synth_, roomSize, damping, width, level);
}
void SynthEngine::setChorus(int voices, double level, double speed, double depthMs) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) fluid_synth_set_chorus(synth_, voices, level, speed, depthMs, FLUID_CHORUS_MOD_SINE);
}
void SynthEngine::setReverbActive(bool active) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) fluid_synth_set_reverb_on(synth_, active ? 1 : 0);
}
void SynthEngine::setChorusActive(bool active) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) fluid_synth_set_chorus_on(synth_, active ? 1 : 0);
}

bool SynthEngine::setPolyphony(int voices) {
    voices = std::clamp(((voices + 4) / 8) * 8, 8, 2048);
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return false;
    return fluid_synth_set_polyphony(synth_, voices) == FLUID_OK;
}

int SynthEngine::loadSoundFontForChannel(const std::string& path, int channel) {
    if (channel < 0 || channel >= 16 || path.empty()) return FLUID_FAILED;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return FLUID_FAILED;

    int newId = FLUID_FAILED;
    auto cached = pathToId_.find(path);
    if (cached != pathToId_.end()) {
        newId = cached->second;
        ++idReferences_[newId];
    } else {
        newId = fluid_synth_sfload(synth_, path.c_str(), 0);
        if (newId == FLUID_FAILED) return FLUID_FAILED;
        pathToId_[path] = newId;
        idReferences_[newId] = 1;
    }

    if (fluid_synth_program_select(synth_, channel, newId, 0, 0) != FLUID_OK) {
        releaseSoundFont(newId);
        return FLUID_FAILED;
    }
    fluid_synth_cc(synth_, channel, 7, channelVolumes_[channel]);
    const int oldId = soundfontIds_[channel];
    soundfontIds_[channel] = newId;
    bankMsb_[channel] = 0;
    bankLsb_[channel] = 0;
    programs_[channel] = 0;
    if (oldId >= 0) releaseSoundFont(oldId);
    enabled_[channel] = true;
    applyTrackTuning(channel);
    applyTrackFilterAdsr(channel);
    applyTrackLfo(channel);
    applyTrackEffectSends(channel);
    return newId;
}

std::vector<SoundFontPreset> SynthEngine::listPresets(const std::string& path) {
    std::vector<SoundFontPreset> result;
    if (path.empty()) return result;

    // Preset enumeration uses an isolated, temporary synth. Loading or walking
    // a large SF2 therefore never acquires the live audio engine mutex.
    fluid_settings_t* listingSettings = new_fluid_settings();
    if (!listingSettings) return result;

    fluid_settings_setnum(listingSettings, "synth.sample-rate", 48000.0);
    fluid_settings_setint(listingSettings, "synth.polyphony", 16);
    fluid_settings_setint(listingSettings, "synth.reverb.active", 0);
    fluid_settings_setint(listingSettings, "synth.chorus.active", 0);

    fluid_synth_t* listingSynth = new_fluid_synth(listingSettings);
    if (!listingSynth) {
        delete_fluid_settings(listingSettings);
        return result;
    }

    const int id = fluid_synth_sfload(listingSynth, path.c_str(), 0);
    if (id != FLUID_FAILED) {
        fluid_sfont_t* soundfont = fluid_synth_get_sfont_by_id(listingSynth, id);
        if (soundfont) {
            fluid_sfont_iteration_start(soundfont);
            fluid_preset_t* preset = nullptr;
            while ((preset = fluid_sfont_iteration_next(soundfont)) != nullptr) {
                const char* name = fluid_preset_get_name(preset);
                result.push_back({
                    fluid_preset_get_banknum(preset),
                    fluid_preset_get_num(preset),
                    name ? name : "Preset sem nome"
                });
            }
        }
    }

    delete_fluid_synth(listingSynth);
    delete_fluid_settings(listingSettings);
    return result;
}
bool SynthEngine::selectPreset(int channel, int bank, int program) {
    if (channel < 0 || channel >= 16 || bank < 0 || program < 0 || program > 127) return false;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_ || soundfontIds_[channel] < 0) return false;
    if (fluid_synth_program_select(synth_, channel, soundfontIds_[channel], bank, program) != FLUID_OK) return false;
    bankMsb_[channel] = (bank >> 7) & 0x7f; bankLsb_[channel] = bank & 0x7f; programs_[channel] = program;
    fluid_synth_cc(synth_, channel, 7, channelVolumes_[channel]);
    applyTrackTuning(channel);
    applyTrackFilterAdsr(channel);
    applyTrackLfo(channel);
    applyTrackEffectSends(channel);
    return true;
}
void SynthEngine::releaseSoundFont(int id) {
    auto it = idReferences_.find(id);
    if (it == idReferences_.end()) return;
    if (--it->second <= 0) {
        for (auto path = pathToId_.begin(); path != pathToId_.end();) {
            if (path->second == id) path = pathToId_.erase(path);
            else ++path;
        }
        idReferences_.erase(it);
        // The ID is no longer assigned to any channel; avoid resetting every other channel's preset.
        if (synth_) fluid_synth_sfunload(synth_, id, 0);
    }
}

void SynthEngine::setTrackEnabled(int channel, bool enabled) {
    if (channel < 0 || channel >= 16) return;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return;
    if (!enabled) {
        fluid_synth_cc(synth_, channel, 64, 0);
        fluid_synth_all_sounds_off(synth_, channel);
    }
    enabled_[channel] = enabled;
}

void SynthEngine::setTrackVolume(int channel, int volume) {
    if (channel < 0 || channel >= 16) return;
    volume = std::clamp(volume, 0, 100);
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return;
    const int midiVolume = (volume * 127 + 50) / 100;
    channelVolumes_[channel] = midiVolume;
    fluid_synth_cc(synth_, channel, 7, midiVolume);
}

void SynthEngine::applyTrackTuning(int channel) {
    if (!synth_ || channel < 0 || channel >= 16) return;
    fluid_synth_set_gen(synth_, channel, GEN_COARSETUNE,
            static_cast<float>(coarseTune_[channel]));
    fluid_synth_set_gen(synth_, channel, GEN_FINETUNE, fineTune_[channel]);
}

void SynthEngine::applyTrackFilterAdsr(int channel) {
    if (!synth_ || channel < 0 || channel >= 16) return;
    const float cutoffCents = 1200.0f * std::log2(filterCutoffHz_[channel] / 8.176f);
    const auto timecents = [](float seconds) {
        return 1200.0f * std::log2(std::max(seconds, 0.001f));
    };
    // fluid_synth_set_gen applies an offset to the SoundFont generator; 13500 cents is the SF2 default cutoff.
    fluid_synth_set_gen(synth_, channel, GEN_FILTERFC, cutoffCents - 13500.0f);
    fluid_synth_set_gen(synth_, channel, GEN_FILTERQ, filterResonance_[channel] * 9.6f);
    // Volume-envelope time generators default to -12000 timecents (instant).
    // Convert the desired absolute time into the offset expected by set_gen.
    fluid_synth_set_gen(synth_, channel, GEN_VOLENVATTACK, timecents(envelopeAttack_[channel]) + 12000.0f);
    fluid_synth_set_gen(synth_, channel, GEN_VOLENVDECAY, timecents(envelopeDecay_[channel]) + 12000.0f);
    fluid_synth_set_gen(synth_, channel, GEN_VOLENVSUSTAIN,
            (100.0f - envelopeSustain_[channel]) * 14.4f);
    fluid_synth_set_gen(synth_, channel, GEN_VOLENVRELEASE, timecents(envelopeRelease_[channel]) + 12000.0f);
}

bool SynthEngine::setTrackFilterAdsr(int channel, float cutoffHz, float resonance,
        float attackSeconds, float decaySeconds, float sustainPercent, float releaseSeconds) {
    if (channel < 0 || channel >= 16) return false;
    std::lock_guard<std::mutex> lock(mutex_);
    filterCutoffHz_[channel] = std::clamp(cutoffHz, 20.0f, 20000.0f);
    filterResonance_[channel] = std::clamp(resonance, 0.0f, 100.0f);
    envelopeAttack_[channel] = std::clamp(attackSeconds, 0.001f, 10.0f);
    envelopeDecay_[channel] = std::clamp(decaySeconds, 0.001f, 10.0f);
    envelopeSustain_[channel] = std::clamp(sustainPercent, 0.0f, 100.0f);
    envelopeRelease_[channel] = std::clamp(releaseSeconds, 0.001f, 10.0f);
    if (!synth_) return false;
    applyTrackFilterAdsr(channel);
    return true;
}

bool SynthEngine::setTrackLfo(int channel, float rateHz, float depthCents) {
    if (channel < 0 || channel >= 16) return false;
    std::lock_guard<std::mutex> lock(mutex_);
    lfoRateHz_[channel] = std::clamp(rateHz, 0.1f, 20.0f);
    lfoDepthCents_[channel] = std::clamp(depthCents, 0.0f, 100.0f);
    if (!synth_) return false;
    applyTrackLfo(channel);
    return true;
}

void SynthEngine::applyTrackLfo(int channel) {
    if (!synth_ || channel < 0 || channel >= 16) return;
    // FluidSynth generator values are additive offsets; 0 cents is the SF2 default of 8.176 Hz.
    const float frequencyOffset = 1200.0f * std::log2(lfoRateHz_[channel] / 8.176f);
    fluid_synth_set_gen(synth_, channel, GEN_MODLFOFREQ, frequencyOffset);
    // The modulation LFO is routed to pitch: this produces vibrato without altering MIDI routing.
    fluid_synth_set_gen(synth_, channel, GEN_MODLFOTOPITCH, lfoDepthCents_[channel]);
}

bool SynthEngine::setTrackEffectSends(int channel, int reverbSend, int chorusSend) {
    if (channel < 0 || channel >= 16) return false;
    std::lock_guard<std::mutex> lock(mutex_);
    reverbSend_[channel] = std::clamp(reverbSend, 0, 127);
    chorusSend_[channel] = std::clamp(chorusSend, 0, 127);
    if (!synth_) return false;
    applyTrackEffectSends(channel);
    return true;
}

void SynthEngine::applyTrackEffectSends(int channel) {
    if (!synth_ || channel < 0 || channel >= 16) return;
    // Standard MIDI channel controls: CC91 = Reverb Send, CC93 = Chorus Send.
    fluid_synth_cc(synth_, channel, 91, reverbSend_[channel]);
    fluid_synth_cc(synth_, channel, 93, chorusSend_[channel]);
}

bool SynthEngine::setTrackTuning(int channel, int coarseSemitones, float fineCents) {
    if (channel < 0 || channel >= 16) return false;
    coarseSemitones = std::clamp(coarseSemitones, -24, 24);
    fineCents = std::clamp(fineCents, -100.0f, 100.0f);
    std::lock_guard<std::mutex> lock(mutex_);
    coarseTune_[channel] = coarseSemitones;
    fineTune_[channel] = fineCents;
    if (!synth_) return false;
    applyTrackTuning(channel);
    return true;
}

void SynthEngine::setActiveChannel(int channel) {
    if (channel < 0 || channel >= 16) return;
    std::lock_guard<std::mutex> lock(mutex_);
    activeChannel_ = channel;
}

void SynthEngine::note(int channel, int noteNumber, int velocity, bool on) {
    if (channel < 0 || channel >= 16 || noteNumber < 0 || noteNumber > 127) return;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return;
    if (on) {
        if (enabled_[channel]) fluid_synth_noteon(synth_, channel, noteNumber, std::clamp(velocity, 1, 127));
    } else {
        fluid_synth_noteoff(synth_, channel, noteNumber);
    }
}

bool SynthEngine::midiEvent(int status, int data1, int data2) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return false;

    const int command = status & 0xF0;
    const int channel = status & 0x0F;
    const int a = std::clamp(data1, 0, 127);
    const int b = std::clamp(data2, 0, 127);

    if (!enabled_[channel]) {
        if (command == 0x80 || (command == 0x90 && b == 0)) {
            fluid_synth_noteoff(synth_, channel, a);
            return true;
        }
        if (command == 0xB0 && a == 64 && b < 64) {
            fluid_synth_cc(synth_, channel, a, b);
            return true;
        }
        return false;
    }

    switch (command) {
        case 0x80:
            fluid_synth_noteoff(synth_, channel, a);
            return true;
        case 0x90:
            if (b == 0) fluid_synth_noteoff(synth_, channel, a);
            else fluid_synth_noteon(synth_, channel, a, b);
            return true;
        case 0xB0:
            fluid_synth_cc(synth_, channel, a, b);
            if (a == 7) channelVolumes_[channel] = b;
            if (a == 91) reverbSend_[channel] = b;
            if (a == 93) chorusSend_[channel] = b;
            if (a == 0) bankMsb_[channel] = b;
            else if (a == 32) bankLsb_[channel] = b;
            // Bank Select is preparatory. Apply the selected bank once when
            // the following Program Change arrives, not on every CC message.
            return true;
        case 0xC0:
            if (soundfontIds_[channel] >= 0 &&
                fluid_synth_program_select(synth_, channel, soundfontIds_[channel],
                    (bankMsb_[channel] << 7) | bankLsb_[channel], a) == FLUID_OK) {
                programs_[channel] = a;
                applyTrackTuning(channel);
                applyTrackFilterAdsr(channel);
                applyTrackLfo(channel);
                applyTrackEffectSends(channel);
                return true;
            }
            return false;
        case 0xA0:
            fluid_synth_key_pressure(synth_, channel, a, b);
            return true;
        case 0xD0:
            fluid_synth_channel_pressure(synth_, channel, a);
            return true;
        case 0xE0:
            fluid_synth_pitch_bend(synth_, channel, a | (b << 7));
            return true;
        default:
            return false;
    }
}

int SynthEngine::getTrackBank(int channel) {
    if (channel < 0 || channel >= 16) return -1;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return -1;
    return (bankMsb_[channel] << 7) | bankLsb_[channel];
}

void SynthEngine::render(float* output, int frames) {
    if (!output || frames <= 0) return;
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) fluid_synth_write_float(synth_, frames, output, 0, 2, output, 1, 2);
    else std::fill(output, output + frames * 2, 0.0f);
}

void SynthEngine::shutdown() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (synth_) { delete_fluid_synth(synth_); synth_ = nullptr; }
    if (settings_) { delete_fluid_settings(settings_); settings_ = nullptr; }
    soundfontIds_.fill(FLUID_FAILED);
    enabled_.fill(true);
    bankMsb_.fill(0);
    bankLsb_.fill(0);
    programs_.fill(0);
    channelVolumes_.fill(127);
    coarseTune_.fill(0);
    fineTune_.fill(0.0f);
    filterCutoffHz_.fill(20000.0f); filterResonance_.fill(0.0f);
    envelopeAttack_.fill(0.01f); envelopeDecay_.fill(0.1f);
    envelopeSustain_.fill(100.0f); envelopeRelease_.fill(0.1f);
    lfoRateHz_.fill(8.176f); lfoDepthCents_.fill(0.0f);
    reverbSend_.fill(0); chorusSend_.fill(0);
    pathToId_.clear();
    idReferences_.clear();
    activeChannel_ = 0;
}