#include <jni.h>
#include "synth_engine.h"
#include <string>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeInit(JNIEnv*, jobject) {
    return SynthEngine::instance().initialize() ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeSetPolyphony(JNIEnv*, jobject, jint voices) {
    return SynthEngine::instance().setPolyphony(voices) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeLoad(JNIEnv* env, jobject, jstring path) {
    if (!path) return FLUID_FAILED;
    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (!chars) return FLUID_FAILED;
    std::string value(chars); env->ReleaseStringUTFChars(path, chars);
    return SynthEngine::instance().loadSoundFontForChannel(value, 0);
}
extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeLoadTrack(JNIEnv* env, jobject, jint channel, jstring path) {
    if (!path) return FLUID_FAILED;
    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (!chars) return FLUID_FAILED;
    std::string value(chars); env->ReleaseStringUTFChars(path, chars);
    return SynthEngine::instance().loadSoundFontForChannel(value, channel);
}
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeListPresets(JNIEnv* env, jobject, jstring path) {
 if (!path) return nullptr; const char* chars=env->GetStringUTFChars(path,nullptr); if(!chars) return nullptr;
 std::string value(chars); env->ReleaseStringUTFChars(path,chars); auto presets=SynthEngine::instance().listPresets(value);
 jclass cls=env->FindClass("java/lang/String"); jobjectArray array=env->NewObjectArray((jsize)presets.size(),cls,nullptr);
 for(jsize i=0;i<(jsize)presets.size();++i){ const auto& p=presets[(size_t)i];
 std::string row=std::to_string(p.bank)+"|"+std::to_string(p.program)+"|"+p.name;
 jstring item=env->NewStringUTF(row.c_str()); env->SetObjectArrayElement(array,i,item); env->DeleteLocalRef(item); }
 return array;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeSelectTrackPreset(JNIEnv*, jobject, jint channel, jint bank, jint program) {
 return SynthEngine::instance().selectPreset(channel,bank,program)?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeSetTrackEnabled(JNIEnv*, jobject, jint channel, jboolean enabled) {
    SynthEngine::instance().setTrackEnabled(channel, enabled == JNI_TRUE);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeSetActiveTrack(JNIEnv*, jobject, jint channel) {
    SynthEngine::instance().setActiveChannel(channel);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeNote(JNIEnv*, jobject, jint note, jint velocity, jboolean on) {
    SynthEngine::instance().note(0, note, velocity, on == JNI_TRUE);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeNoteOnTrack(JNIEnv*, jobject, jint channel, jint note, jint velocity, jboolean on) {
    SynthEngine::instance().note(channel, note, velocity, on == JNI_TRUE);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeMidiEvent(JNIEnv*, jobject, jint status, jint data1, jint data2) {
    SynthEngine::instance().midiEvent(status, data1, data2);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeRender(JNIEnv* env, jobject, jfloatArray output, jint frames) {
    if (!output || frames <= 0 || env->GetArrayLength(output) < frames * 2) return;
    jfloat* data = env->GetFloatArrayElements(output, nullptr);
    if (!data) return;
    SynthEngine::instance().render(data, frames);
    env->ReleaseFloatArrayElements(output, data, 0);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeShutdown(JNIEnv*, jobject) {
    SynthEngine::instance().shutdown();
}