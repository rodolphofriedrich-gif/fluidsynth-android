#include <jni.h>
#include "synth_engine.h"
#include <string>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeInit(JNIEnv*, jobject) {
    return SynthEngine::instance().initialize() ? JNI_TRUE : JNI_FALSE;
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