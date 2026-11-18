#include <jni.h>
#include "synth_engine.h"
#include "jni_string_utils.h"
#include <string>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeInit(JNIEnv*, jobject) {
    return SynthEngine::instance().initialize() ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetPolyphony(JNIEnv*, jobject, jint voices) {
    return SynthEngine::instance().setPolyphony(voices) ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetReverb(JNIEnv*, jobject, jfloat room, jfloat damp, jfloat width, jfloat level) { SynthEngine::instance().setReverb(room, damp, width, level); }
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetChorus(JNIEnv*, jobject, jint voices, jfloat level, jfloat speed, jfloat depth) { SynthEngine::instance().setChorus(voices, level, speed, depth); }
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetReverbActive(JNIEnv*, jobject, jboolean active) { SynthEngine::instance().setReverbActive(active == JNI_TRUE); }
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetChorusActive(JNIEnv*, jobject, jboolean active) { SynthEngine::instance().setChorusActive(active == JNI_TRUE); }

extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeLoad(JNIEnv* env, jobject, jstring path) {
    if (!path) return FLUID_FAILED;
    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (!chars) return FLUID_FAILED;
    std::string value(chars); env->ReleaseStringUTFChars(path, chars);
    return SynthEngine::instance().loadSoundFontForChannel(value, 0);
}
extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeLoadTrack(JNIEnv* env, jobject, jint channel, jstring path) {
    if (!path) return FLUID_FAILED;
    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (!chars) return FLUID_FAILED;
    std::string value(chars); env->ReleaseStringUTFChars(path, chars);
    return SynthEngine::instance().loadSoundFontForChannel(value, channel);
}
extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeListPresets(JNIEnv* env, jobject, jstring path) {
    if (!path) return nullptr;

    const char* chars = env->GetStringUTFChars(path, nullptr);
    if (!chars) return nullptr;
    std::string value(chars);
    env->ReleaseStringUTFChars(path, chars);

    const auto presets = SynthEngine::instance().listPresets(value);
    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) return nullptr;

    jobjectArray array = env->NewObjectArray(
            static_cast<jsize>(presets.size()), stringClass, nullptr);
    if (!array) {
        env->DeleteLocalRef(stringClass);
        return nullptr;
    }

    for (jsize i = 0; i < static_cast<jsize>(presets.size()); ++i) {
        const auto& preset = presets[static_cast<size_t>(i)];
        const std::string row = std::to_string(preset.bank) + "|"
                + std::to_string(preset.program) + "|" + preset.name;
        jstring item = newJStringFromUtf8(env, row);
        if (!item) {
            env->DeleteLocalRef(stringClass);
            return nullptr;
        }
        env->SetObjectArrayElement(array, i, item);
        env->DeleteLocalRef(item);
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(stringClass);
            return nullptr;
        }
    }

    env->DeleteLocalRef(stringClass);
    return array;
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSelectTrackPreset(JNIEnv*, jobject, jint channel, jint bank, jint program) {
 return SynthEngine::instance().selectPreset(channel,bank,program)?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetTrackEnabled(JNIEnv*, jobject, jint channel, jboolean enabled) {
    SynthEngine::instance().setTrackEnabled(channel, enabled == JNI_TRUE);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeSetActiveTrack(JNIEnv*, jobject, jint channel) {
    SynthEngine::instance().setActiveChannel(channel);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeNote(JNIEnv*, jobject, jint note, jint velocity, jboolean on) {
    SynthEngine::instance().note(0, note, velocity, on == JNI_TRUE);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeNoteOnTrack(JNIEnv*, jobject, jint channel, jint note, jint velocity, jboolean on) {
    SynthEngine::instance().note(channel, note, velocity, on == JNI_TRUE);
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeMidiEvent(JNIEnv*, jobject, jint status, jint data1, jint data2) {
    return SynthEngine::instance().midiEvent(status, data1, data2) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeGetTrackBank(JNIEnv*, jobject, jint channel) {
    return SynthEngine::instance().getTrackBank(channel);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeRender(JNIEnv* env, jobject, jfloatArray output, jint frames) {
    if (!output || frames <= 0 || env->GetArrayLength(output) < frames * 2) return;
    jfloat* data = env->GetFloatArrayElements(output, nullptr);
    if (!data) return;
    SynthEngine::instance().render(data, frames);
    env->ReleaseFloatArrayElements(output, data, 0);
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_NativeBridge_nativeShutdown(JNIEnv*, jobject) {
    SynthEngine::instance().shutdown();
}