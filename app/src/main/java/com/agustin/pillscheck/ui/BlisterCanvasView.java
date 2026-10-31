package com.agustin.pillscheck.ui;

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
    private OnSlotClickListener listener;

    // Paints
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
        textPaint.setColor(Color.argb(90, 255, 255, 255));
        textPaint.setTextSize(spToPx(10));
        textPaint.setLetterSpacing(0.1f);
        textPaint.setFakeBoldText(true);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
    }

    public void configure(int rows, int cols, String blisterType, boolean[] slots) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.blisterType = blisterType != null ? blisterType : "silver_round_blue";

        int total = this.rows * this.cols;
        this.slotStates = new boolean[total];
        if (slots != null) {
            for (int i = 0; i < Math.min(slots.length, total); i++) {
                this.slotStates[i] = slots[i];
            }
        }
        requestLayout();
        invalidate();
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

        // Aspect ratio calculation
        float cellAspect = blisterType.contains("capsule") ? 1.35f : 1.0f;
        float headerPadding = dpToPx(24);
        float sideMargin = dpToPx(16);
        float availableWidth = targetWidth - (sideMargin * 2);
        float cellWidth = availableWidth / (float) cols;
        float cellHeight = cellWidth * cellAspect;
        float targetHeight = (cellHeight * rows) + headerPadding + dpToPx(16);

        setMeasuredDimension(targetWidth, (int) targetHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cornerRadius = dpToPx(14);
        blisterRect.set(dpToPx(8), dpToPx(8), w - dpToPx(8), h - dpToPx(8));

        // 1. Draw blister foil background
        drawBlisterBackground(canvas, blisterRect, cornerRadius);

        // 2. Draw embossed text & details
        drawEmbossedDetails(canvas, blisterRect);

        // 3. Draw cavities and pills
        drawCavitiesAndPills(canvas, blisterRect);
    }

    private void drawBlisterBackground(Canvas canvas, RectF rect, float cornerRadius) {
        if ("amber_round_yellow".equals(blisterType)) {
            // Amber / Orange translucent blister
            bgPaint.setShader(new LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    new int[]{Color.rgb(234, 88, 12), Color.rgb(194, 65, 12), Color.rgb(249, 115, 22)},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            borderPaint.setColor(Color.rgb(154, 52, 18));
        } else {
            // Metallic Silver aluminum blister
            bgPaint.setShader(new LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    new int[]{Color.rgb(226, 232, 240), Color.rgb(190, 200, 215), Color.rgb(241, 245, 249)},
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
            creasePaint.setStrokeWidth(dpToPx(3.5f));
            creasePaint.setStyle(Paint.Style.STROKE);
            creasePaint.setColor(Color.argb(120, 124, 45, 18));
            canvas.drawLine(rect.left + dpToPx(18), centerY, rect.right - dpToPx(18), centerY, creasePaint);

            Paint creaseHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
            creaseHighlight.setStrokeWidth(dpToPx(1.5f));
            creaseHighlight.setStyle(Paint.Style.STROKE);
            creaseHighlight.setColor(Color.argb(150, 254, 215, 170));
            canvas.drawLine(rect.left + dpToPx(18), centerY + dpToPx(2.5f), rect.right - dpToPx(18), centerY + dpToPx(2.5f), creaseHighlight);
        }
    }

    private void drawEmbossedDetails(Canvas canvas, RectF rect) {
        if ("silver_round_blue".equals(blisterType)) {
            // Embossed text on left
            canvas.save();
            canvas.rotate(-90, rect.left + dpToPx(14), rect.centerY());
            textPaint.setColor(Color.argb(120, 100, 116, 139));
            canvas.drawText("LB807 V03/28", rect.left + dpToPx(14) - dpToPx(26), rect.centerY() + dpToPx(3), textPaint);
            canvas.restore();
        } else if ("amber_round_yellow".equals(blisterType)) {
            // Embossed text on right
            canvas.save();
            canvas.rotate(90, rect.right - dpToPx(14), rect.centerY());
            textPaint.setColor(Color.argb(140, 120, 53, 15));
            canvas.drawText("10000 V11/27", rect.right - dpToPx(14) - dpToPx(24), rect.centerY() + dpToPx(3), textPaint);
            canvas.restore();
        }
    }

    private void drawCavitiesAndPills(Canvas canvas, RectF rect) {
        float leftPadding = ("silver_round_blue".equals(blisterType)) ? dpToPx(26) : dpToPx(16);
        float rightPadding = ("amber_round_yellow".equals(blisterType)) ? dpToPx(26) : dpToPx(16);
        float topPadding = dpToPx(14);
        float bottomPadding = dpToPx(14);

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

                boolean isFilled = slotStates[index];

                if ("silver_capsule_yellow".equals(blisterType)) {
                    drawCapsuleSlot(canvas, cx, cy, cellW * 0.46f, cellH * 0.76f, isFilled);
                } else {
                    float radius = Math.min(cellW, cellH) * 0.35f;
                    drawRoundSlot(canvas, cx, cy, radius, isFilled);
                }
            }
        }
    }

    private void drawRoundSlot(Canvas canvas, float cx, float cy, float radius, boolean isFilled) {
        float rimRadius = radius * 1.25f;

        // Rim
        if ("amber_round_yellow".equals(blisterType)) {
            cavityRimPaint.setColor(Color.argb(100, 124, 45, 18));
            canvas.drawCircle(cx, cy, rimRadius, cavityRimPaint);
        } else {
            cavityRimPaint.setColor(Color.argb(70, 100, 116, 139));
            canvas.drawCircle(cx, cy, rimRadius, cavityRimPaint);
        }

        if (!isFilled) {
            // Empty cavity (crushed/open blister pocket)
            if ("amber_round_yellow".equals(blisterType)) {
                cavityEmptyPaint.setColor(Color.rgb(180, 83, 9));
            } else {
                cavityEmptyPaint.setColor(Color.rgb(148, 163, 184));
            }
            canvas.drawCircle(cx, cy, radius, cavityEmptyPaint);

            // Inner dark pocket shadow
            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(110, 15, 23, 42));
            canvas.drawCircle(cx + dpToPx(1f), cy + dpToPx(1f), radius * 0.85f, shadowPaint);

            // Broken foil reflective highlights
            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(dpToPx(1.2f));
            highlightPaint.setColor(Color.argb(160, 255, 255, 255));
            tempRect.set(cx - radius * 0.7f, cy - radius * 0.7f, cx + radius * 0.7f, cy + radius * 0.7f);
            canvas.drawArc(tempRect, 210, 100, false, highlightPaint);
        } else {
            // Filled pill
            if ("silver_round_blue".equals(blisterType)) {
                // Vibrant blue round tablet (Folcres)
                pillPaint.setShader(new RadialGradient(
                        cx - radius * 0.25f, cy - radius * 0.35f, radius * 1.1f,
                        new int[]{Color.rgb(56, 189, 248), Color.rgb(2, 132, 199), Color.rgb(3, 105, 161)},
                        new float[]{0f, 0.6f, 1f},
                        Shader.TileMode.CLAMP
                ));
            } else {
                // Vibrant yellow round tablet (Minoxidil)
                pillPaint.setShader(new RadialGradient(
                        cx - radius * 0.25f, cy - radius * 0.35f, radius * 1.1f,
                        new int[]{Color.rgb(254, 240, 138), Color.rgb(234, 179, 8), Color.rgb(202, 138, 4)},
                        new float[]{0f, 0.65f, 1f},
                        Shader.TileMode.CLAMP
                ));
            }

            // Pill drop shadow
            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(80, 0, 0, 0));
            canvas.drawCircle(cx + dpToPx(1.5f), cy + dpToPx(2f), radius, shadowPaint);

            // Pill body
            canvas.drawCircle(cx, cy, radius, pillPaint);

            // Dome 3D glossy highlight
            highlightPaint.setStyle(Paint.Style.FILL);
            highlightPaint.setColor(Color.argb(170, 255, 255, 255));
            canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.35f, radius * 0.26f, highlightPaint);
        }
    }

    private void drawCapsuleSlot(Canvas canvas, float cx, float cy, float capW, float capH, boolean isFilled) {
        float r = capW / 2f;
        tempRect.set(cx - r, cy - capH / 2f, cx + r, cy + capH / 2f);

        // Cavity rim
        RectF rimRect = new RectF(tempRect);
        rimRect.inset(-dpToPx(2.5f), -dpToPx(2.5f));
        cavityRimPaint.setColor(Color.argb(70, 100, 116, 139));
        canvas.drawRoundRect(rimRect, r + dpToPx(2f), r + dpToPx(2f), cavityRimPaint);

        if (!isFilled) {
            // Empty capsule pocket
            cavityEmptyPaint.setColor(Color.rgb(148, 163, 184));
            canvas.drawRoundRect(tempRect, r, r, cavityEmptyPaint);

            // Shadow
            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(120, 15, 23, 42));
            RectF innerShadow = new RectF(tempRect);
            innerShadow.inset(dpToPx(2f), dpToPx(2f));
            canvas.drawRoundRect(innerShadow, r - dpToPx(1f), r - dpToPx(1f), shadowPaint);

            // Reflective edge
            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(dpToPx(1.2f));
            highlightPaint.setColor(Color.argb(160, 255, 255, 255));
            canvas.drawLine(tempRect.left + dpToPx(2f), tempRect.top + dpToPx(4f), tempRect.left + dpToPx(2f), tempRect.bottom - dpToPx(6f), highlightPaint);
        } else {
            // Filled Yellow Capsule (Vitamin D)
            // Drop shadow
            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setColor(Color.argb(90, 0, 0, 0));
            RectF shadowR = new RectF(tempRect);
            shadowR.offset(dpToPx(1.5f), dpToPx(2f));
            canvas.drawRoundRect(shadowR, r, r, shadowPaint);

            // Capsule body gradient
            pillPaint.setShader(new LinearGradient(
                    tempRect.left, tempRect.top, tempRect.right, tempRect.bottom,
                    new int[]{Color.rgb(253, 224, 71), Color.rgb(234, 179, 8), Color.rgb(202, 138, 4)},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRoundRect(tempRect, r, r, pillPaint);

            // Center dividing seam line
            Paint seamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            seamPaint.setStyle(Paint.Style.STROKE);
            seamPaint.setStrokeWidth(dpToPx(1.5f));
            seamPaint.setColor(Color.argb(140, 161, 98, 7));
            canvas.drawLine(tempRect.left, cy, tempRect.right, cy, seamPaint);

            // Capsule glossy white light reflection streak
            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(dpToPx(1.6f));
            highlightPaint.setColor(Color.argb(180, 255, 255, 255));
            canvas.drawLine(cx - r * 0.45f, tempRect.top + dpToPx(4f), cx - r * 0.45f, tempRect.bottom - dpToPx(6f), highlightPaint);
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
        float leftPadding = ("silver_round_blue".equals(blisterType)) ? dpToPx(26) : dpToPx(16);
        float rightPadding = ("amber_round_yellow".equals(blisterType)) ? dpToPx(26) : dpToPx(16);
        float topPadding = dpToPx(14);
        float bottomPadding = dpToPx(14);

        float gridLeft = blisterRect.left + leftPadding;
        float gridTop = blisterRect.top + topPadding;
        float gridWidth = blisterRect.width() - leftPadding - rightPadding;
        float gridHeight = blisterRect.height() - topPadding - bottomPadding;

        if (touchX < gridLeft || touchX > gridLeft + gridWidth ||
            touchY < gridTop || touchY > gridTop + gridHeight) {
            return -1;
        }

        float cellW = gridWidth / (float) cols;
        float cellH = gridHeight / (float) rows;

        int c = (int) ((touchX - gridLeft) / cellW);
        int r = (int) ((touchY - gridTop) / cellH);

        c = Math.max(0, Math.min(cols - 1, c));
        r = Math.max(0, Math.min(rows - 1, r));

        return r * cols + c;
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }
}
