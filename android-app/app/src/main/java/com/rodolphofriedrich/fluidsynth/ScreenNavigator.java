package com.rodolphofriedrich.fluidsynth;

import android.view.View;

/** Alterna as duas telas principais sem recriar os controles nem perder o estado. */
public final class ScreenNavigator {
    private ScreenNavigator() {}

    public static void show(View midiScreen, View settingsScreen, boolean showSettings) {
        if (midiScreen == null || settingsScreen == null) return;
        midiScreen.setVisibility(showSettings ? View.GONE : View.VISIBLE);
        settingsScreen.setVisibility(showSettings ? View.VISIBLE : View.GONE);
    }
}
