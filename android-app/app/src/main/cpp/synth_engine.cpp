#include "synth_engine.h"
#include <algorithm>
#include <cstring>

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
    const int oldId = soundfontIds_[channel];
    soundfontIds_[channel] = newId;
    bankMsb_[channel] = 0;
    bankLsb_[channel] = 0;
    programs_[channel] = 0;
    if (oldId >= 0) releaseSoundFont(oldId);
    enabled_[channel] = true;
    return newId;
}

std::vector<SoundFontPreset> SynthEngine::listPresets(const std::string& path) {
    std::vector<SoundFontPreset> result;
    if (path.empty()) return result;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return result;
    int id = FLUID_FAILED;
    bool temporary = false;
    auto cached = pathToId_.find(path);
    if (cached != pathToId_.end()) id = cached->second;
    else { id = fluid_synth_sfload(synth_, path.c_str(), 0); if (id == FLUID_FAILED) return result; temporary = true; }
    fluid_sfont_t* soundfont = fluid_synth_get_sfont_by_id(synth_, id);
    if (soundfont) {
        fluid_sfont_iteration_start(soundfont);
        fluid_preset_t* preset = nullptr;
        while ((preset = fluid_sfont_iteration_next(soundfont)) != nullptr) {
            const char* name = fluid_preset_get_name(preset);
            result.push_back({fluid_preset_get_banknum(preset), fluid_preset_get_num(preset), name ? name : "Preset sem nome"});
        }
    }
    if (temporary) fluid_synth_sfunload(synth_, id, 1);
    return result;
}
bool SynthEngine::selectPreset(int channel, int bank, int program) {
    if (channel < 0 || channel >= 16 || bank < 0 || program < 0 || program > 127) return false;
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_ || soundfontIds_[channel] < 0) return false;
    if (fluid_synth_program_select(synth_, channel, soundfontIds_[channel], bank, program) != FLUID_OK) return false;
    bankMsb_[channel] = (bank >> 7) & 0x7f; bankLsb_[channel] = bank & 0x7f; programs_[channel] = program;
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
        if (synth_) fluid_synth_sfunload(synth_, id, 1);
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

void SynthEngine::midiEvent(int status, int data1, int data2) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!synth_) return;
    const int command = status & 0xF0;
    const int channel = status & 0x0F;
    const int a = std::clamp(data1, 0, 127);
    const int b = std::clamp(data2, 0, 127);
    if (!enabled_[channel]) {
        if (command == 0x80 || (command == 0x90 && b == 0))
            fluid_synth_noteoff(synth_, channel, a);
        else if (command == 0xB0 && a == 64 && b < 64)
            fluid_synth_cc(synth_, channel, a, b);
        return;
    }
    switch (command) {
        case 0x80:
            fluid_synth_noteoff(synth_, channel, a);
            break;
        case 0x90:
            if (b == 0) fluid_synth_noteoff(synth_, channel, a);
            else fluid_synth_noteon(synth_, channel, a, b);
            break;
        case 0xB0:
            fluid_synth_cc(synth_, channel, a, b);
            if (a == 0) bankMsb_[channel] = b;
            else if (a == 32) bankLsb_[channel] = b;
            else break;
            if (soundfontIds_[channel] >= 0) {
                const int bank = (bankMsb_[channel] << 7) | bankLsb_[channel];
                fluid_synth_program_select(synth_, channel, soundfontIds_[channel], bank, programs_[channel]);
            }
            break;
        case 0xC0:
            if (soundfontIds_[channel] >= 0 &&
                fluid_synth_program_select(synth_, channel, soundfontIds_[channel],
                    (bankMsb_[channel] << 7) | bankLsb_[channel], a) == FLUID_OK) {
                programs_[channel] = a;
            }
            break;
        case 0xE0:
            fluid_synth_pitch_bend(synth_, channel, a | (b << 7));
            break;
        default:
            break;
    }
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
    pathToId_.clear();
    idReferences_.clear();
    activeChannel_ = 0;
}