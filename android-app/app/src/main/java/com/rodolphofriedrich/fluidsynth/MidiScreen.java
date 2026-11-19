package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ProgressBar;

/** Constrói a interface MIDI sem concentrar nela a lógica de áudio ou roteamento. */
public final class MidiScreen {
    public static final int[] NOTES = {60,62,64,65,67,69,71,72,74,76,77,79};
    public final ScrollView scrollView;
    public final LinearLayout topControls;
    public final TextView status, audioDiagnostics, midiActivity;
    public final Button[] trackButtons=new Button[16], trackMuteButtons=new Button[16], trackSoloButtons=new Button[16];
    public final SeekBar[] trackVolumeSeekBars=new SeekBar[16];
    public final TextView[] trackVolumeValues=new TextView[16];
    public final Button selectInstrumentButton, trackEnableButton, soloButton;
    public final SeekBar masterVolumeSeekBar, velocitySeekBar;
    public final TextView masterVolumeValue, velocityValue;
    public final LinearLayout importProgressPanel;
    public final TextView importProgressText;
    public final ProgressBar importProgressBar;
    public final Button effectsToggleButton;
    public final Button[] keyboardKeys=new Button[NOTES.length];
    private final GridLayout trackGrid;
    private final Context context;

    public MidiScreen(Context c) {
        context=c;
        LinearLayout root=new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(c,12),AppUi.dp(c,8),AppUi.dp(c,12),AppUi.dp(c,16));
        root.setBackgroundColor(AppUi.BG);

        topControls=new LinearLayout(c);
        topControls.setGravity(Gravity.CENTER_VERTICAL);
        topControls.setPadding(AppUi.dp(c,10),AppUi.dp(c,6),AppUi.dp(c,10),AppUi.dp(c,6));
        topControls.setBackground(AppUi.rounded(c,AppUi.PANEL,12,AppUi.BORDER));
        LinearLayout master=sliderGroup(c,c.getString(R.string.master_volume_label));
        masterVolumeValue=value(c,"100%");
        master.addView(masterVolumeValue);
        masterVolumeSeekBar=slider(c,100,100,c.getString(R.string.master_volume_accessibility));
        master.addView(masterVolumeSeekBar,new LinearLayout.LayoutParams(-1,AppUi.dp(c,34)));
        topControls.addView(master,new LinearLayout.LayoutParams(0,-2,1f));
        LinearLayout velocity=sliderGroup(c,c.getString(R.string.velocity_label));
        velocityValue=value(c,"100");
        velocity.addView(velocityValue);
        velocitySeekBar=slider(c,126,99,c.getString(R.string.velocity_accessibility));
        velocity.addView(velocitySeekBar,new LinearLayout.LayoutParams(-1,AppUi.dp(c,34)));
        LinearLayout vp=new LinearLayout.LayoutParams(0,-2,1f); vp.leftMargin=AppUi.dp(c,8);
        topControls.addView(velocity,vp);
        effectsToggleButton=new Button(c);
        effectsToggleButton.setText("≋"); effectsToggleButton.setTextSize(24);
        effectsToggleButton.setAllCaps(false); effectsToggleButton.setTextColor(AppUi.TEXT);
        effectsToggleButton.setContentDescription(c.getString(R.string.effects_panel_open));
        effectsToggleButton.setBackground(AppUi.ripple(c,AppUi.CONTROL,10,AppUi.BORDER));
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(AppUi.dp(c,48),AppUi.dp(c,48));
        ep.leftMargin=AppUi.dp(c,8); topControls.addView(effectsToggleButton,ep);
        // A barra é adicionada pela Activity fora do ScrollView para permanecer fixa.

        status=new TextView(c); audioDiagnostics=new TextView(c); midiActivity=new TextView(c);
        status.setVisibility(View.GONE); audioDiagnostics.setVisibility(View.GONE); midiActivity.setVisibility(View.GONE);

        LinearLayout panel=AppUi.panel(c,root);
        panel.addView(AppUi.sectionLabel(c,c.getString(R.string.tracks_title)));
        TextView hint=new TextView(c); hint.setText(c.getString(R.string.track_hint));
        hint.setTextColor(AppUi.MUTED); hint.setTextSize(12);
        hint.setPadding(0,AppUi.dp(c,3),0,AppUi.dp(c,8)); panel.addView(hint);
        trackGrid=new GridLayout(c); trackGrid.setUseDefaultMargins(false); panel.addView(trackGrid);
        for(int i=0;i<16;i++){
            LinearLayout strip=new LinearLayout(c); strip.setOrientation(LinearLayout.VERTICAL);
            strip.setPadding(AppUi.dp(c,5),AppUi.dp(c,4),AppUi.dp(c,5),AppUi.dp(c,5));
            strip.setBackground(AppUi.rounded(c,AppUi.PANEL,10,AppUi.BORDER));
            Button ch=new Button(c); ch.setText(c.getString(R.string.track_channel_empty,i+1));
            ch.setTextSize(12); ch.setTextColor(AppUi.TEXT); ch.setAllCaps(false);
            ch.setPadding(2,0,2,0); ch.setMinHeight(0); ch.setMinimumHeight(0);
            ch.setBackground(AppUi.ripple(c,AppUi.CONTROL,8,AppUi.CONTROL_BORDER));
            ch.setContentDescription(c.getString(R.string.track_accessibility,i+1));
            strip.addView(ch,new LinearLayout.LayoutParams(-1,AppUi.dp(c,44))); trackButtons[i]=ch;
            TextView vl=value(c,"100%"); vl.setGravity(Gravity.CENTER); strip.addView(vl); trackVolumeValues[i]=vl;
            SeekBar vol=slider(c,100,100,c.getString(R.string.channel_volume_accessibility,i+1));
            strip.addView(vol,new LinearLayout.LayoutParams(-1,AppUi.dp(c,30))); trackVolumeSeekBars[i]=vol;
            LinearLayout actions=new LinearLayout(c);
            Button mute=smallButton(c,"CH ✓"); mute.setContentDescription(c.getString(R.string.channel_toggle_accessibility,i+1));
            Button solo=smallButton(c,"SOLO"); solo.setContentDescription(c.getString(R.string.solo_accessibility,i+1));
            LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,AppUi.dp(c,34),1f);
            ap.setMargins(AppUi.dp(c,2),AppUi.dp(c,2),AppUi.dp(c,2),0);
            actions.addView(mute,ap); actions.addView(solo,new LinearLayout.LayoutParams(ap));
            strip.addView(actions); trackMuteButtons[i]=mute; trackSoloButtons[i]=solo;
            GridLayout.LayoutParams cell=new GridLayout.LayoutParams();
            cell.width=0; cell.height=GridLayout.LayoutParams.WRAP_CONTENT;
            cell.setMargins(AppUi.dp(c,3),AppUi.dp(c,3),AppUi.dp(c,3),AppUi.dp(c,3));
            trackGrid.addView(strip,cell);
        }
        updateTrackGridOrientation(c.getResources().getConfiguration());

        importProgressPanel=AppUi.panel(c,root); importProgressPanel.setVisibility(View.GONE);
        importProgressPanel.addView(AppUi.sectionLabel(c,c.getString(R.string.import_progress_title)));
        importProgressText=new TextView(c); importProgressText.setTextColor(AppUi.MUTED);
        importProgressText.setTextSize(12); importProgressPanel.addView(importProgressText);
        importProgressBar=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setMax(1000);
        importProgressPanel.addView(importProgressBar,new LinearLayout.LayoutParams(-1,AppUi.dp(c,8)));

        selectInstrumentButton=new Button(c); trackEnableButton=new Button(c); soloButton=new Button(c);
        selectInstrumentButton.setVisibility(View.GONE); trackEnableButton.setVisibility(View.GONE); soloButton.setVisibility(View.GONE);

        LinearLayout keyboard=AppUi.panel(c,root);
        keyboard.addView(AppUi.sectionLabel(c,c.getString(R.string.virtual_keyboard)));
        TextView kh=new TextView(c); kh.setText(c.getString(R.string.keyboard_hint));
        kh.setTextColor(AppUi.MUTED); kh.setTextSize(12); kh.setPadding(0,AppUi.dp(c,3),0,AppUi.dp(c,8)); keyboard.addView(kh);
        for(int row=0;row<2;row++){
            LinearLayout line=new LinearLayout(c); line.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.bottomMargin=AppUi.dp(c,6); keyboard.addView(line,rp);
            for(int col=0;col<6;col++){
                int index=row*6+col,note=NOTES[index]; Button key=smallButton(c,noteName(note));
                key.setTextSize(12); key.setContentDescription(c.getString(R.string.key_accessibility,noteName(note),note));
                LinearLayout.LayoutParams kp=new LinearLayout.LayoutParams(0,AppUi.dp(c,66),1f);
                kp.setMargins(AppUi.dp(c,2),AppUi.dp(c,2),AppUi.dp(c,2),AppUi.dp(c,2));
                line.addView(key,kp); keyboardKeys[index]=key;
            }
        }
        scrollView=new ScrollView(c); scrollView.setFillViewport(true); scrollView.setClipToPadding(false); scrollView.addView(root);
    }
    private static LinearLayout sliderGroup(Context c,String label){
        LinearLayout g=new LinearLayout(c); g.setOrientation(LinearLayout.VERTICAL);
        TextView t=AppUi.sectionLabel(c,label); t.setTextSize(10); g.addView(t); return g;
    }
    private static TextView value(Context c,String text){
        TextView v=new TextView(c); v.setText(text); v.setTextColor(AppUi.TEXT); v.setTextSize(12);
        v.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD); return v;
    }
    private static SeekBar slider(Context c,int max,int progress,String description){
        SeekBar b=new SeekBar(c); b.setMax(max); b.setProgress(progress); b.setContentDescription(description); AppUi.tint(b); return b;
    }
    private static Button smallButton(Context c,String text){
        Button b=new Button(c); b.setText(text); b.setTextSize(10); b.setAllCaps(false); b.setTextColor(AppUi.TEXT);
        b.setPadding(0,0,0,0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setBackground(AppUi.ripple(c,AppUi.CONTROL,7,AppUi.CONTROL_BORDER)); return b;
    }
    public void updateTrackGridOrientation(Configuration cfg){
        int columns=cfg.orientation==Configuration.ORIENTATION_PORTRAIT?2:4;
        trackGrid.setColumnCount(columns); trackGrid.setRowCount(16/columns);
        for(int i=0;i<trackGrid.getChildCount();i++){
            GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(i/columns,1f),GridLayout.spec(i%columns,1f));
            p.width=0; p.height=GridLayout.LayoutParams.WRAP_CONTENT;
            p.setMargins(AppUi.dp(context,3),AppUi.dp(context,3),AppUi.dp(context,3),AppUi.dp(context,3));
            trackGrid.getChildAt(i).setLayoutParams(p);
        }
        trackGrid.requestLayout();
    }
    public void showMidiActivity(int channel) { /* Não ocupa espaço no cabeçalho. */ }
    private static String noteName(int note){
        return new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"}[note%12]+(note/12-1);
    }
}