package com.rodolphofriedrich.fluidsynth;

import android.view.View;

/** Crossfade curto entre telas sem recriar as Views nem reiniciar o áudio. */
public final class ScreenTransitionController {
    private ScreenTransitionController() {}

    public static void show(View current, View next) {
        if (current == null || next == null || current == next) return;

        current.animate().cancel();
        next.animate().cancel();
        next.setAlpha(0f);
        next.setVisibility(View.VISIBLE);

        current.animate()
                .alpha(0f)
                .setDuration(120L)
                .withEndAction(() -> {
                    current.setVisibility(View.GONE);
                    current.setAlpha(1f);
                })
                .start();

        next.animate()
                .alpha(1f)
                .setDuration(180L)
                .start();
    }
}
