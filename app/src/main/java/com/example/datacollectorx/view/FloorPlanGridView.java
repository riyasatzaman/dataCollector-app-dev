package com.example.datacollectorx.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.datacollectorx.R;

import java.util.HashSet;
import java.util.Set;

public class FloorPlanGridView extends View {

    private Bitmap floorPlanBitmap;
    private Matrix matrix = new Matrix();
    private Matrix inverseMatrix = new Matrix();
    private Paint gridPaint = new Paint();
    private Paint highlightPaint = new Paint();
    private Paint recordedPaint = new Paint();
    private Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    // Map Metadata (Defaults, will be updated by bitmap size)
    private float imageWidthPx = 1102f;
    private float imageHeightPx = 1716f;
    private float widthFeet = 354f;
    private float heightFeet = 106f;
    private float squareSizeMeters = 3.0f;
    private float ftToM = 0.3048f;

    private float widthMeters;
    private float heightMeters;
    private int columns;
    private int rows;

    private int selectedIndex = -1;
    private float selectedXm = -1f;
    private float selectedYm = -1f;

    private Set<Integer> recordedSquares = new HashSet<>();

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    public interface OnSquareSelectedListener {
        void onSquareSelected(int index, float xm, float ym);
    }

    private OnSquareSelectedListener listener;

    public FloorPlanGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Try to load floor_plan, fallback to cab_2nd_floor if not found
        int resId = getResources().getIdentifier("floor_plan", "drawable", context.getPackageName());
        if (resId == 0) resId = R.drawable.cab_2nd_floor;
        
        floorPlanBitmap = BitmapFactory.decodeResource(getResources(), resId);

        if (floorPlanBitmap != null) {
            imageWidthPx = floorPlanBitmap.getWidth();
            imageHeightPx = floorPlanBitmap.getHeight();
        }

        widthMeters = widthFeet * ftToM;
        heightMeters = heightFeet * ftToM;

        // Recalculate columns and rows to ensure total coverage
        columns = (int) Math.ceil(widthMeters / squareSizeMeters);
        rows = (int) Math.ceil(heightMeters / squareSizeMeters);

        // Grid Paint Settings
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0); 
        gridPaint.setAlpha(180);

        highlightPaint.setColor(Color.YELLOW); // Selection in yellow
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setAlpha(128);

        recordedPaint.setColor(Color.RED); // Recorded in red
        recordedPaint.setStyle(Paint.Style.FILL);
        recordedPaint.setAlpha(160);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setOnSquareSelectedListener(OnSquareSelectedListener listener) {
        this.listener = listener;
    }

    public void setRecordedSquares(Set<Integer> recordedSquares) {
        this.recordedSquares = recordedSquares;
        invalidate();
    }

    public void addRecordedSquare(int index) {
        this.recordedSquares.add(index);
        if (selectedIndex == index) {
            selectedIndex = -1; // Deselect if it was just recorded
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (floorPlanBitmap == null) return;

        canvas.save();
        canvas.concat(matrix);

        // Draw the map
        canvas.drawBitmap(floorPlanBitmap, 0, 0, bitmapPaint);

        // Draw the grid
        drawGrid(canvas);

        // Draw recorded squares
        for (Integer index : recordedSquares) {
            drawSquare(canvas, index, recordedPaint);
        }

        // Draw the selection highlight
        if (selectedIndex != -1) {
            drawSquare(canvas, selectedIndex, highlightPaint);
        }

        canvas.restore();
    }

    private void drawGrid(Canvas canvas) {
        if (widthMeters <= 0 || heightMeters <= 0) return;

        float pxPerMeterW = imageWidthPx / widthMeters;
        float pxPerMeterH = imageHeightPx / heightMeters;
        float squareSizePxW = squareSizeMeters * pxPerMeterW;
        float squareSizePxH = squareSizeMeters * pxPerMeterH;

        for (int i = 0; i <= columns; i++) {
            float x = i * squareSizePxW;
            canvas.drawLine(x, 0, x, imageHeightPx, gridPaint);
        }
        for (int j = 0; j <= rows; j++) {
            float y = j * squareSizePxH;
            canvas.drawLine(0, y, imageWidthPx, y, gridPaint);
        }
    }

    private void drawSquare(Canvas canvas, int index, Paint paint) {
        if (index <= 0 || columns <= 0) return;
        
        int row = (index - 1) / columns;
        int col = (index - 1) % columns;

        float pxPerMeterW = imageWidthPx / widthMeters;
        float pxPerMeterH = imageHeightPx / heightMeters;
        float squareSizePxW = squareSizeMeters * pxPerMeterW;
        float squareSizePxH = squareSizeMeters * pxPerMeterH;

        float left = col * squareSizePxW;
        float top = row * squareSizePxH;
        canvas.drawRect(left, top, left + squareSizePxW, top + squareSizePxH, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = scaleGestureDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;
        return handled || super.onTouchEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            matrix.postTranslate(-distanceX, -distanceY);
            invalidate();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            float[] pts = new float[]{e.getX(), e.getY()};
            matrix.invert(inverseMatrix);
            inverseMatrix.mapPoints(pts);

            float xPx = pts[0];
            float yPx = pts[1];

            if (xPx >= 0 && xPx <= imageWidthPx && yPx >= 0 && yPx <= imageHeightPx) {
                float xm = (xPx / imageWidthPx) * widthMeters;
                float ym = (yPx / imageHeightPx) * heightMeters;

                int col = (int) (xm / squareSizeMeters);
                int row = (int) (ym / squareSizeMeters);

                if (col >= 0 && col < columns && row >= 0 && row < rows) {
                    int index = (row * columns) + col + 1;
                    
                    // Disable re-recording: check if already recorded
                    if (recordedSquares.contains(index)) {
                        return true; 
                    }

                    selectedIndex = index;
                    selectedXm = (col + 0.5f) * squareSizeMeters;
                    selectedYm = (row + 0.5f) * squareSizeMeters;

                    if (listener != null) {
                        listener.onSquareSelected(selectedIndex, selectedXm, selectedYm);
                    }
                    invalidate();
                }
            }
            return true;
        }
    }
}
