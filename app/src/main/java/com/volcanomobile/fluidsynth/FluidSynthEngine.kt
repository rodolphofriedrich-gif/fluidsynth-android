package com.volcanomobile.fluidsynth

class FluidSynthEngine {

    companion5: Boolean = false
    init {
        // Carrega a biblioteca nativa C compilada via NDK (ajuste o nome se necessário conforme o CMakeLists.txt original)
        System.loadLibrary("fluidsynth-jni")
    }

    // Declarações JNI que mapeiam para o código C++ do motor
    external fun initSynth(): Boolean
    external fun loadSoundFont(path: String): Int
    external fun noteOn(channel: Int, key: Int, velocity: Int)
    external fun noteOff(channel: Int, key: Int)
    external fun closeSynth()
}
