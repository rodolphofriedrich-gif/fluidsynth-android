package com.rodolphofriedrich.fluidsynth;

import android.graphics.Color;

/**
 * Tokens da identidade visual Studio Premium.
 *
 * Esta classe contém somente valores de apresentação. Não deve receber
 * dependências de áudio, MIDI, armazenamento ou ciclo de vida Android.
 */
public final class VisualTheme {
    public static final int BACKGROUND = Color.rgb(18, 18, 18);       // #121212
    public static final int SURFACE = Color.rgb(30, 30, 30);         // #1E1E1E
    public static final int CONTROL = Color.rgb(42, 42, 42);         // #2A2A2A
    public static final int ACCENT = Color.rgb(152, 116, 232);        // #9874E8
    public static final int ACCENT_SOFT = Color.argb(76, 152, 116, 232);
    public static final int TEXT_PRIMARY = Color.WHITE;                    // #FFFFFF
    public static final int TEXT_SECONDARY = Color.rgb(189, 189, 189);// #BDBDBD
    public static final int BORDER = Color.rgb(56, 56, 56);           // #383838
    public static final int CONTROL_BORDER = Color.rgb(80, 80, 80);  // #505050
    public static final int KEY_BORDER = Color.rgb(102, 102, 102);       // #666666

    public static final int SUCCESS = Color.rgb(22, 185, 129);        // #16B981
    public static final int SOLO = Color.rgb(242, 169, 59);           // #F2A93B
    public static final int MUTE = Color.rgb(185, 75, 88);             // #B94B58
    public static final int EFFECTS = Color.rgb(152, 116, 232);       // #9874E8

    public static final int RADIUS_CONTROL = 12;
    public static final int RADIUS_CARD = 18;

    private VisualTheme() {}
}
