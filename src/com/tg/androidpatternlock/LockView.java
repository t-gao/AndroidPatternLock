package com.tg.androidpatternlock;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public class LockView extends View {

    private static boolean DEBUG = true;

    private static final float CIRCLE_STROKE_WIDTH_DEFAULT = 4.0f;

    private static final int MSG_RESET = 0;
    private static final int MSG_SHOW_WRONG = 1;

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

    private boolean mTouchForbidden = false;

    private boolean mSizesInitialized = false;

    private ArrayList<Integer> mSelectedIndices = new ArrayList<Integer>();
    private SparseArray<Cell> mAllCells = new SparseArray<Cell>();
    // private boolean mAllCellsInitialized = false;

    private HashSet<Integer> mCorners;

    private boolean mIsThumbMode = false;

    private UiStyle mUiStyle = UiStyle.Circle;
    private WorkMode mWorkMode = WorkMode.Inputing;// default is Inputing
    private DisplayMode mDisplayMode = DisplayMode.Normal;
    private SkipPolicy mSkipPolicy = SkipPolicy.Allow;

    private boolean mPatternInProgress = false;

    private Paint mCirclePaint;
    private int mCircleColorNormal;
    private int mCircleColorCorrect;
    private int mCircleColorWrong;

    private final Path mCurrentPath = new Path();
    private Paint mPathPaint;
    private int mPathColorCorrect;
    private int mPathColorWrong;

    private Handler mHandler;

    private CreationHandler mCreationHandler;
    private InputHandler mInputHandler;

    private PatternListener mPatternListener;
    private PatternPasswordStorageFetcher mPatternFetcher;

    private CreationHandler.PatternCreatingListener mCreatingListener;

    private ComplexityChecker mComplexityChecker;

    private Bitmap mBitmapWrong;
    private Bitmap mBitmapCorrect;
    private Bitmap mBitmapUnselected;

    public LockView(Context context) {
        super(context);
        init(context);
    }

    public LockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mRowCenters = new float[MAX_ROWS];
        mColumnCenters = new float[MAX_COLUMNS];

        // TODO: get colors from attr
        mCircleColorNormal = context.getResources().getColor(
                R.color.circle_normal);
        mCircleColorCorrect = context.getResources().getColor(
                R.color.circle_selected);
        mCircleColorWrong = Color.RED;
        mPathColorCorrect = mCircleColorCorrect;
        mPathColorWrong = mCircleColorWrong;

        mCreationHandler = new CreationHandler();
        mCreatingListener = new CreationHandler.PatternCreatingListener() {

            @Override
            public void onInputOnce(ArrayList<Integer> pattern) {
                boolean complexityCheckPass = true;
                if (mComplexityChecker != null) {
                    complexityCheckPass = mComplexityChecker.check(pattern);
                }
                if (complexityCheckPass) {
                    sendResetMessageDelayed(500);
                } else {
                    mCreationHandler.reset();
                    sendMessageShowWrong();
                }
                if (mPatternListener != null) {
                    mPatternListener.onCreatingInputOnce(pattern, complexityCheckPass);
                }
            }

            @Override
            public void onComplete(boolean match, String encryptedPatternStr) {
                if (match) {
                    sendResetMessageDelayed(1000);
                } else {
                    sendMessageShowWrong();
                }

                if (mPatternListener != null) {
                    mPatternListener.onCreatingInputComplete(match,
                            encryptedPatternStr);
                }
            }
        };
        mCreationHandler.setPatternCreatingListener(mCreatingListener);

        mInputHandler = new InputHandler();

        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_RESET:
                    resetPattern();
                    break;
                case MSG_SHOW_WRONG:
                    drawWrong();
                    sendResetMessageDelayed(1000);
                    break;
                }
                super.handleMessage(msg);
            }
        };

        initCornerIndices();
    }

    /**
     *  TODO: this handles only the 3*3 case.
     */
    private void initCornerIndices() {
        mCorners = new HashSet<Integer>();
        mCorners.add(0);
        mCorners.add(2);
        mCorners.add(6);
        mCorners.add(8);
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

        for (int i = 0; i < MAX_ROWS; i++) {
            float rowY = mRowCenters[i];
            for (int j = 0; j < MAX_COLUMNS; j++) {
                float colunmX = mColumnCenters[j];
                int index = getIndexByRowAndColumn(i, j);
                mAllCells.put(index, new Cell(index, i, j, colunmX, rowY));
            }
        }

        int diameter = (int) mRadius;//(int) (2 * mRadius);//TODO: why are the images only partially drawn?
        if (diameter > 0) {
            try {
                mBitmapCorrect = scaleBitmap(mBitmapCorrect, diameter, diameter, true);
                mBitmapWrong = scaleBitmap(mBitmapWrong, diameter, diameter, true);
                mBitmapUnselected = scaleBitmap(mBitmapUnselected, diameter, diameter, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mSizesInitialized = true;
    }

    private void drawCircles(Canvas canvas) {
        if (DEBUG) {
            Log.d(LOG_TAG, "drawCircles");
        }
        // canvas.drawColor(Color.TRANSPARENT);
        // Iterator<Map.Entry<Integer, Cell>> iter = mAllCells.entrySet()
        // .iterator();
        // while (iter.hasNext()) {
        int size = mAllCells.size();
        for (int i = 0; i < size; i++) {
            // Map.Entry<Integer, Cell> entry = iter.next();
            // Cell c = entry.getValue();
            Cell c = mAllCells.valueAt(i);
            boolean selected = mSelectedIndices.contains(c.index);
            if (mDisplayMode != DisplayMode.Normal && selected) {
                boolean correct = mDisplayMode == DisplayMode.Correct;
                if (mUiStyle == UiStyle.Dot) {
                    drawDotSelected(canvas, c.centerX, c.centerY,
                            mRadius * 0.5f, correct);
                } else if (mUiStyle == UiStyle.Image) {
                    drawImage(canvas, c.centerX, c.centerY, true, correct);
                } else {
                    drawCircleSelected(canvas, c.centerX, c.centerY, mRadius,
                            0, correct);
                }
            } else {
                if (mUiStyle == UiStyle.Dot) {
                    drawDotNormal(canvas, c.centerX, c.centerY, mRadius * 0.5f);
                } else if (mUiStyle == UiStyle.Image) {
                    drawImage(canvas, c.centerX, c.centerY, false, false);
                } else {
                    drawCircleNormal(canvas, c.centerX, c.centerY, mRadius);
                }
            }
            if (DEBUG) {
                Log.d(LOG_TAG, "ROW: " + c.row + ", COLUMN: " + c.column
                        + "; x: " + c.centerX + ", y: " + c.centerY
                        + ", selected: " + selected);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // make the height be equal to the width
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        if (DEBUG) {
            Log.d(LOG_TAG, "onMeasure, set width and height: " + width);
        }
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onDraw");
        }
        initSizes();
        if (mPatternInProgress) {
            drawPath(canvas, mDisplayMode == DisplayMode.Correct);
        }
        drawCircles(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsThumbMode || mTouchForbidden) {
            return true;
        }

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
            resetPattern();
            return true;
        default:
            return super.onTouchEvent(event);
        }
    }

    /**
     * TODO: this handles only the 3*3 case.
     * @param last the last index in the selected indices array.
     * @param current the currently hit index.
     * @return -1 if no skip happens, otherwise the skipped index.
     */
    private Integer getSkippedIndex(Integer last, Integer current) {
        Integer skipped = -1;
        if ((mCorners.contains(last) && mCorners.contains(current))
                || (last == 1 && current == 7)
                || (last == 7 && current == 1)
                || (last == 3 && current == 5)
                || (last == 5 && current == 3)) {
            skipped = (last + current) / 2;
        }
        return skipped;
    }

    private int detectHitAndDraw(float eventX, float eventY) {
        mCurrentPath.rewind();

        boolean detected = false;
        int index = -1;
        int row = detectRow(eventY);
        if (row >= 0) {
            int column = detectColumn(eventX);
            if (column >= 0) {
                index = getIndexByRowAndColumn(row, column);
                if (index >= 0 && index < MAX_ROWS * MAX_COLUMNS
                        && !mSelectedIndices.contains(index)) {
                    boolean valid = true;
                    if (!mSelectedIndices.isEmpty()) {
                        Integer last = mSelectedIndices.get(mSelectedIndices.size() - 1);
                        Integer skipped = getSkippedIndex(last, index);
                        if (skipped >= 0) {// if skip happens
                            switch (mSkipPolicy) {
                                case Allow:
                                    break;
                                case AutoConnect:
                                    mSelectedIndices.add(skipped);
                                    break;
                                case Disallow:
                                    valid = false;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    if (valid) {
                        detected = true;
                        mSelectedIndices.add(index);
                        float centerX = mFirstX + column * mGridWidth;
                        float centerY = mFirstY + row * mGridWidth;

                        // if (!drawLine) {
                        invalidate((int) (centerX - mRadius) - 1,
                                (int) (centerY - mRadius) - 1,
                                (int) (centerX + mRadius) + 1,
                                (int) (centerY + mRadius) + 1);
                        // }
                    }
                }
            }
        }

        boolean drawLine = !mSelectedIndices.isEmpty();
        if (drawLine) {
            Cell c1 = mAllCells.get(mSelectedIndices.get(0));
            mCurrentPath.moveTo(c1.centerX, c1.centerY);
            int size = mSelectedIndices.size();
            for (int i = 1; i < size; i++) {
                Cell c = mAllCells.get(mSelectedIndices.get(i));
                mCurrentPath.lineTo(c.centerX, c.centerY);
            }
        }

        if (!detected && drawLine) {
            mCurrentPath.lineTo(eventX, eventY);
        }

        if (drawLine) {
            invalidate();
        }

        return index;
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
        final float x = event.getX();
        final float y = event.getY();
        detectHitAndDraw(x, y);
    }

    private void handleActionUp(MotionEvent event) {
        if (mPatternInProgress) {
            mTouchForbidden = true;
            if (mWorkMode == WorkMode.Creating) {
                mCreationHandler.completeInput(mSelectedIndices);
            } else if (mWorkMode == WorkMode.Inputing) {
                boolean correct = mInputHandler.check(mPatternFetcher,
                        mSelectedIndices);
                if (correct) {
                    sendResetMessageDelayed(300);
                } else {
                    sendMessageShowWrong();
                }
                if (mPatternListener != null) {
                    mPatternListener.onInputCheckResult(correct);
                }
            }
        }
    }

    private void sendMessageShowWrong() {
        mTouchForbidden = true;
        mHandler.sendEmptyMessage(MSG_SHOW_WRONG);
    }

    private void sendResetMessageDelayed(long delayMillis) {
        mTouchForbidden = true;
        mHandler.sendEmptyMessageDelayed(MSG_RESET, delayMillis);
    }

    private void resetPattern() {
        mSelectedIndices.clear();
        mIsThumbMode = false;
        mPatternInProgress = false;
        mDisplayMode = DisplayMode.Normal;
        mTouchForbidden = false;
        mCurrentPath.rewind();
        invalidate();
    }

    protected void drawWrong() {
        mDisplayMode = DisplayMode.Wrong;
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

    protected void drawPath(Canvas canvas, boolean correct) {
        if (mPathPaint == null) {
            mPathPaint = new Paint();
            mPathPaint.setStrokeWidth(getCircleStrokeWidth()/*mRadius * 0.25f*/);
            mPathPaint.setAntiAlias(true);
            mPathPaint.setDither(true);
            mPathPaint.setStyle(Style.STROKE);
            mPathPaint.setStrokeJoin(Paint.Join.ROUND);
            mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        }
        mPathPaint.setColor(correct ? mPathColorCorrect : mPathColorWrong);
        canvas.drawPath(mCurrentPath, mPathPaint);
    }

    protected void drawDot(Canvas canvas, float cx, float cy, float radius,
            int color) {
        if (mCirclePaint == null) {
            mCirclePaint = new Paint();
            mCirclePaint.setDither(true);
//            mCirclePaint.setStrokeWidth(getCircleStrokeWidth());
            mCirclePaint.setAntiAlias(true);
        }

        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(color);
        canvas.drawCircle(cx, cy, radius, mCirclePaint);
    }

    private void drawDotNormal(Canvas canvas, float cx, float cy, float radius) {
        drawDot(canvas, cx, cy, radius, mCircleColorNormal);
    }

    private void drawDotSelected(Canvas canvas, float cx, float cy,
            float radius, boolean correct) {
        drawDot(canvas, cx, cy, radius, correct ? mCircleColorCorrect
                : mCircleColorWrong);
    }

    protected void drawCircleNormal(Canvas canvas, float cx, float cy,
            float radius) {
        if (mCirclePaint == null) {
            mCirclePaint = new Paint();
            mCirclePaint.setDither(true);
            mCirclePaint.setStrokeWidth(getCircleStrokeWidth());
            mCirclePaint.setAntiAlias(true);
        }
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(mCircleColorNormal);
        canvas.drawCircle(cx, cy, radius, mCirclePaint);
    }

    protected void drawCircleSelected(Canvas canvas, float cx, float cy,
            float radius, float innerRadius, boolean correct) {
        if (mCirclePaint == null) {
            mCirclePaint = new Paint();
            mCirclePaint.setStrokeWidth(getCircleStrokeWidth());
            mCirclePaint.setDither(true);
            mCirclePaint.setAntiAlias(true);
        }
        mCirclePaint
                .setColor(correct ? mCircleColorCorrect : mCircleColorWrong);

        mCirclePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, radius, mCirclePaint);

        // TODO: hard code
        if (innerRadius <= 0 || innerRadius > 0.7f * radius) {
            innerRadius = 0.3f * radius;// default
        }
        mCirclePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, innerRadius, mCirclePaint);
    }

    private void drawImage(Canvas canvas, float cx, float cy, boolean selected,
            boolean correct) {
        Bitmap bitmap = null;
        if (selected) {
            if (correct) {
                bitmap = mBitmapCorrect;
            } else {
                bitmap = mBitmapWrong;
            }
        } else {
            bitmap = mBitmapUnselected;
        }
        Rect dst = new Rect((int)(cx - mRadius), (int)(cy - mRadius), (int)(cx + mRadius), (int)(cy + mRadius));
        canvas.drawBitmap(bitmap, new Rect(0,0,100,100), dst, null);
    }

    private float getGridSize() {
        float w = this.getWidth();
        float h = this.getHeight();
        float gridW = w / MAX_COLUMNS;
        float gridH = h / MAX_ROWS;

        return Math.min(gridW, gridH);
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

//    private int[] getRowAndColumnByIndex(int index) {
//        if (index < 0 || index >= MAX_ROWS * MAX_COLUMNS) {
//            return null;
//        }
//
//        int[] coords = new int[2];
//        coords[0] = index / MAX_COLUMNS;// row
//        coords[1] = index % MAX_COLUMNS;
//        return coords;
//    }

    public void setThumbMode(boolean isThumb) {
        resetPattern();
        mIsThumbMode = isThumb;
        mDisplayMode = DisplayMode.Correct;
    }

    public void setThumbPattern(ArrayList<Integer> pattern) {
        if (pattern == null) {
            mSelectedIndices.clear();
        } else {
            mSelectedIndices = pattern;
        }
        invalidate();
    }

    public void setUiStyle(UiStyle uiStyle) {
        mUiStyle = uiStyle;
    }

    public void setWorkMode(WorkMode workMode) {
        mWorkMode = workMode;
        if (workMode == WorkMode.Creating) {
            mCreationHandler.reset();
        } else if (workMode == WorkMode.Inputing) {
            mInputHandler.reset();
        }

        resetPattern();
    }

    public void setSkipPolicy(SkipPolicy skipPolicy) {
        mSkipPolicy = skipPolicy;
    }

    public void setCircleColorNormal(int circleColorNormal) {
        this.mCircleColorNormal = circleColorNormal;
    }

    public void setCircleColorCorrect(int circleColorCorrect) {
        this.mCircleColorCorrect = circleColorCorrect;
    }

    public void setCircleColorWrong(int circleColorWrong) {
        this.mCircleColorWrong = circleColorWrong;
    }

    public void setPathColorCorrect(int pathColorCorrect) {
        this.mPathColorCorrect = pathColorCorrect;
    }

    public void setPathColorWrong(int pathColorWrong) {
        this.mPathColorWrong = pathColorWrong;
    }

    /**
     * @param bms as in the order: correct, wrong, unselected
     */
    public void setImageBitmaps(Bitmap... bms) {
        if (bms != null) {
            int len = bms.length;
            if (len > 2) {
                mBitmapCorrect = bms[0];
                mBitmapWrong = bms[1];
                mBitmapUnselected = bms[2];
            } else if (len > 1) {
                mBitmapCorrect = bms[0];
                mBitmapWrong = bms[1];
            } else if (len > 0) {
                mBitmapCorrect = bms[0];
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        if (src == null) {
            return null;
        } else {
            return Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter);
        }
    }

    /**
     * Sets the pattern password fetcher to the LockView. Users of the LockView
     * must call this so that the stored pattern can be fetched and compared to
     * the one input by user.
     * 
     * @param fetcher
     */
    public void setPatternPasswordStorageFetcher(
            PatternPasswordStorageFetcher fetcher) {
        mPatternFetcher = fetcher;
    }

    public static abstract class PatternPasswordStorageFetcher {
        public abstract ArrayList<Integer> fetch();

        public abstract String fetchEncrypted();
    }

    public void setPatternListener(PatternListener listener) {
        mPatternListener = listener;
    }

    public interface PatternListener {
        public void onCreatingInputOnce(ArrayList<Integer> pattern,
                boolean complexityCheckPass);

        public void onCreatingInputComplete(boolean match,
                String encryptedPatternStr);

        public void onInputCheckResult(boolean correct);
    }

    public void setComplexityChecker(ComplexityChecker checker) {
        mComplexityChecker = checker;
    }

    public static abstract class ComplexityChecker {
        public boolean check(ArrayList<Integer> pattern) {
            return true;
        }
    }

    public static enum WorkMode {
        Creating, Inputing
    }

    public static enum UiStyle {
        Circle, Dot, Image
    }

    public static enum SkipPolicy {
        Allow, AutoConnect, Disallow
    }

    private class Cell {
        Cell(int index, int row, int column, float centerX, float centerY) {
            this.index = index;
            this.row = row;
            this.column = column;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        int index;
        int row;
        int column;
        float centerX;
        float centerY;
    }

    private enum DisplayMode {
        Normal, Correct, Wrong
    }
}
