package com.rodolphofriedrich.fluidsynth;
import android.app.Activity;
import android.content.res.Configuration;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
/** Gerencia o painel de efeitos responsivo, sem acessar o motor de áudio. */
public final class EffectsWorkspaceController {
 private final Activity activity; private final LinearLayout workspace;
 private final View midiView,effectsView; private final Button toggleButton; private boolean open;
 public EffectsWorkspaceController(Activity a,LinearLayout w,View m,View e,Button b){
  activity=a;workspace=w;midiView=m;effectsView=e;toggleButton=b;apply();
 }
 public boolean isOpen(){return open;}
 public void toggle(){setOpen(!open);}
 public void setOpen(boolean value){open=value;apply();}
 public void onConfigurationChanged(){apply();}
 private boolean splitMode(){
  Configuration c=activity.getResources().getConfiguration();
  return c.smallestScreenWidthDp>=600||c.screenWidthDp>=640;
 }
 private void apply(){
  TransitionManager.beginDelayedTransition(workspace,new AutoTransition().setDuration(220));
  LinearLayout.LayoutParams m,e;
  if(!open){
   midiView.setVisibility(View.VISIBLE);effectsView.setVisibility(View.GONE);
   m=new LinearLayout.LayoutParams(-1,-1,1f);e=new LinearLayout.LayoutParams(0,0,0f);
  }else if(splitMode()){
   midiView.setVisibility(View.VISIBLE);effectsView.setVisibility(View.VISIBLE);
   m=new LinearLayout.LayoutParams(0,-1,.64f);e=new LinearLayout.LayoutParams(0,-1,.36f);
   e.leftMargin=AppUi.dp(activity,6);
  }else{
   midiView.setVisibility(View.GONE);effectsView.setVisibility(View.VISIBLE);
   m=new LinearLayout.LayoutParams(0,0,0f);e=new LinearLayout.LayoutParams(-1,-1,1f);
  }
  midiView.setLayoutParams(m);effectsView.setLayoutParams(e);
  toggleButton.setSelected(open);
  toggleButton.setBackground(AppUi.ripple(activity,open?AppUi.ACCENT:AppUi.CONTROL,10,AppUi.BORDER));
  toggleButton.setContentDescription(activity.getString(open?R.string.effects_panel_close:R.string.effects_panel_open));
 }
}