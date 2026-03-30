package com.example.pathfinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.ComposeShader;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Discord-style color picker:
 *   - Large square: X = saturation (left→right), Y = brightness (top→bottom, bright→dark)
 *     with a white→transparent horizontal gradient over a black→transparent vertical gradient,
 *     tinted by the current hue.
 *   - Thin hue bar below the square.
 *   - Small circle selector on the square, tick selector on the hue bar.
 */
public class ColorPickerView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private static final float HUE_BAR_HEIGHT_DP = 20f;
    private static final float HUE_BAR_MARGIN_DP = 16f;
    private static final float SELECTOR_RADIUS_DP = 10f;
    private static final float HUE_CURSOR_WIDTH_DP = 4f;

    // Current HSV state
    private float hue        = 0f;   // 0–360
    private float saturation = 1f;   // 0–1  (X in square)
    private float brightness = 1f;   // 0–1  (Y in square, 1=bright at top)

    // Rects
    private final RectF squareRect  = new RectF();
    private final RectF hueBarRect  = new RectF();

    // Paints
    private final Paint huePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint squarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hueTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Pixel density
    private float density;

    private OnColorChangedListener listener;

    public ColorPickerView(Context context) {
        super(context); init(context);
    }
    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        selectorRingPaint.setStyle(Paint.Style.STROKE);
        selectorRingPaint.setStrokeWidth(2.5f * density);
        selectorRingPaint.setColor(Color.WHITE);

        hueTickPaint.setStyle(Paint.Style.STROKE);
        hueTickPaint.setStrokeWidth(HUE_CURSOR_WIDTH_DP * density);
        hueTickPaint.setColor(Color.WHITE);

        // Drop shadow on selector
        selectorRingPaint.setShadowLayer(3f * density, 0, 1f * density, 0x66000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // needed for shadow
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        float hueH   = HUE_BAR_HEIGHT_DP * density;
        float margin = HUE_BAR_MARGIN_DP  * density;

        squareRect.set(0, 0, w, h - hueH - margin);
        hueBarRect.set(0, h - hueH, w, h);

        buildHueShader();
        buildSquareShader();
    }

    private void buildHueShader() {
        int[] hueColors = new int[361];
        for (int i = 0; i <= 360; i++) {
            hueColors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }
        huePaint.setShader(new LinearGradient(
                hueBarRect.left, 0, hueBarRect.right, 0,
                hueColors, null, Shader.TileMode.CLAMP));
    }

    private void buildSquareShader() {
        if (squareRect.width() == 0) return;

        // White → hue color  (left to right = sat 0→1)
        int pureHue = Color.HSVToColor(new float[]{hue, 1f, 1f});
        Shader satShader = new LinearGradient(
                squareRect.left, 0, squareRect.right, 0,
                Color.WHITE, pureHue, Shader.TileMode.CLAMP);

        // Transparent → black  (top to bottom = bri 1→0)
        Shader briShader = new LinearGradient(
                0, squareRect.top, 0, squareRect.bottom,
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);

        squarePaint.setShader(new ComposeShader(briShader, satShader, PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (squareRect.width() == 0) return;

        float r = 10f * density; // corner radius

        // ── 1. Saturation / Brightness square ──────────────────────────────
        canvas.drawRoundRect(squareRect, r, r, squarePaint);

        // ── 2. Hue bar ─────────────────────────────────────────────────────
        canvas.drawRoundRect(hueBarRect, (HUE_BAR_HEIGHT_DP / 2f) * density,
                (HUE_BAR_HEIGHT_DP / 2f) * density, huePaint);

        // ── 3. Square selector circle ───────────────────────────────────────
        float sx = squareRect.left + saturation * squareRect.width();
        float sy = squareRect.top  + (1f - brightness) * squareRect.height();
        sx = Math.max(squareRect.left, Math.min(squareRect.right,  sx));
        sy = Math.max(squareRect.top,  Math.min(squareRect.bottom, sy));

        float sr = SELECTOR_RADIUS_DP * density;
        selectorFillPaint.setColor(getCurrentColor());
        canvas.drawCircle(sx, sy, sr, selectorFillPaint);
        canvas.drawCircle(sx, sy, sr, selectorRingPaint);

        // ── 4. Hue bar cursor (vertical line) ──────────────────────────────
        float hx = hueBarRect.left + (hue / 360f) * hueBarRect.width();
        float tickW = (HUE_CURSOR_WIDTH_DP / 2f) * density;
        float tickH = (HUE_BAR_HEIGHT_DP / 2f) * density;
        // draw a rounded white rect as the tick
        Paint tickFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickFill.setColor(Color.WHITE);
        tickFill.setShadowLayer(2f * density, 0, 1f * density, 0x66000000);
        canvas.drawRoundRect(
                hx - tickW, hueBarRect.top    - 3 * density,
                hx + tickW, hueBarRect.bottom + 3 * density,
                tickW, tickW, tickFill);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (isTouchInOrNear(squareRect, x, y)) {
            saturation = Math.max(0f, Math.min(1f,
                    (x - squareRect.left) / squareRect.width()));
            brightness = Math.max(0f, Math.min(1f,
                    1f - (y - squareRect.top) / squareRect.height()));
            notifyAndInvalidate();
            return true;
        }

        if (isTouchInOrNear(hueBarRect, x, y)) {
            hue = Math.max(0f, Math.min(360f,
                    (x - hueBarRect.left) / hueBarRect.width() * 360f));
            buildSquareShader(); // rebuild square gradient for new hue
            notifyAndInvalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean isTouchInOrNear(RectF rect, float x, float y) {
        float slack = 24f * density;
        return x >= rect.left  - slack && x <= rect.right  + slack
                && y >= rect.top   - slack && y <= rect.bottom + slack;
    }

    private void notifyAndInvalidate() {
        invalidate();
        if (listener != null) listener.onColorChanged(getCurrentColor());
    }

    private int getCurrentColor() {
        return Color.HSVToColor(new float[]{hue, saturation, brightness});
    }

    public int getSelectedColor() {
        return getCurrentColor();
    }

    public String getSelectedColorHex() {
        return String.format("#%06X", 0xFFFFFF & getCurrentColor());
    }

    public void setOnColorChangedListener(OnColorChangedListener l) {
        this.listener = l;
    }

    /** Pre-load an existing color (e.g. when editing a tag) */
    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue        = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        buildSquareShader();
        invalidate();
    }
}