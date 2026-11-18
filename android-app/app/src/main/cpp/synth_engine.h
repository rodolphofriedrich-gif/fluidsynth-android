#pragma once
#include <array>
#include <string>
#include <unordered_map>
#include <mutex>
#include <vector>
#include <fluidsynth.h>
struct SoundFontPreset { int bank; int program; std::string name; };
class SynthEngine {
public:
 static SynthEngine& instance();
 bool initialize(); bool setPolyphony(int voices);
 void setReverb(double roomSize, double damping, double width, double level);
 void setChorus(int voices, double level, double speed, double depthMs);
 void setReverbActive(bool active); void setChorusActive(bool active);
 int loadSoundFontForChannel(const std::string& path,int channel);
 std::vector<SoundFontPreset> listPresets(const std::string& path);
 bool selectPreset(int channel,int bank,int program);
 void setTrackEnabled(int channel,bool enabled); void setActiveChannel(int channel); int getTrackBank(int channel);
 void note(int channel,int note,int velocity,bool on); bool midiEvent(int status,int data1,int data2);
 void render(float* output,int frames); void shutdown();
private:
 SynthEngine()=default; ~SynthEngine()=default; void releaseSoundFont(int id);
 std::mutex mutex_; fluid_settings_t* settings_=nullptr; fluid_synth_t* synth_=nullptr;
 std::array<int,16> soundfontIds_{}; std::array<bool,16> enabled_{};
 std::array<int,16> bankMsb_{}; std::array<int,16> bankLsb_{}; std::array<int,16> programs_{};
 int activeChannel_=0; std::unordered_map<std::string,int> pathToId_; std::unordered_map<int,int> idReferences_;
};