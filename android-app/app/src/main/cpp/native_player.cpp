#include <jni.h>
#include <algorithm>
#include <fluidsynth.h>
#include <mutex>

static fluid_settings_t* settings = nullptr;
static fluid_synth_t* synth = nullptr;
static int current_soundfont_id = FLUID_FAILED;
static std::mutex synth_mutex;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeInit(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> guard(synth_mutex);
    if (synth) return JNI_TRUE;
    settings = new_fluid_settings();
    if (!settings) return JNI_FALSE;
    fluid_settings_setnum(settings, "synth.sample-rate", 48000.0);
    fluid_settings_setint(settings, "synth.polyphony", 256);
    fluid_settings_setnum(settings, "synth.gain", 0.7);
    fluid_settings_setint(settings, "synth.reverb.active", 1);
    fluid_settings_setint(settings, "synth.chorus.active", 1);
    synth = new_fluid_synth(settings);
    if (!synth) {
        delete_fluid_settings(settings);
        settings = nullptr;
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeLoad(JNIEnv* env, jobject, jstring path) {
    if (!path) return FLUID_FAILED;
    const char* native_path = env->GetStringUTFChars(path, nullptr);
    if (!native_path) return FLUID_FAILED;
    std::lock_guard<std::mutex> guard(synth_mutex);
    int new_id = synth ? fluid_synth_sfload(synth, native_path, 1) : FLUID_FAILED;
    env->ReleaseStringUTFChars(path, native_path);
    if (new_id >= 0) {
        if (current_soundfont_id >= 0 && current_soundfont_id != new_id)
            fluid_synth_sfunload(synth, current_soundfont_id, 1);
        current_soundfont_id = new_id;
    }
    return new_id;
}

extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeNote(JNIEnv*, jobject, jint note, jint velocity, jboolean on) {
    std::lock_guard<std::mutex> guard(synth_mutex);
    if (!synth || note < 0 || note > 127) return;
    if (on) fluid_synth_noteon(synth, 0, note, std::clamp(static_cast<int>(velocity), 1, 127));
    else fluid_synth_noteoff(synth, 0, note);
}

extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeRender(JNIEnv* env, jobject, jfloatArray output, jint frames) {
    if (!output || frames <= 0 || env->GetArrayLength(output) < frames * 2) return;
    jfloat* out = env->GetFloatArrayElements(output, nullptr);
    if (!out) return;
    {
        std::lock_guard<std::mutex> guard(synth_mutex);
        if (synth) fluid_synth_write_float(synth, frames, out, 0, 2, out, 1, 2);
        else std::fill(out, out + frames * 2, 0.0f);
    }
    env->ReleaseFloatArrayElements(output, out, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeShutdown(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> guard(synth_mutex);
    if (synth) {
        delete_fluid_synth(synth);
        synth = nullptr;
    }
    current_soundfont_id = FLUID_FAILED;
    if (settings) {
        delete_fluid_settings(settings);
        settings = nullptr;
    }
}