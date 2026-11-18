package com.rodolphofriedrich.fluidsynth;
import android.content.Context;
import android.content.SharedPreferences;
/** Persistência isolada dos efeitos globais. */
public final class EffectsSettingsStore {
    private static final String PREFS="audio_effects";
    private static final String[] NAMES={"reverb","chorus"};
    private static final float[][] DEFAULTS={{.20f,0f,100f,.90f},{3f,2f,.3f,8f}};
    private EffectsSettingsStore(){}
    public static float[] defaults(int effect){return DEFAULTS[effect].clone();}
    private static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static float[] loadValues(Context c,int effect){
        float[] v=defaults(effect);SharedPreferences p=prefs(c);
        for(int i=0;i<4;i++)v[i]=p.getFloat(NAMES[effect]+"_"+i,v[i]);return v;
    }
    public static void saveValues(Context c,int effect,float[] v){
        if(v==null||v.length!=4)return;SharedPreferences.Editor e=prefs(c).edit();
        for(int i=0;i<4;i++)e.putFloat(NAMES[effect]+"_"+i,v[i]);e.apply();
    }
    public static boolean loadActive(Context c,int effect){return prefs(c).getBoolean(NAMES[effect]+"_active",true);}
    public static void saveActive(Context c,int effect,boolean active){prefs(c).edit().putBoolean(NAMES[effect]+"_active",active).apply();}
}
