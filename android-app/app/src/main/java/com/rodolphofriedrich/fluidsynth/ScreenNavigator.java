package com.rodolphofriedrich.fluidsynth;
import android.view.View;
/** Alterna as telas principais sem recriar controles nem perder o estado. */
public final class ScreenNavigator {
    private ScreenNavigator() {}
    public static void show(View midi, View settings, View effects, boolean showSettings, boolean showEffects) {
        if (midi == null || settings == null || effects == null) return;
        midi.setVisibility(!showSettings && !showEffects ? View.VISIBLE : View.GONE);
        settings.setVisibility(showSettings ? View.VISIBLE : View.GONE);
        effects.setVisibility(showEffects ? View.VISIBLE : View.GONE);
    }
}
