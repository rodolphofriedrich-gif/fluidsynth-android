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
    public final Button panicButton;
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
        topControls.setPadding(0,AppUi.dp(c,8),0,AppUi.dp(c,8));
        topControls.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        topControls.setElevation(0);
        LinearLayout master=sliderGroup(c,c.getString(R.string.master_volume_label));
        masterVolumeValue=value(c,"100%");
        master.addView(masterVolumeValue);
        masterVolumeSeekBar=slider(c,100,100,c.getString(R.string.master_volume_accessibility));
        master.addView(masterVolumeSeekBar,new LinearLayout.LayoutParams(-1,AppUi.dp(c,48)));
        topControls.addView(master,new LinearLayout.LayoutParams(0,-2,1f));
        LinearLayout velocity=sliderGroup(c,c.getString(R.string.velocity_label));
        velocityValue=value(c,"100");
        velocity.addView(velocityValue);
        velocitySeekBar=slider(c,126,99,c.getString(R.string.velocity_accessibility));
        velocity.addView(velocitySeekBar,new LinearLayout.LayoutParams(-1,AppUi.dp(c,34)));
        LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(0,-2,1f); vp.leftMargin=AppUi.dp(c,8);
        topControls.addView(velocity,vp);
        effectsToggleButton=new Button(c);
        effectsToggleButton.setText("≋"); effectsToggleButton.setTextSize(24);
        effectsToggleButton.setAllCaps(false); effectsToggleButton.setTextColor(AppUi.TEXT);
        effectsToggleButton.setContentDescription(c.getString(R.string.effects_panel_open));
        effectsToggleButton.setBackground(AppUi.ripple(c,AppUi.CONTROL,10,AppUi.BORDER));
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(AppUi.dp(c,48),AppUi.dp(c,48));
        ep.leftMargin=AppUi.dp(c,8); topControls.addView(effectsToggleButton,ep);

        status=new TextView(c); audioDiagnostics=new TextView(c); midiActivity=new TextView(c);
        status.setVisibility(View.GONE); audioDiagnostics.setVisibility(View.GONE); midiActivity.setVisibility(View.GONE);

        LinearLayout panel=AppUi.flatSection(c,root);
        panel.addView(AppUi.sectionLabel(c,c.getString(R.string.tracks_title)));
        TextView hint=new TextView(c); hint.setText(c.getString(R.string.track_hint));
        hint.setTextColor(AppUi.MUTED); hint.setTextSize(12);
        hint.setPadding(0,AppUi.dp(c,3),0,AppUi.dp(c,8)); panel.addView(hint);

        LinearLayout midiTools = new LinearLayout(c);
        midiTools.setOrientation(LinearLayout.HORIZONTAL);
        midiTools.setGravity(Gravity.CENTER_VERTICAL);
        panicButton = smallButton(c, "PANIC");
        panicButton.setTextSize(11);
        panicButton.setTextColor(0xFFFFD8DC);
        panicButton.setContentDescription("Interromper todas as notas e liberar sustain");
        panicButton.setBackground(AppUi.ripple(c, android.graphics.Color.TRANSPARENT, 10, 0));
        panicButton.setElevation(0);
        LinearLayout.LayoutParams panicParams = new LinearLayout.LayoutParams(-1, AppUi.dp(c, 44));
        panicParams.setMargins(0, 0, 0, AppUi.dp(c, 8));
        midiTools.addView(panicButton, panicParams);
        panel.addView(midiTools);

        trackGrid=new GridLayout(c); trackGrid.setUseDefaultMargins(false); panel.addView(trackGrid);
        for(int i=0;i<16;i++){
            LinearLayout strip=new LinearLayout(c);
            strip.setOrientation(LinearLayout.HORIZONTAL);
            strip.setGravity(Gravity.CENTER_VERTICAL);
            strip.setPadding(0,AppUi.dp(c,5),0,AppUi.dp(c,5));
            strip.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            Button ch=new Button(c);
            ch.setText(c.getString(R.string.track_channel_empty,i+1));
            ch.setTextSize(10);
            ch.setTextColor(i == 0 ? VisualTheme.ACCENT : AppUi.TEXT);
            ch.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
            ch.setAllCaps(false);
            ch.setGravity(Gravity.CENTER);
            ch.setPadding(AppUi.dp(c,2),AppUi.dp(c,3),AppUi.dp(c,2),AppUi.dp(c,3));
            ch.setMinHeight(0); ch.setMinimumHeight(0);
            ch.setBackground(AppUi.ripple(c,android.graphics.Color.TRANSPARENT,8,0));
            ch.setContentDescription(c.getString(R.string.track_accessibility,i+1));
            strip.addView(ch,new LinearLayout.LayoutParams(AppUi.dp(c,88),AppUi.dp(c,48)));
            trackButtons[i]=ch;

            LinearLayout volumeGroup=new LinearLayout(c);
            volumeGroup.setOrientation(LinearLayout.VERTICAL);
            volumeGroup.setPadding(AppUi.dp(c,8),0,AppUi.dp(c,8),0);
            TextView vl=value(c,"100%");
            vl.setGravity(Gravity.CENTER_VERTICAL);
            volumeGroup.addView(vl);
            trackVolumeValues[i]=vl;
            SeekBar vol=slider(c,100,100,c.getString(R.string.channel_volume_accessibility,i+1));
            volumeGroup.addView(vol,new LinearLayout.LayoutParams(-1,AppUi.dp(c,36)));
            trackVolumeSeekBars[i]=vol;
            strip.addView(volumeGroup,new LinearLayout.LayoutParams(0,-2,1f));

            LinearLayout actions=new LinearLayout(c);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button mute=smallButton(c,"M");
            mute.setContentDescription(c.getString(R.string.channel_toggle_accessibility,i+1));
            Button solo=smallButton(c,"S");
            solo.setContentDescription(c.getString(R.string.solo_accessibility,i+1));
            mute.setBackground(AppUi.ripple(c,android.graphics.Color.TRANSPARENT,8,0));
            solo.setBackground(AppUi.ripple(c,android.graphics.Color.TRANSPARENT,8,0));
            LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(AppUi.dp(c,38),AppUi.dp(c,44));
            actions.addView(mute,actionParams);
            actions.addView(solo,new LinearLayout.LayoutParams(AppUi.dp(c,38),AppUi.dp(c,44)));
            strip.addView(actions);
            trackMuteButtons[i]=mute; trackSoloButtons[i]=solo;

            LinearLayout rowContainer=new LinearLayout(c);
            rowContainer.setOrientation(LinearLayout.VERTICAL);
            rowContainer.addView(strip,new LinearLayout.LayoutParams(-1,-2));
            if(i<15){
                View divider=new View(c);
                divider.setBackgroundColor(android.graphics.Color.argb(26,154,170,194));
                rowContainer.addView(divider,new LinearLayout.LayoutParams(-1,AppUi.dp(c,1)));
            }
            GridLayout.LayoutParams cell=new GridLayout.LayoutParams();
            cell.width=-1; cell.height=GridLayout.LayoutParams.WRAP_CONTENT;
            cell.setMargins(0,0,0,0);
            trackGrid.addView(rowContainer,cell);
        }
        updateTrackGridOrientation(c.getResources().getConfiguration());

        importProgressPanel=AppUi.flatSection(c,root); importProgressPanel.setVisibility(View.GONE);
        importProgressPanel.addView(AppUi.sectionLabel(c,c.getString(R.string.import_progress_title)));
        importProgressText=new TextView(c); importProgressText.setTextColor(AppUi.MUTED);
        importProgressText.setTextSize(12); importProgressPanel.addView(importProgressText);
        importProgressBar=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);
        importProgressBar.setMax(1000);
        importProgressPanel.addView(importProgressBar,new LinearLayout.LayoutParams(-1,AppUi.dp(c,8)));

        selectInstrumentButton=new Button(c); trackEnableButton=new Button(c); soloButton=new Button(c);
        selectInstrumentButton.setVisibility(View.GONE); trackEnableButton.setVisibility(View.GONE); soloButton.setVisibility(View.GONE);

        AppUi.addDivider(c, root);
        LinearLayout keyboard=AppUi.flatSection(c,root);
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
        TextView v=new TextView(c); v.setText(text); AppUi.styleValue(v); return v;
    }
    private static SeekBar slider(Context c,int max,int progress,String description){
        SeekBar b=new SeekBar(c); b.setMax(max); b.setProgress(progress); b.setContentDescription(description);
        b.setPadding(AppUi.dp(c,4),0,AppUi.dp(c,4),0); AppUi.tint(b); return b;
    }
    private static Button smallButton(Context c,String text){
        Button b=new Button(c); b.setText(text); b.setTextSize(10); b.setAllCaps(false); b.setTextColor(AppUi.TEXT);
        b.setPadding(AppUi.dp(c,3),0,AppUi.dp(c,3),0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        b.setBackground(AppUi.ripple(c,AppUi.CONTROL,10,AppUi.CONTROL_BORDER)); return b;
    }

    /** Reorganiza os 16 canais sem deixar a GridLayout com parâmetros da orientação anterior. */
    public void updateTrackGridOrientation(Configuration cfg){
        int columns = 1;
        int childCount=trackGrid.getChildCount();
        View[] channelViews=new View[childCount];
        for(int i=0;i<childCount;i++) channelViews[i]=trackGrid.getChildAt(i);
        trackGrid.removeAllViews();
        trackGrid.setColumnCount(columns);
        trackGrid.setRowCount((childCount+columns-1)/columns);
        for(int i=0;i<childCount;i++){
            GridLayout.LayoutParams params=new GridLayout.LayoutParams(
                    GridLayout.spec(i/columns,1f),GridLayout.spec(i%columns,1f));
            params.width=0;
            params.height=GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(AppUi.dp(context,3),AppUi.dp(context,3),
                    AppUi.dp(context,3),AppUi.dp(context,3));
            trackGrid.addView(channelViews[i],params);
        }
        trackGrid.requestLayout();
    }
    public void showMidiActivity(int channel) { /* Não ocupa espaço no cabeçalho. */ }
    private static String noteName(int note){
        return new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"}[note%12]+(note/12-1);
    }
}