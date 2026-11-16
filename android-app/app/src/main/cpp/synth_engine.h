#pragma once
#include <array>
#include <string>
#include <unordered_map>
#include <mutex>
#include <fluidsynth.h>

class SynthEngine {
public:
    static SynthEngine& instance();
    bool initialize();
    bool setPolyphony(int voices);
    int loadSoundFontForChannel(const std::string& path, int channel);
    void setTrackEnabled(int channel, bool enabled);
    void setActiveChannel(int channel);
    void note(int channel, int note, int velocity, bool on);
    void midiEvent(int status, int data1, int data2);
    void render(float* output, int frames);
    void shutdown();
private:
    SynthEngine() = default;
    ~SynthEngine() = default;
    void releaseSoundFont(int id);
    std::mutex mutex_;
    fluid_settings_t* settings_ = nullptr;
    fluid_synth_t* synth_ = nullptr;
    std::array<int,16> soundfontIds_{};
    std::array<bool,16> enabled_{};
    std::array<int,16> bankMsb_{};
    std::array<int,16> bankLsb_{};
    std::array<int,16> programs_{};
    int activeChannel_ = 0;
    std::unordered_map<std::string,int> pathToId_;
    std::unordered_map<int,int> idReferences_;
};