package com.yugensoft.simplesleepjournal.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.yugensoft.simplesleepjournal.HumanReadableConverter;
import com.yugensoft.simplesleepjournal.R;

/**
 * View which shows a comparison of a sleep target and sleep record
 */
public class SleepComparisonBar extends View {

    private long mTimeDifference = 0; // Time difference in milliseconds

    private RectF mBounds = new RectF();
    private RectF mHorizontalBar = new RectF();
    private RectF mHorizontalBarBorder = new RectF();
    private RectF mCenterBar = new RectF();
    private RectF mMovingBar = new RectF();

    private Paint mHorizontalBarPaint;
    private Paint mHorizontalBarBorderPaint;
    private Paint mCenterBarPaint;
    private Paint mMovingBarPaint;
    private Paint mTextPaint;
    private Shader mHorizontalBarShader;

    private float mHorizontalBarThickness;
    private int mTextPos;
    private float mTextToBarPadding;
    private String mTextLine1 = "";
    private float mTextLine1X;
    private float mTextLine1Y;
    private String mTextLine2 = "";
    private float mTextLine2X;
    private float mTextLine2Y;
    private float mTextWidth = 0.0f;
    private float mExpectedMaximumTextWidth = 0.0f;
    private float mTextHeight = 0.0f;
    private int mHorizontalBarColor;
    private int mHorizontalBarBorderColor;
    private float mHorizontalBarBorderThickness;
    private int mTextColor;

     // Draw text to the left of the bar
    public static final int TEXTPOS_LEFT = 0;
     // Draw text to the right of the bar
    public static final int TEXTPOS_RIGHT = 1;

    // Geometry constants
    private static final float LINE_SPACING = 1.2f; // Line spacing, multiple of line height
    private static final float CENTER_BAR_WIDTH_FACTOR = 0.2f;
    private static final float CENTER_BAR_HEIGHT_FACTOR = 1.5f;
    private static final float MOVING_BAR_WIDTH_FACTOR = 0.1f;
    private static final float MOVING_BAR_HEIGHT_FACTOR = 2f;
    private static final float TIME_DIFFERENCE_BOUNDARY = 4f;
    // Color constants
    private static final int SHADER_START_COLOR = 0xFF000000 + 0x5CDD5C;
    private static final int SHADER_CENTER_COLOR = 0XFFFFFFFF;
    private static final int SHADER_END_COLOR= 0xFF000000 + 0xFF6B6B;

    // Class constructor taking only a context.
    public SleepComparisonBar(Context context) {
        super(context);
        init();
    }

    /**
     * Class constructor taking a context and an attribute set. This constructor
     * is used by the layout engine to construct a SleepComparisonBar from a set of
     * XML attributes.
     */
    public SleepComparisonBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SleepComparisonBar,
                0, 0);
        try {
            mTextWidth = a.getDimension(R.styleable.SleepComparisonBar_labelWidth, 0.0f);
            mTextHeight = a.getDimension(R.styleable.SleepComparisonBar_labelHeight, 0.0f);
            mTextPos = a.getInteger(R.styleable.SleepComparisonBar_labelPosition, TEXTPOS_RIGHT);
            mTextColor = a.getColor(R.styleable.SleepComparisonBar_labelColor, Color.BLACK);
            mHorizontalBarColor = a.getColor(R.styleable.SleepComparisonBar_horizontalBarColor, Color.BLACK);
            mHorizontalBarBorderColor = a.getColor(R.styleable.SleepComparisonBar_horizontalBarBorderColor, Color.BLACK);
            mHorizontalBarBorderThickness = a.getDimension(R.styleable.SleepComparisonBar_horizontalBarBorder, 0);
            mHorizontalBarThickness = a.getDimension(R.styleable.SleepComparisonBar_horizontalBarThickness, 0.0f);
            mTextToBarPadding = a.getDimension(R.styleable.SleepComparisonBar_textToBarPadding, 0);

        } finally {
            a.recycle();
        }

        init();
    }

    // Setters and getters
    public float getBarThickness() {
        return mHorizontalBarThickness;
    }
    public void setBarThickness(float mBarThickness) {
        this.mHorizontalBarThickness = mBarThickness;
        onDataChanged();
        invalidate();
    }
    public float getTextWidth() {
        return mTextWidth;
    }
    public void setTextWidth(float textWidth) {
        mTextWidth = textWidth;
        invalidate();
    }
    public float getTextHeight() {
        return mTextHeight;
    }
    public void setTextHeight(float textHeight) {
        mTextHeight = textHeight;
        invalidate();
    }
    public int getTextPos() {
        return mTextPos;
    }
    public void setTextPos(int textPos) {
        if (textPos != TEXTPOS_LEFT && textPos != TEXTPOS_RIGHT) {
            throw new IllegalArgumentException(
                    "TextPos must be one of TEXTPOS_LEFT or TEXTPOS_RIGHT");
        }
        mTextPos = textPos;
        onDataChanged();
        invalidate();
    }
    public float getTimeDifference() {
        return mTimeDifference;
    }
    public void setTimeDifference(long milliseconds) {
        this.mTimeDifference = milliseconds;
        String text = new HumanReadableConverter(getContext()).MillisecondsToStandard(milliseconds, false);
        this.mTextLine1 = text.replace("-", "");
        if(milliseconds >= 0) {
            this.mTextLine2 = getContext().getString(R.string.late);
        } else {
            this.mTextLine2 = getContext().getString(R.string.early);
        }
        onDataChanged();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(mHorizontalBarBorder, mHorizontalBarBorderPaint);
        canvas.drawRect(mHorizontalBar, mHorizontalBarPaint);
        canvas.drawRect(mCenterBar, mCenterBarPaint);
        canvas.drawRect(mMovingBar, mMovingBarPaint);
        canvas.drawText(mTextLine1, mTextLine1X, mTextLine1Y, mTextPaint);
        canvas.drawText(mTextLine2, mTextLine2X, mTextLine2Y, mTextPaint);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Set dimensions for text, bars, etc
        //
        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());
        float ww = (float) w - xpad;
        float hh = (float) h - ypad;

        // Create a bounding rectangle for positioning other elements
        mBounds = new RectF(0, 0, ww, hh);
        mBounds.offsetTo(getPaddingLeft(), getPaddingTop());

        // Regenerate view
        onDataChanged();
    }

    private void onDataChanged() {
        // Determine text dimensions
        mTextHeight = mHorizontalBarThickness / 2;
        mTextPaint.setTextSize(mTextHeight);
        mTextWidth = Math.max(mTextPaint.measureText(mTextLine1), mTextPaint.measureText(mTextLine2));
        mTextWidth = Math.max(mTextWidth, mExpectedMaximumTextWidth); // Stops jumpy rendering and different bar sizes

        // Determine moving bar dimensions
        float movingBarWidth = mHorizontalBarThickness * MOVING_BAR_WIDTH_FACTOR;
        float movingBarHeight = mHorizontalBarThickness * MOVING_BAR_HEIGHT_FACTOR;

        // Make adjustments based on text position
        float horizontalBarWidth = mBounds.width() - (mTextWidth + mTextToBarPadding);
        if (mTextPos == TEXTPOS_LEFT) {
            mTextPaint.setTextAlign(Paint.Align.RIGHT);
            mHorizontalBarBorder = new RectF(
                    mBounds.left + mTextWidth + mTextToBarPadding + movingBarWidth/2,
                    mBounds.centerY() - mHorizontalBarThickness /2,
                    mBounds.left + mTextWidth + mTextToBarPadding + horizontalBarWidth - movingBarWidth/2,
                    mBounds.centerY() + mHorizontalBarThickness /2
            );
            mTextLine1X = mHorizontalBarBorder.left - mTextToBarPadding;
            mTextLine2X = mTextLine1X;
        } else {
            mTextPaint.setTextAlign(Paint.Align.LEFT);

            mHorizontalBarBorder = new RectF(
                    mBounds.left + movingBarWidth/2,
                    mBounds.centerY() - mHorizontalBarThickness /2,
                    horizontalBarWidth - movingBarWidth/2,
                    mBounds.centerY() + mHorizontalBarThickness /2
            );
            mTextLine1X = mHorizontalBarBorder.right + mTextToBarPadding;
            mTextLine2X = mTextLine1X;
        }
        mTextLine2Y = mHorizontalBarBorder.bottom;
        mTextLine1Y = mHorizontalBarBorder.bottom - (mTextHeight * LINE_SPACING);

        // Create the inner non-border bar
        mHorizontalBar = new RectF(
                mHorizontalBarBorder.left + mHorizontalBarBorderThickness,
                mHorizontalBarBorder.top + mHorizontalBarBorderThickness,
                mHorizontalBarBorder.right - mHorizontalBarBorderThickness,
                mHorizontalBarBorder.bottom - mHorizontalBarBorderThickness
        );
        mHorizontalBarShader = new LinearGradient(
                mHorizontalBarBorder.left + mHorizontalBarBorderThickness,
                0,
                mHorizontalBarBorder.right - mHorizontalBarBorderThickness,
                0,
                new int[] {SHADER_START_COLOR, SHADER_CENTER_COLOR, SHADER_END_COLOR},
                new float[] {0, 0.5f, 1},
                Shader.TileMode.CLAMP
        );
        mHorizontalBarPaint.setShader(mHorizontalBarShader);

        // Place the Center Bar
        float centerBarWidth = mHorizontalBarThickness * CENTER_BAR_WIDTH_FACTOR;
        float centerBarHeight = mHorizontalBarThickness * CENTER_BAR_HEIGHT_FACTOR;
        mCenterBar = new RectF(
                mHorizontalBarBorder.centerX() - centerBarWidth/2,
                mHorizontalBarBorder.centerY() - centerBarHeight/2,
                mHorizontalBarBorder.centerX() + centerBarWidth/2,
                mHorizontalBarBorder.centerY() + centerBarHeight/2
        );

        // Place the Moving Bar
        float timeDiff = (float)mTimeDifference / (60*60*1000);
        if(timeDiff > TIME_DIFFERENCE_BOUNDARY) {
            timeDiff = TIME_DIFFERENCE_BOUNDARY;
            mMovingBarPaint.setColor(Color.RED);
        } else if(timeDiff < -TIME_DIFFERENCE_BOUNDARY) {
            timeDiff = -TIME_DIFFERENCE_BOUNDARY;
            mMovingBarPaint.setColor(Color.RED);
        } else {
            mMovingBarPaint.setColor(Color.BLACK);
        }
        float movingBarCenterX = mHorizontalBarBorder.centerX() + (timeDiff / TIME_DIFFERENCE_BOUNDARY) * (mHorizontalBarBorder.width()/2);
        float movingBarCenterY = mHorizontalBarBorder.centerY();
        mMovingBar = new RectF(
                movingBarCenterX - movingBarWidth/2,
                movingBarCenterY - movingBarHeight/2,
                movingBarCenterX + movingBarWidth/2,
                movingBarCenterY + movingBarHeight/2
        );
    }

    //
    // Measurement functions.
    //
    @Override
    protected int getSuggestedMinimumWidth() {
        return (int) (mHorizontalBarThickness * 5);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return (int) (mHorizontalBarThickness * MOVING_BAR_HEIGHT_FACTOR);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        int minh = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight();
        int h = Math.min(MeasureSpec.getSize(heightMeasureSpec), minh);

        setMeasuredDimension(w, h);
    }

    /**
     * Initialize the control. This code is in a separate method so that it can be
     * called from both constructors.
     */
    private void init() {
        // Set up the paint for the label text
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextHeight = mHorizontalBarThickness / 2;
        if (mTextHeight == 0) {
            mTextHeight = mTextPaint.getTextSize();
        } else {
            mTextPaint.setTextSize(mTextHeight);
        }
        mExpectedMaximumTextWidth = Math.max(
                mTextPaint.measureText(getResources().getString(R.string.early)),
                mTextPaint.measureText(getResources().getString(R.string.late))
        );

        // Set up the paints for the bars
        mCenterBarPaint = new Paint(0);
        mCenterBarPaint.setColor(Color.BLACK);
        mCenterBarPaint.setStyle(Paint.Style.FILL);

        mMovingBarPaint = new Paint(0);
        mMovingBarPaint.setColor(Color.BLACK);
        mMovingBarPaint.setStyle(Paint.Style.FILL);

        mHorizontalBarPaint = new Paint(0);
        mHorizontalBarPaint.setColor(mHorizontalBarColor);
        mHorizontalBarPaint.setStyle(Paint.Style.FILL);

        mHorizontalBarBorderPaint = new Paint(0);
        mHorizontalBarBorderPaint.setColor(mHorizontalBarBorderColor);
        mHorizontalBarBorderPaint.setStyle(Paint.Style.FILL);

    }

}



