package com.nitsutech.pillscheck.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class BlisterCanvasView extends View {

    public interface OnSlotClickListener {
        void onSlotToggled(int slotIndex, boolean isFilled, int remainingCount);
    }

    private int rows = 2;
    private int cols = 5;
    private boolean[] slotStates = new boolean[10];
    private String blisterType = "silver_round_blue";
    private int highlightedSlotIndex = -1;
    private OnSlotClickListener listener;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cavityEmptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cavityRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF blisterRect = new RectF();
    private final RectF tempRect = new RectF();

    // Cache slot center coordinates for fast accurate touch detection & drawing
    private float[] slotX = new float[30];
    private float[] slotY = new float[30];
    private float[] slotRadius = new float[30];

    public BlisterCanvasView(Context context) {
        super(context);
        init();
    }

    public BlisterCanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlisterCanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setTextSize(spToPx(8.5f));
        textPaint.setLetterSpacing(0.08f);
        textPaint.setFakeBoldText(true);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.2f));
    }

    public void configure(int rows, int cols, String blisterType, boolean[] slots) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.blisterType = blisterType != null ? blisterType : "silver_round_blue";

        int total = isFolcresStaggered() ? 30 : (this.rows * this.cols);
        this.slotStates = new boolean[total];
        if (slots != null) {
            for (int i = 0; i < Math.min(slots.length, total); i++) {
                this.slotStates[i] = slots[i];
            }
        }
        if (slotX.length < total) {
            slotX = new float[total];
            slotY = new float[total];
            slotRadius = new float[total];
        }
        requestLayout();
        invalidate();
    }

    public void setHighlightedSlotIndex(int index) {
        this.highlightedSlotIndex = index;
        invalidate();
    }

    public int getHighlightedSlotIndex() {
        return highlightedSlotIndex;
    }

    public boolean isFolcresStaggered() {
        return "silver_round_blue".equals(blisterType) || rows == 4;
    }

    public void setOnSlotClickListener(OnSlotClickListener listener) {
        this.listener = listener;
    }

    public boolean[] getSlotStates() {
        return slotStates;
    }

    public int getRemainingCount() {
        int count = 0;
        for (boolean b : slotStates) {
            if (b) count++;
        }
        return count;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int targetWidth = widthMode == MeasureSpec.EXACTLY ? widthSize : dpToPx(340);

        // Well-proportioned heights that fit all 3 blisters on screen without distortion
        int targetHeight;
        if (isFolcresStaggered()) {
            targetHeight = dpToPx(112); // 4 rows
        } else if ("silver_capsule_yellow".equals(blisterType)) {
            targetHeight = dpToPx(84);  // 2 rows vertical capsules
        } else {
            targetHeight = dpToPx(76);  // 2 rows round tablets
        }

        setMeasuredDimension(targetWidth, targetHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cornerRadius = dpToPx(10);
        blisterRect.set(dpToPx(4), dpToPx(3), w - dpToPx(4), h - dpToPx(3));

        // 1. Draw blister foil background
        drawBlisterBackground(canvas, blisterRect, cornerRadius);

        // 2. Draw embossed text & details
        drawEmbossedDetails(canvas, blisterRect);

        // 3. Compute and draw cavities and pills
        if (isFolcresStaggered()) {
            drawFolcresStaggeredCavities(canvas, blisterRect);
        } else {
            drawRegularGridCavities(canvas, blisterRect);
        }
    }

    private void drawBlisterBackground(Canvas canvas, RectF rect, float cornerRadius) {
        if ("amber_round_yellow".equals(blisterType)) {
            bgPaint.setShader(new LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    new int[]{Color.rgb(234, 88, 12), Color.rgb(194, 65, 12), Color.rgb(249, 115, 22)},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            borderPaint.setColor(Color.rgb(154, 52, 18));
        } else {
            bgPaint.setShader(new LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    new int[]{Color.rgb(226, 232, 240), Color.rgb(195, 204, 217), Color.rgb(241, 245, 249)},
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP
            ));
            borderPaint.setColor(Color.rgb(148, 163, 184));
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);

        // Minoxidil horizontal center crease line
        if ("amber_round_yellow".equals(blisterType)) {
            float centerY = rect.centerY();
            Paint creasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            creasePaint.setStrokeWidth(dpToPx(2.5f));
            creasePaint.setStyle(Paint.Style.STROKE);
            creasePaint.setColor(Color.argb(120, 124, 45, 18));
            canvas.drawLine(rect.left + dpToPx(14), centerY, rect.right - dpToPx(24), centerY, creasePaint);

            Paint creaseHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
            creaseHighlight.setStrokeWidth(dpToPx(1f));
            creaseHighlight.setStyle(Paint.Style.STROKE);
            creaseHighlight.setColor(Color.argb(150, 254, 215, 170));
            canvas.drawLine(rect.left + dpToPx(14), centerY + dpToPx(2f), rect.right - dpToPx(24), centerY + dpToPx(2f), creaseHighlight);
        }
    }

    private void drawEmbossedDetails(Canvas canvas, RectF rect) {
        if (isFolcresStaggered()) {
            // Embossed text on RIGHT margin (matching physical blister)
            canvas.save();
            canvas.rotate(90, rect.right - dpToPx(10), rect.centerY());
            textPaint.setColor(Color.argb(120, 100, 116, 139));
            canvas.drawText("LB807 V03/28", rect.right - dpToPx(10) - dpToPx(24), rect.centerY() + dpToPx(3), textPaint);
            canvas.restore();
        } else if ("amber_round_yellow".equals(blisterType)) {
            // Embossed text on RIGHT
            canvas.save();
            canvas.rotate(90, rect.right - dpToPx(10), rect.centerY());
            textPaint.setColor(Color.argb(140, 120, 53, 15));
            canvas.drawText("10000 V11/27", rect.right - dpToPx(10) - dpToPx(22), rect.centerY() + dpToPx(3), textPaint);
            canvas.restore();
        }
    }

    private void drawFolcresStaggeredCavities(Canvas canvas, RectF rect) {
        float leftMargin = dpToPx(12);
        float rightMargin = dpToPx(26); // room for embossed batch text
        float topMargin = dpToPx(6);
        float bottomMargin = dpToPx(6);

        float availW = rect.width() - leftMargin - rightMargin;
        float availH = rect.height() - topMargin - bottomMargin;

        float stepX = availW / 8.0f;
        float stepY = availH / 4.0f;
        float radius = Math.min(stepX * 0.40f, stepY * 0.42f);

        // Row 0: 8 cavities (starts on left)
        // Row 1: 7 cavities (shifted right by half step)
        // Row 2: 8 cavities (starts on left)
        // Row 3: 7 cavities (shifted right by half step)
        int[] rowCols = new int[]{8, 7, 8, 7};
        int slotIndex = 0;

        for (int r = 0; r < 4; r++) {
            int colsInRow = rowCols[r];
            float y = rect.top + topMargin + (r * stepY) + (stepY / 2.0f);
            // Even rows (0, 2) start on the left; Odd rows (1, 3) shift right by half a step
            float startX = rect.left + leftMargin + (r % 2 == 1 ? stepX * 0.5f : 0f);

            for (int c = 0; c < colsInRow; c++) {
                if (slotIndex >= slotStates.length) break;

                float x = startX + (c * stepX) + (stepX / 2.0f);
                slotX[slotIndex] = x;
                slotY[slotIndex] = y;
                slotRadius[slotIndex] = radius;

                boolean isFilled = slotStates[slotIndex];
                boolean isHighlighted = (slotIndex == highlightedSlotIndex);
                drawRoundSlot(canvas, x, y, radius, isFilled, isHighlighted);
                slotIndex++;
            }
        }
    }

    private void drawRegularGridCavities(Canvas canvas, RectF rect) {
        float leftPadding = dpToPx(14);
        float rightPadding = dpToPx(26);
        float topPadding = dpToPx(6);
        float bottomPadding = dpToPx(6);

        float gridWidth = rect.width() - leftPadding - rightPadding;
        float gridHeight = rect.height() - topPadding - bottomPadding;

        float cellW = gridWidth / (float) cols;
        float cellH = gridHeight / (float) rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index >= slotStates.length) break;

                float cx = rect.left + leftPadding + (c * cellW) + (cellW / 2f);
                float cy = rect.top + topPadding + (r * cellH) + (cellH / 2f);

                slotX[index] = cx;
                slotY[index] = cy;

                boolean isFilled = slotStates[index];
                boolean isHighlighted = (index == highlightedSlotIndex);

                if ("silver_capsule_yellow".equals(blisterType)) {
                    // Vertical elongated capsule (standing upright: height > width)
                    float capW = Math.min(dpToPx(16), cellW * 0.35f);
                    float capH = Math.min(dpToPx(32), cellH * 0.78f);
                    slotRadius[index] = capH / 2f;
                    drawCapsuleSlot(canvas, cx, cy, capW, capH, isFilled, isHighlighted);
                } else {
                    float radius = Math.min(cellW * 0.36f, cellH * 0.38f);
                    slotRadius[index] = radius;
                    drawRoundSlot(canvas, cx, cy, radius, isFilled, isHighlighted);
                }
            }
        }
    }

    private void drawRoundSlot(Canvas canvas, float cx, float cy, float radius, boolean isFilled, boolean isHighlighted) {
        float rimRadius = radius * 1.25f;

        if ("amber_round_yellow".equals(blisterType)) {
            cavityRimPaint.setColor(Color.argb(100, 124, 45, 18));
        } else {
            cavityRimPaint.setColor(Color.argb(70, 100, 116, 139));
        }
        canvas.drawCircle(cx, cy, rimRadius, cavityRimPaint);

        if (!isFilled) {
            if ("amber_round_yellow".equals(blisterType)) {
                cavityEmptyPaint.setColor(Color.rgb(180, 83, 9));
            } else {
                cavityEmptyPaint.setColor(Color.rgb(148, 163, 184));
            }
            canvas.drawCircle(cx, cy, radius, cavityEmptyPaint);

            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(110, 15, 23, 42));
            canvas.drawCircle(cx + dpToPx(0.8f), cy + dpToPx(0.8f), radius * 0.85f, shadowPaint);

            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(dpToPx(1f));
            highlightPaint.setColor(Color.argb(160, 255, 255, 255));
            tempRect.set(cx - radius * 0.7f, cy - radius * 0.7f, cx + radius * 0.7f, cy + radius * 0.7f);
            canvas.drawArc(tempRect, 210, 100, false, highlightPaint);
        } else {
            if (isFolcresStaggered()) {
                pillPaint.setShader(new RadialGradient(
                        cx - radius * 0.25f, cy - radius * 0.35f, radius * 1.1f,
                        new int[]{Color.rgb(56, 189, 248), Color.rgb(2, 132, 199), Color.rgb(3, 105, 161)},
                        new float[]{0f, 0.6f, 1f},
                        Shader.TileMode.CLAMP
                ));
            } else {
                pillPaint.setShader(new RadialGradient(
                        cx - radius * 0.25f, cy - radius * 0.35f, radius * 1.1f,
                        new int[]{Color.rgb(254, 240, 138), Color.rgb(234, 179, 8), Color.rgb(202, 138, 4)},
                        new float[]{0f, 0.65f, 1f},
                        Shader.TileMode.CLAMP
                ));
            }

            // If highlighted as today's dose: thin contrasting border (borde finito de otro color)
            if (isHighlighted) {
                Paint targetBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                targetBorderPaint.setStyle(Paint.Style.STROKE);
                targetBorderPaint.setStrokeWidth(dpToPx(1.6f));
                if (isFolcresStaggered()) {
                    targetBorderPaint.setColor(Color.rgb(245, 158, 11)); // Golden amber for blue pill
                } else {
                    targetBorderPaint.setColor(Color.rgb(14, 165, 233)); // Sky cyan for yellow pill
                }
                canvas.drawCircle(cx, cy, radius + dpToPx(1.8f), targetBorderPaint);
            }

            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(80, 0, 0, 0));
            canvas.drawCircle(cx + dpToPx(1.2f), cy + dpToPx(1.5f), radius, shadowPaint);

            canvas.drawCircle(cx, cy, radius, pillPaint);

            highlightPaint.setStyle(Paint.Style.FILL);
            highlightPaint.setColor(Color.argb(170, 255, 255, 255));
            canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.35f, radius * 0.26f, highlightPaint);
        }
    }

    private void drawCapsuleSlot(Canvas canvas, float cx, float cy, float capW, float capH, boolean isFilled, boolean isHighlighted) {
        float r = capW / 2f;
        tempRect.set(cx - r, cy - capH / 2f, cx + r, cy + capH / 2f);

        RectF rimRect = new RectF(tempRect);
        rimRect.inset(-dpToPx(2f), -dpToPx(2f));
        cavityRimPaint.setColor(Color.argb(70, 100, 116, 139));
        canvas.drawRoundRect(rimRect, r + dpToPx(1.5f), r + dpToPx(1.5f), cavityRimPaint);

        if (!isFilled) {
            cavityEmptyPaint.setColor(Color.rgb(148, 163, 184));
            canvas.drawRoundRect(tempRect, r, r, cavityEmptyPaint);

            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(120, 15, 23, 42));
            RectF innerShadow = new RectF(tempRect);
            innerShadow.inset(dpToPx(1.5f), dpToPx(1.5f));
            canvas.drawRoundRect(innerShadow, r - dpToPx(1f), r - dpToPx(1f), shadowPaint);

            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(dpToPx(1f));
            highlightPaint.setColor(Color.argb(160, 255, 255, 255));
            canvas.drawLine(tempRect.left + dpToPx(2f), tempRect.top + dpToPx(3f), tempRect.left + dpToPx(2f), tempRect.bottom - dpToPx(4f), highlightPaint);
        } else {
            // If highlighted as today's dose: thin contrasting border (borde finito de otro color)
            if (isHighlighted) {
                Paint targetBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                targetBorderPaint.setStyle(Paint.Style.STROKE);
                targetBorderPaint.setStrokeWidth(dpToPx(1.6f));
                targetBorderPaint.setColor(Color.rgb(14, 165, 233)); // Sky cyan
                RectF haloRect = new RectF(tempRect);
                haloRect.inset(-dpToPx(2.2f), -dpToPx(2.2f));
                canvas.drawRoundRect(haloRect, r + dpToPx(2.2f), r + dpToPx(2.2f), targetBorderPaint);
            }

            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(90, 0, 0, 0));
            RectF shadowR = new RectF(tempRect);
            shadowR.offset(dpToPx(1.2f), dpToPx(1.5f));
            canvas.drawRoundRect(shadowR, r, r, shadowPaint);

            pillPaint.setShader(new LinearGradient(
                    tempRect.left, tempRect.top, tempRect.right, tempRect.bottom,
                    new int[]{Color.rgb(253, 224, 71), Color.rgb(234, 179, 8), Color.rgb(202, 138, 4)},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRoundRect(tempRect, r, r, pillPaint);

            Paint seamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            seamPaint.setStyle(Paint.Style.STROKE);
            seamPaint.setStrokeWidth(dpToPx(1.2f));
            seamPaint.setColor(Color.argb(140, 161, 98, 7));
            canvas.drawLine(tempRect.left, cy, tempRect.right, cy, seamPaint);

            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(dpToPx(1.4f));
            highlightPaint.setColor(Color.argb(180, 255, 255, 255));
            canvas.drawLine(cx - r * 0.45f, tempRect.top + dpToPx(3f), cx - r * 0.45f, tempRect.bottom - dpToPx(4f), highlightPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            int touchedIndex = getTouchedSlotIndex(x, y);
            if (touchedIndex >= 0 && touchedIndex < slotStates.length) {
                slotStates[touchedIndex] = !slotStates[touchedIndex];
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                invalidate();
                if (listener != null) {
                    listener.onSlotToggled(touchedIndex, slotStates[touchedIndex], getRemainingCount());
                }
                return true;
            }
        }
        return true;
    }

    private int getTouchedSlotIndex(float touchX, float touchY) {
        int bestIdx = -1;
        float bestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < slotStates.length; i++) {
            float dx = touchX - slotX[i];
            float dy = touchY - slotY[i];
            float distSq = dx * dx + dy * dy;
            float touchRadius = Math.max(dpToPx(20), slotRadius[i] * 1.5f);
            if (distSq <= touchRadius * touchRadius && distSq < bestDistSq) {
                bestDistSq = distSq;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }
}
