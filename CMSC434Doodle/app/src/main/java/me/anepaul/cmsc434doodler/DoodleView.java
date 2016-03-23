package me.anepaul.cmsc434doodler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Deque;
import java.util.LinkedList;

/**
 * TODO: document your custom view class.
 */
public class DoodleView extends View {
    public final String TAG = getClass().getSimpleName();

    private boolean mTimeJumping = false;

    private Bitmap mDoodle;
    private Canvas mDoodleCanvas;
    private Paint mDoodlePaint;
    private Path mDoodlePath;

    private int mDoodleColor = Color.RED;
    private float mDoodleWidth = 5;
    private int mDoodleSmoothFactor = 10;
    private int mDoodleOpacity = 1;

    private Deque<DoodleStroke> mStrokes;
    private Deque<DoodleStroke> mRedoStrokes;

    public DoodleView(Context context) {
        super(context);
        init(null, 0);
    }

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public DoodleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setDrawingCacheEnabled(true);
        setDrawingCacheQuality(DRAWING_CACHE_QUALITY_HIGH);

        // Load attributes
        mDoodlePaint = new Paint();
        mDoodlePaint.setColor(mDoodleColor);
        mDoodlePaint.setStrokeWidth(mDoodleWidth);       // set the size
        mDoodlePaint.setDither(true);                    // set the dither to true
        mDoodlePaint.setStyle(Paint.Style.STROKE);       // set to STROKE
        mDoodlePaint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        mDoodlePaint.setStrokeCap(Paint.Cap.BUTT);      // set the paint cap to round too
        mDoodlePaint.setPathEffect(
                new CornerPathEffect(mDoodleSmoothFactor)); // set the path effect when they join.
        mDoodlePaint.setAntiAlias(true);    // set anti alias so it smooths

        mDoodlePath = new Path();

        mRedoStrokes = new LinkedList<>();

        mStrokes = new LinkedList<>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mStrokes.size() > 8) {
            DoodleStroke strk = mStrokes.pollFirst();
            mDoodleCanvas.drawPath(strk.doodlePath, strk.doodlePaint);
        }

        canvas.drawBitmap(mDoodle, 1, 1, null);

        drawOnCanvas(canvas);
    }

    private void drawOnCanvas(Canvas canvas) {
        // Draw undo-able strokes
        for (DoodleStroke stroke : mStrokes) {
            canvas.drawPath(stroke.doodlePath, stroke.doodlePaint);
        }

        if (mDoodlePath != null) {
            // Draw stroke in process
            canvas.drawPath(mDoodlePath, mDoodlePaint);
        }
    }

    public Bitmap getBitmap() {
        Bitmap bmp = Bitmap.createBitmap(mDoodle);
        Canvas canvas = new Canvas(bmp);
        drawOnCanvas(canvas);
        return bmp;
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mDoodle = Bitmap.createBitmap(w,h, Bitmap.Config.RGB_565);
        mDoodleCanvas = new Canvas(mDoodle);
        mDoodleCanvas.drawColor(Color.WHITE);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDoodlePath = new Path();
                lastPath().moveTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() < 2 && lastPath() != null) {
                    if (mTimeJumping) {
                        mRedoStrokes.clear();
                        mTimeJumping = false;
                    }
                    lastPath().lineTo(touchX, touchY);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDoodlePath != null) {
                    mStrokes.offerLast(new DoodleStroke(mDoodlePath, mDoodlePaint));
                    mDoodlePath = null;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() < 3) {
                    mDoodlePath = null;
                    doUndo();
                    invalidate();
                } else {
                    doRedo();
                }
                break;
        }

        return true;
    }

    public int doUndo() {
        if (mStrokes.peekLast() != null) {
            mRedoStrokes.offerFirst(mStrokes.pollLast());
            invalidate();
        }
        return mStrokes.size();
    }

    public int doRedo() {
        if (mRedoStrokes.peekFirst() != null) {
            mStrokes.offerLast(mRedoStrokes.pollFirst());
            invalidate();
        }
        return mRedoStrokes.size();
    }

    // Helper Methods and classes
    private class DoodleStroke {
        Path doodlePath;
        Paint doodlePaint;

        DoodleStroke(Path path, Paint paint) {
            doodlePath = path;
            doodlePaint = new Paint(paint);
        }
    }



    private Path lastPath() {
        return mDoodlePath;
    }

    public int getDoodleColor() {
        return mDoodleColor;
    }

    public void setDoodleColor(int mDoodleColor) {
        this.mDoodleColor = mDoodleColor;
        mDoodlePaint.setColor(mDoodleColor);
    }

    public float getDoodleWidth() {
        return mDoodleWidth;
    }

    public void setDoodleWidth(float mDoodleWidth) {
        this.mDoodleWidth = mDoodleWidth;
        mDoodlePaint.setStrokeWidth(mDoodleWidth);
    }

    public int getDoodleSmoothFactor() {
        return mDoodleSmoothFactor;
    }

    public void setDoodleSmoothFactor(int mDoodleSmoothFactor) {
        this.mDoodleSmoothFactor = mDoodleSmoothFactor;
        mDoodlePaint.setPathEffect(new CornerPathEffect(mDoodleSmoothFactor));
    }

    public int getDoodleOpacity() {
        return mDoodleOpacity;
    }

    public void setDoodleOpacity(int mDoodleOpacity) {
        this.mDoodleOpacity = mDoodleOpacity;
        mDoodlePaint.setAlpha(mDoodleOpacity);
    }
}
