#include <jni.h>
#include <algorithm>
#include <fluidsynth.h>
#include <mutex>
#include <string>
static fluid_settings_t* settings=nullptr;
static fluid_synth_t* synth=nullptr;
static std::mutex lock;
extern "C" JNIEXPORT jboolean JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeInit(JNIEnv*,jobject) {
 std::lock_guard<std::mutex> g(lock);
 if(synth) return JNI_TRUE;
 settings=new_fluid_settings();
 if(!settings) return JNI_FALSE;
 fluid_settings_setnum(settings,"synth.sample-rate",48000.0);
 fluid_settings_setint(settings,"synth.polyphony",256);
 fluid_settings_setnum(settings,"synth.gain",0.7);
 fluid_settings_setint(settings,"synth.reverb.active",1);
 fluid_settings_setint(settings,"synth.chorus.active",1);
 synth=new_fluid_synth(settings);
 return synth?JNI_TRUE:JNI_FALSE;
}
extern "C" JNIEXPORT jint JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeLoad(JNIEnv* env,jobject,jstring path) {
 const char* p=env->GetStringUTFChars(path,nullptr);
 std::lock_guard<std::mutex> g(lock);
 int id=synth?fluid_synth_sfload(synth,p,1):-1;
 env->ReleaseStringUTFChars(path,p);
 return id;
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeNote(JNIEnv*,jobject,jint note,jint velocity,jboolean on) {
 std::lock_guard<std::mutex> g(lock);
 if(synth) { if(on) fluid_synth_noteon(synth,0,note,velocity); else fluid_synth_noteoff(synth,0,note); }
}
extern "C" JNIEXPORT void JNICALL
Java_com_rodolphofriedrich_fluidsynth_MainActivity_nativeRender(JNIEnv* env,jobject,jfloatArray output,jint frames) {
 jfloat* out=env->GetFloatArrayElements(output,nullptr);
 std::lock_guard<std::mutex> g(lock);
 if(synth) fluid_synth_write_float(synth,frames,out,0,2,out,1,2);
 else std::fill(out,out+frames*2,0.0f);
 env->ReleaseFloatArrayElements(output,out,0);
}
