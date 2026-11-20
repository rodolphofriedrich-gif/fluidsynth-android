package com.rodolphofriedrich.fluidsynth;

import android.graphics.Color;

/**
 * Tokens da identidade visual Studio Premium.
 *
 * Esta classe contém somente valores de apresentação. Não deve receber
 * dependências de áudio, MIDI, armazenamento ou ciclo de vida Android.
 */
public final class VisualTheme {
    public static final int BACKGROUND = Color.rgb(8, 13, 24);       // #080D18
    public static final int SURFACE = Color.rgb(19, 29, 48);         // #131D30
    public static final int CONTROL = Color.rgb(32, 46, 70);         // #202E46
    public static final int ACCENT = Color.rgb(57, 139, 255);         // #398BFF
    public static final int ACCENT_SOFT = Color.argb(76, 57, 139, 255);
    public static final int TEXT_PRIMARY = Color.rgb(241, 245, 252);  // #F1F5FC
    public static final int TEXT_SECONDARY = Color.rgb(154, 170, 194);// #9AAAC2
    public static final int BORDER = Color.rgb(41, 58, 85);           // #293A55
    public static final int CONTROL_BORDER = Color.rgb(53, 73, 103);  // #354967
    public static final int KEY_BORDER = Color.rgb(64, 87, 117);       // #405775

    public static final int SUCCESS = Color.rgb(22, 185, 129);        // #16B981
    public static final int SOLO = Color.rgb(242, 169, 59);           // #F2A93B
    public static final int MUTE = Color.rgb(185, 75, 88);             // #B94B58
    public static final int EFFECTS = Color.rgb(152, 116, 232);       // #9874E8

    public static final int RADIUS_CONTROL = 12;
    public static final int RADIUS_CARD = 18;

    private VisualTheme() {}
}
