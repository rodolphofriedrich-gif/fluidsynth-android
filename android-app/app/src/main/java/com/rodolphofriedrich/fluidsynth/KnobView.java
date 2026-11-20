package com.rodolphofriedrich.fluidsynth;
import android.content.Context;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
/** Knob vertical reutilizável, com suporte a teclado/acessibilidade. */
public final class KnobView extends View {
    public interface Listener{void onKnobChanged(KnobView knob,float value,boolean tracking);}
    private static final float START=135f,SWEEP=270f;
    private final Paint track=new Paint(3),arc=new Paint(3),body=new Paint(3),mark=new Paint(3);
    private final RectF oval=new RectF();private final float density,dragRange;
    private float value,downY,downValue;private boolean moved;private Listener listener;
    public KnobView(Context c){super(c);density=getResources().getDisplayMetrics().density;dragRange=180f*density;
        track.setStyle(Paint.Style.STROKE);track.setStrokeCap(Paint.Cap.ROUND);track.setStrokeWidth(5*density);track.setColor(AppUi.CONTROL);
        arc.setStyle(Paint.Style.STROKE);arc.setStrokeCap(Paint.Cap.ROUND);arc.setStrokeWidth(5*density);arc.setColor(AppUi.ACCENT);
        body.setColor(Color.rgb(46,60,84));mark.setStyle(Paint.Style.STROKE);mark.setStrokeCap(Paint.Cap.ROUND);mark.setStrokeWidth(3*density);mark.setColor(AppUi.TEXT);
        setClickable(true);setFocusable(true);}
    public void setListener(Listener l){listener=l;} public float getValue(){return value;}
    public void setValue(float v){value=clamp(v);invalidate();}
    @Override public void setEnabled(boolean e){super.setEnabled(e);setAlpha(e?1f:.4f);}
    @Override protected void onMeasure(int w,int h){int s=Math.round(88*density);setMeasuredDimension(resolveSize(s,w),resolveSize(s,h));}
    @Override protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,sw=track.getStrokeWidth(),r=Math.min(x,y)-sw;
        oval.set(x-r,y-r,x+r,y+r);c.drawArc(oval,START,SWEEP,false,track);if(value>.001f)c.drawArc(oval,START,SWEEP*value,false,arc);
        float in=r-sw*1.6f;c.drawCircle(x,y,in,body);double a=Math.toRadians(START+SWEEP*value);
        c.drawLine(x+(float)Math.cos(a)*in*.35f,y+(float)Math.sin(a)*in*.35f,x+(float)Math.cos(a)*in*.85f,y+(float)Math.sin(a)*in*.85f,mark);}
    @Override public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return false;switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:downY=e.getY();downValue=value;moved=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;
        case MotionEvent.ACTION_MOVE:{float dy=downY-e.getY();if(Math.abs(dy)>4*density)moved=true;float n=clamp(downValue+dy/dragRange);
            if(n!=value){value=n;invalidate();if(listener!=null)listener.onKnobChanged(this,value,true);}return true;}
        case MotionEvent.ACTION_UP:if(!moved)performClick();else if(listener!=null)listener.onKnobChanged(this,value,false);releaseParent();return true;
        case MotionEvent.ACTION_CANCEL:if(moved&&listener!=null)listener.onKnobChanged(this,value,false);releaseParent();return true;default:return true;}}
    private void releaseParent(){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.SeekBar");
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 1f, value));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
    }
    @Override public boolean performAccessibilityAction(int action, Bundle args) {
        if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId() && args != null && isEnabled()) {
            float next = clamp(args.getFloat(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value));
            if (next != value) {
                value = next;
                invalidate();
                if (listener != null) listener.onKnobChanged(this, value, false);
            }
            return true;
        }
        return super.performAccessibilityAction(action, args);
    }
    private static float clamp(float v){return Math.max(0f,Math.min(1f,v));}
}
