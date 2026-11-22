package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/** Knob vertical reutilizável, com suporte a teclado/acessibilidade. */
public final class KnobView extends View {
    public interface Listener {
        void onKnobChanged(KnobView knob, float value, boolean tracking);
    }

    private static final float START = 135f;
    private static final float SWEEP = 270f;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float density;
    private final float dragRange;
    private final int preferredSizeDp;

    private float value;
    private float downY;
    private float downValue;
    private boolean moved;
    private Listener listener;

    public KnobView(Context context) {
        this(context, 92);
    }

    public KnobView(Context context, int preferredSizeDp) {
        super(context);
        this.preferredSizeDp = Math.max(48, preferredSizeDp);
        density = getResources().getDisplayMetrics().density;
        dragRange = 180f * density;

        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setStrokeWidth(5.5f * density);
        track.setColor(AppUi.CONTROL);

        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setStrokeWidth(5.5f * density);
        arc.setColor(VisualTheme.EFFECTS);

        body.setColor(Color.rgb(34, 45, 66));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1f * density);
        rim.setColor(Color.argb(150, 117, 102, 143));

        highlight.setStyle(Paint.Style.STROKE);
        highlight.setStrokeWidth(1f * density);
        highlight.setColor(Color.argb(55, 255, 255, 255));

        mark.setStyle(Paint.Style.STROKE);
        mark.setStrokeCap(Paint.Cap.ROUND);
        mark.setStrokeWidth(3f * density);
        mark.setColor(AppUi.TEXT);

        setClickable(true);
        setFocusable(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = clamp(value);
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : .4f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.round(preferredSizeDp * density);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec),
                resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x = getWidth() / 2f;
        float y = getHeight() / 2f;
        float stroke = track.getStrokeWidth();
        float radius = Math.min(x, y) - stroke - density;
        oval.set(x - radius, y - radius, x + radius, y + radius);

        canvas.drawArc(oval, START, SWEEP, false, track);
        if (value > .001f) {
            canvas.drawArc(oval, START, SWEEP * value, false, arc);
        }

        float innerRadius = radius - stroke * 1.55f;
        body.setShader(new RadialGradient(
                x - innerRadius * .28f,
                y - innerRadius * .34f,
                innerRadius * 1.55f,
                new int[] {
                    Color.rgb(81, 70, 107),
                    Color.rgb(51, 43, 72),
                    Color.rgb(33, 28, 45)
                },
                new float[] {0f, .48f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, innerRadius, body);
        body.setShader(null);

        canvas.drawCircle(x, y, innerRadius, rim);
        RectF innerHighlight = new RectF(
                x - innerRadius + density * 2,
                y - innerRadius + density * 2,
                x + innerRadius - density * 2,
                y + innerRadius - density * 2);
        canvas.drawArc(innerHighlight, 205f, 130f, false, highlight);

        double angle = Math.toRadians(START + SWEEP * value);
        float startX = x + (float) Math.cos(angle) * innerRadius * .34f;
        float startY = y + (float) Math.sin(angle) * innerRadius * .34f;
        float endX = x + (float) Math.cos(angle) * innerRadius * .82f;
        float endY = y + (float) Math.sin(angle) * innerRadius * .82f;
        canvas.drawLine(startX, startY, endX, endY, mark);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                downValue = value;
                moved = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE: {
                float delta = downY - event.getY();
                if (Math.abs(delta) > 4 * density) moved = true;
                float next = clamp(downValue + delta / dragRange);
                if (next != value) {
                    value = next;
                    invalidate();
                    if (listener != null) listener.onKnobChanged(this, value, true);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!moved) {
                    performClick();
                } else if (listener != null) {
                    listener.onKnobChanged(this, value, false);
                }
                releaseParent();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (moved && listener != null) {
                    listener.onKnobChanged(this, value, false);
                }
                releaseParent();
                return true;
            default:
                return true;
        }
    }

    private void releaseParent() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.SeekBar");
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 1f, value));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle args) {
        if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()
                && args != null && isEnabled()) {
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

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
