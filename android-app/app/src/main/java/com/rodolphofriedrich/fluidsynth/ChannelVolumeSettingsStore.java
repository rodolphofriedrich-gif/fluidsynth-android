package com.rodolphofriedrich.fluidsynth;
import android.content.Context;
import android.content.SharedPreferences;
/** Armazena separadamente o volume de cada canal MIDI. */
public final class ChannelVolumeSettingsStore {
 private static final int DEFAULT=100;
 private ChannelVolumeSettingsStore(){}
 public static int load(Context c,int channel){
  check(channel);return Math.max(0,Math.min(100,prefs(c).getInt("channel_"+channel,DEFAULT)));
 }
 public static void save(Context c,int channel,int volume){
  check(channel);prefs(c).edit().putInt("channel_"+channel,Math.max(0,Math.min(100,volume))).apply();
 }
 private static SharedPreferences prefs(Context c){
  return c.getApplicationContext().getSharedPreferences("channel_volume",Context.MODE_PRIVATE);
 }
 private static void check(int channel){
  if(channel<0||channel>=16)throw new IllegalArgumentException("Canal MIDI inválido");
 }
}