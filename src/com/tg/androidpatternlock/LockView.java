package com.tg.androidpatternlock;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class LockView extends View {

    private static boolean DEBUG = true;

    private static final float CIRCLE_STROKE_WIDTH_DEFAULT = 4.0f;

    private static final int MAX_COLUMNS = 3;
    private static final int MAX_ROWS = 3;

    private static final String LOG_TAG = "LockView";

    private float mDiameterFactor = 0.5f;// TODO: read from attr

    private float mGridWidth;
    private float mRadius;

    private float mFirstX;// The x of the center of the top left circle
    private float mFirstY;

    private float[] mRowCenters;// y
    private float[] mColumnCenters;// x

    private boolean mSizesInitialized = false;

    private ArrayList<Integer> mSelectedIndices = new ArrayList<Integer>();

    private DisplayMode mDisplayMode = DisplayMode.Normal;

    private boolean mPatternInProgress = false;

    public LockView(Context context) {
        super(context);
        init();
    }

    public LockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mRowCenters = new float[MAX_ROWS];
        mColumnCenters = new float[MAX_COLUMNS];
    }

    private void initSizes() {
        if (mSizesInitialized) {
            return;
        }

        if (DEBUG) {
            Log.d(LOG_TAG, "initSizes");
        }

        mGridWidth = getGridSize();
        mRadius = mGridWidth * mDiameterFactor * 0.5f;
        mFirstX = 0.5f * mGridWidth;
        mFirstY = mFirstX;
        if (DEBUG) {
            Log.d(LOG_TAG, "mGridWidth: " + mGridWidth + ", mRadius: "
                    + mRadius + ", mFirstX: " + mFirstX + ", mFirstY: "
                    + mFirstY);
        }

        for (int i = 0; i < MAX_ROWS; i++) {
            mRowCenters[i] = mFirstY + i * mGridWidth;
        }
        for (int i = 0; i < MAX_COLUMNS; i++) {
            mColumnCenters[i] = mFirstX + i * mGridWidth;
        }

        mSizesInitialized = true;
    }

    private void drawCircles(Canvas canvas) {
        if (DEBUG) {
            Log.d(LOG_TAG, "drawCircles");
        }
        canvas.drawColor(Color.TRANSPARENT);
        for (int i = 0; i < MAX_ROWS; i++) {
            float rowY = mRowCenters[i];
            for (int j = 0; j < MAX_COLUMNS; j++) {
                float colunmX = mColumnCenters[j];
                int index = getIndexByRowAndColumn(i, j);
                boolean selected = mSelectedIndices.contains(index);
                if (mDisplayMode != DisplayMode.Normal && selected) {
                    drawCircleSelected(canvas, colunmX, rowY, mRadius, 0, true);
                } else {
                    drawCircleNormal(canvas, colunmX, rowY, mRadius);
                }
                if (DEBUG) {
                    Log.d(LOG_TAG, "ROW: " + i + ", COLUMN: " + j + "; x: "
                            + colunmX + ", y: " + rowY + ", selected: "
                            + selected);
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onDraw");
        }
        initSizes();
        drawCircles(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            handleActionDown(event);
            return true;
        case MotionEvent.ACTION_UP:
            handleActionUp(event);
            return true;
        case MotionEvent.ACTION_MOVE:
            handleActionMove(event);
            return true;
        case MotionEvent.ACTION_CANCEL:
            return true;
        }
        return super.onTouchEvent(event);
    }

    private int detectHitAndDraw(float eventX, float eventY) {
        int row = detectRow(eventY);
        if (row >= 0) {
            int column = detectColumn(eventX);
            if (column >= 0) {
                int index = getIndexByRowAndColumn(row, column);
                if (!mSelectedIndices.contains(index)) {
                    mSelectedIndices.add(index);
                    float centerX = mFirstX + column * mGridWidth;
                    float centerY = mFirstY + row * mGridWidth;

                    invalidate((int) (centerX - mRadius) - 1,
                            (int) (centerY - mRadius) - 1,
                            (int) (centerX + mRadius) + 1,
                            (int) (centerY + mRadius) + 1);

                    return index;
                }
            }
        }

        return -1;
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        final float x = event.getX();
        final float y = event.getY();
        int hitIndex = detectHitAndDraw(x, y);
        if (hitIndex >= 0 && hitIndex < MAX_COLUMNS * MAX_ROWS) {
            mPatternInProgress = true;
            mDisplayMode = DisplayMode.Correct;
        }
    }

    private void handleActionMove(MotionEvent event) {
        // TODO: draw path
        final float x = event.getX();
        final float y = event.getY();
        detectHitAndDraw(x, y);
    }

    private void handleActionUp(MotionEvent event) {
        // TODO
    }

    private void resetPattern() {
        mSelectedIndices.clear();
        mPatternInProgress = false;
        mDisplayMode = DisplayMode.Normal;
        invalidate();
    }

    private int detectRow(float y) {
        int i = 0;
        int row = -1;
        while (i < MAX_ROWS) {
            float topLimit = mFirstY + (i * mGridWidth) - mRadius;
            float bottomLimit = topLimit + 2 * mRadius;
            if (y > topLimit && y < bottomLimit) {
                row = i;
                break;
            }
            i++;
        }
        if (DEBUG) {
            Log.d(LOG_TAG, "detectRow, input y: " + y + ", detected row: "
                    + row);
        }
        return row;
    }

    private int detectColumn(float x) {
        int i = 0;
        int column = -1;
        while (i < MAX_ROWS) {
            float leftLimit = mFirstX + (i * mGridWidth) - mRadius;
            float rightLimit = leftLimit + 2 * mRadius;
            if (x > leftLimit && x < rightLimit) {
                column = i;
                break;
            }
            i++;
        }
        if (DEBUG) {
            Log.d(LOG_TAG, "detectColumn, input x: " + x
                    + ", detected column: " + column);
        }
        return column;
    }

    protected void drawCircleNormal(Canvas canvas, float cx, float cy,
            float radius) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(getCircleStrokeWidth());
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, radius, paint);
    }

    protected void drawCircleSelected(Canvas canvas, float cx, float cy,
            float radius, float innerRadius, boolean correct) {
        Paint paint = new Paint();
        paint.setColor(correct ? Color.BLUE : Color.RED);
        paint.setStrokeWidth(getCircleStrokeWidth());
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, radius, paint);

        // TODO: hard code
        if (innerRadius <= 0 || innerRadius > 0.7f * radius) {
            innerRadius = 0.3f * radius;// default
        }
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, innerRadius, paint);
    }

    private float getGridSize() {
        float w = this.getWidth();
        float h = this.getHeight();
        float cellW = w / MAX_COLUMNS;
        float cellH = h / MAX_ROWS;

        return Math.min(cellW, cellH);
    }

    protected static float getCircleStrokeWidth() {
        return CIRCLE_STROKE_WIDTH_DEFAULT;// TODO
    }

    private int getIndexByRowAndColumn(int row, int column) {
        if (row < 0 || row >= MAX_ROWS || column < 0 || column >= MAX_COLUMNS) {
            return -1;
        }

        return row * MAX_COLUMNS + column;
    }

    private int[] getRowAndColumnByIndex(int index) {
        if (index < 0 || index >= MAX_ROWS * MAX_COLUMNS) {
            return null;
        }

        int[] coords = new int[2];
        coords[0] = index / MAX_COLUMNS;// row
        coords[1] = index % MAX_COLUMNS;
        return coords;
    }

    private enum DisplayMode {
        Normal, Correct, Wrong
    }
}
