package com.rodolphofriedrich.fluidsynth;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;
/** Interface global de Reverb e Chorus; não acessa o motor nativo. */
public final class EffectsScreen {
    public static final int REVERB=0,CHORUS=1;
    public interface Listener{void onEffectActive(int effect,boolean active);void onParamChanged(int effect,int param,float value);}
    private static final class Spec{
        final String name,unit;final float min,max,def;final int decimals;
        Spec(String n,String u,float a,float b,float d,int p){name=n;unit=u;min=a;max=b;def=d;decimals=p;}
        float toValue(float t){float v=min+(max-min)*Math.max(0,Math.min(1,t));float p=(float)Math.pow(10,decimals);return Math.round(v*p)/p;}
        float norm(float v){return Math.max(0,Math.min(1,(v-min)/(max-min)));}
        String format(float v){return String.format(Locale.getDefault(),"%."+decimals+"f%s",v,unit);}
    }
    private static final String[] NAMES={"REVERB","CHORUS"};
    private static final Spec[][] SPECS={
        {new Spec("Tamanho da sala","",0,1.2f,.2f,2),new Spec("Amortecimento","",0,1,0,2),new Spec("Largura","",0,100,100,1),new Spec("Nível","",0,1,.9f,2)},
        {new Spec("Vozes","",0,99,3,0),new Spec("Nível","",0,10,2,1),new Spec("Velocidade"," Hz",.1f,5,.3f,1),new Spec("Profundidade"," ms",0,21,8,1)}};
    public final ScrollView scrollView;private final Context context;private final float[][] values=new float[2][4];
    private final KnobView[][] knobs=new KnobView[2][4];private final TextView[][] labels=new TextView[2][4];
    private final Switch[] active=new Switch[2];private final View[] pages=new View[2];private final Button[] tabs=new Button[2];
    private Listener listener;private boolean silent;private long lastSend;
    public EffectsScreen(Context c){context=c;LinearLayout root=new LinearLayout(c);root.setOrientation(1);root.setPadding(dp(16),dp(24),dp(16),dp(24));root.setBackgroundColor(AppUi.BG);
        LinearLayout header=AppUi.panel(c,root);header.addView(AppUi.sectionLabel(c,"EFEITOS"));TextView hint=new TextView(c);hint.setText("Aplicados a todo o sintetizador");hint.setTextColor(AppUi.MUTED);hint.setTextSize(12);header.addView(hint);
        LinearLayout row=new LinearLayout(c);header.addView(row);for(int i=0;i<2;i++){final int e=i;Button b=new Button(c);b.setText(NAMES[i]);AppUi.styleButton(c,b,AppUi.CONTROL,AppUi.TEXT,44,13);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(3),0,dp(3),0);row.addView(b,p);tabs[i]=b;b.setOnClickListener(v->selectTab(e));}
        for(int i=0;i<2;i++)buildPage(root,i);scrollView=new ScrollView(c);scrollView.setFillViewport(true);scrollView.addView(root);selectTab(REVERB);}
    public void setListener(Listener l){listener=l;}public float[] values(int e){return values[e].clone();}
    public void setParam(int e,int p,float v){Spec s=SPECS[e][p];float safe=Math.max(s.min,Math.min(s.max,v));values[e][p]=safe;knobs[e][p].setValue(s.norm(safe));labels[e][p].setText(s.format(safe));}
    public void setActive(int e,boolean on){silent=true;active[e].setChecked(on);silent=false;for(KnobView k:knobs[e])k.setEnabled(on);}
    private void buildPage(LinearLayout root,int e){LinearLayout page=AppUi.panel(context,root);pages[e]=page;LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t=new TextView(context);t.setText("Ativo");t.setTextColor(AppUi.TEXT);t.setTextSize(15);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));Switch sw=new Switch(context);
        sw.setThumbTintList(ColorStateList.valueOf(AppUi.ACCENT));sw.setTrackTintList(ColorStateList.valueOf(Color.argb(110,79,140,255)));sw.setChecked(true);sw.setContentDescription("Ativar "+NAMES[e]);
        sw.setOnCheckedChangeListener((b,on)->{for(KnobView k:knobs[e])if(k!=null)k.setEnabled(on);if(!silent&&listener!=null)listener.onEffectActive(e,on);});row.addView(sw);active[e]=sw;page.addView(row);
        GridLayout grid=new GridLayout(context);grid.setColumnCount(2);for(int p=0;p<4;p++){final int param=p;Spec s=SPECS[e][p];LinearLayout cell=new LinearLayout(context);cell.setOrientation(1);cell.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView name=new TextView(context);name.setText(s.name);name.setTextColor(AppUi.MUTED);name.setTextSize(12);cell.addView(name);KnobView k=new KnobView(context);k.setContentDescription(s.name);k.setValue(s.norm(s.def));
            k.setListener((view,v,tracking)->onKnob(e,param,v,tracking));cell.addView(k,new LinearLayout.LayoutParams(dp(88),dp(88)));
            TextView label=new TextView(context);label.setText(s.format(s.def));label.setTextColor(AppUi.TEXT);label.setTextSize(14);label.setTypeface(android.graphics.Typeface.DEFAULT,1);cell.addView(label);
            values[e][p]=s.def;knobs[e][p]=k;labels[e][p]=label;GridLayout.LayoutParams gp=new GridLayout.LayoutParams(GridLayout.spec(p/2,1f),GridLayout.spec(p%2,1f));gp.width=0;grid.addView(cell,gp);}
        page.addView(grid);Button reset=new Button(context);reset.setText("Restaurar padrão");AppUi.styleButton(context,reset,AppUi.CONTROL,AppUi.TEXT,48,14);reset.setOnClickListener(v->{for(int p=0;p<4;p++)setParam(e,p,SPECS[e][p].def);if(listener!=null)listener.onParamChanged(e,-1,0);});page.addView(reset);}
    private void onKnob(int e,int p,float t,boolean tracking){float v=SPECS[e][p].toValue(t);labels[e][p].setText(SPECS[e][p].format(v));if(v==values[e][p])return;values[e][p]=v;
        long now=SystemClock.uptimeMillis();if(tracking&&now-lastSend<30)return;lastSend=now;if(listener!=null)listener.onParamChanged(e,p,v);}
    private void selectTab(int selected){for(int i=0;i<2;i++){boolean on=i==selected;pages[i].setVisibility(on?View.VISIBLE:View.GONE);tabs[i].setBackground(AppUi.rounded(context,on?AppUi.ACCENT:AppUi.CONTROL,12,0));tabs[i].setTextColor(on?Color.WHITE:AppUi.TEXT);}}
    private int dp(float v){return Math.round(v*context.getResources().getDisplayMetrics().density);}
}
