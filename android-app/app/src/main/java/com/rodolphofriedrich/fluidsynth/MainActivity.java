package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    static { System.loadLibrary("player"); }
    private native boolean nativeInit();
    private native int nativeLoad(String path);
    private native void nativeNote(int note,int velocity,boolean on);
    private native void nativeRender(float[] out,int frames);
    private AudioTrack track; private volatile boolean playing=false;
    private TextView status;
    private static final int PICK_SF2=71;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(20,28,20,20);
        status=new TextView(this); status.setText("FluidSynth — selecione um arquivo SF2"); status.setTextSize(18); root.addView(status);
        Button choose=new Button(this); choose.setText("ESCOLHER SOUNDFont (.sf2)"); choose.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_SF2); }); root.addView(choose);
        TextView hint=new TextView(this); hint.setText("Carregue um SF2 para liberar o teclado."); root.addView(hint);
        int[] notes={60,62,64,65,67,69,71,72,74,76,77,79};
        for(int row=0;row<2;row++) {
            LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER); root.addView(line);
            for(int col=0;col<6;col++) { int n=notes[row*6+col]; Button key=new Button(this); key.setText(noteName(n)); key.setTextSize(11); line.addView(key,new LinearLayout.LayoutParams(0,150,1));
                key.setOnTouchListener((v,e)->{ if(track==null)return true; switch(e.getAction()){case 0: nativeNote(n,100,true); return true; case 1: case 3: nativeNote(n,0,false); return true;} return true; }); }
        }
        setContentView(root);
        if(nativeInit()) startAudio(); else status.setText("Falha ao iniciar o motor FluidSynth.");
    }
    private String noteName(int n){return new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"}[n%12]+(n/12-1);}
    private void startAudio(){
        int min=AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_FLOAT);
        track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(48000).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()).setBufferSizeInBytes(Math.max(min,8192)).setTransferMode(AudioTrack.MODE_STREAM).build();
        playing=true; track.play();
        new Thread(()->{ float[] data=new float[512*2]; while(playing){ nativeRender(data,512); track.write(data,0,data.length,AudioTrack.WRITE_BLOCKING); } },"FluidSynth-Audio").start();
    }
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d); if(r==PICK_SF2&&c==RESULT_OK&&d!=null){ try(InputStream in=getContentResolver().openInputStream(d.getData())){ File f=new File(getFilesDir(),"selected.sf2"); try(FileOutputStream out=new FileOutputStream(f)){byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0)out.write(buf,0,n);} int id=nativeLoad(f.getAbsolutePath()); status.setText(id>=0?"SoundFont carregado: "+f.getName():"Falha ao carregar SF2."); }catch(Exception e){status.setText("Erro: "+e.getMessage());} } }
    @Override protected void onDestroy(){playing=false;if(track!=null){track.pause();track.flush();track.release();} super.onDestroy();}
}
