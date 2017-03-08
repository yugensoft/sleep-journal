package com.yugensoft.simplesleepjournal.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.yugensoft.simplesleepjournal.R;
import com.yugensoft.simplesleepjournal.ShapeDrawer;
import com.yugensoft.simplesleepjournal.database.TimeEntry;

import java.util.ArrayList;


public class DayRecordsDisplayBar extends View {
    private static final String TAG = "records_visual";
    // elements --
    private RectF mBounds = new RectF();

    private Paint mPaint = new Paint();  // general paint
    private Paint mPaintLabel = new Paint();
    //--

    Rect textBounds = new Rect(); // working variable

    public static class ScalableSizeParameters {
        public static float MAXIMUM_SCALE = 2.5f;
        public static float MINIMUM_SCALE = 1.0f;
        public static float ROUNDING_FRACTION = 0.25f;

        private float mScale = 1.0f;

        private float mArrowHeight = 40;
        private float mMinimumRowHeight = 40;
        private float mTextSize = 16.0f; // text size in case of "empty" etc
        private float mTargetBarWidth = 3.0f; // width of target-indicating bars
        private float mCenterLineThickness = 2.0f;
        private float mTickWidth = 1.0f;
        private float mBigTickWidth = 2.0f;
        private float mTickLength = 20.0f;
        private float mBigTickLength = 30.0f;

        public float getArrowHeight() {
            return mArrowHeight * mScale;
        }

        public void setArrowHeight(int arrowHeight) {
            mArrowHeight = arrowHeight;
        }

        public float getMinimumRowHeight() {
            return mMinimumRowHeight * mScale;
        }

        public void setMinimumRowHeight(int minimumRowHeight) {
            mMinimumRowHeight = minimumRowHeight;
        }

        public float getTextSize() {
            return mTextSize * mScale;
        }

        public void setTextSize(float textSize) {
            mTextSize = textSize;
        }

        public float getTargetBarWidth() {
            return mTargetBarWidth * mScale;
        }

        public void setTargetBarWidth(float targetBarWidth) {
            mTargetBarWidth = targetBarWidth;
        }

        public float getCenterLineThickness() {
            return mCenterLineThickness * mScale;
        }

        public void setCenterLineThickness(float centerLineThickness) {
            mCenterLineThickness = centerLineThickness;
        }

        public float getTickWidth() {
            return mTickWidth * mScale;
        }

        public void setTickWidth(float tickWidth) {
            mTickWidth = tickWidth;
        }

        public float getBigTickWidth() {
            return mBigTickWidth * mScale;
        }

        public void setBigTickWidth(float bigTickWidth) {
            mBigTickWidth = bigTickWidth;
        }

        public float getTickLength() {
            return mTickLength * mScale;
        }

        public void setTickLength(float tickLength) {
            mTickLength = tickLength;
        }

        public float getBigTickLength() {
            return mBigTickLength * mScale;
        }

        public void setBigTickLength(float bigTickLength) {
            mBigTickLength = bigTickLength;
        }

        public float getScale() {
            return mScale;
        }

        /**
         * Sets the scale by which the components are rendered.
         * @param scale Scale, limited by a maximum and minimum.
         */
        public void setScale(float scale) {
            // set limits on the scale
            if(scale > MAXIMUM_SCALE) {
                scale = MAXIMUM_SCALE;
            } else if(scale < MINIMUM_SCALE) {
                scale = MINIMUM_SCALE;
            }

            mScale = scale;
        }

    }

    // parameters --
    private ScalableSizeParameters sizes = new ScalableSizeParameters();
    private float mArrowWidthFactor = 0.25f;
    private int mLeftmostTickTime = 3; // time in hours for the first tick-bar (centered on prior midnight)
    private float mLeftmostTickFraction = 0.05f; // fraction of bar width for first tick bar position
    private int mRightmostTickTime = 27; // time in hours for the last tick-bar
    private float mRightmostTickFraction = 0.95f; // fraction of bar width from last tick bar position
    private int mTickFrequency = 1; // e.g. 3 means "one tick every 3 hours"
    private int mHeaderTimeLabelFrequency = 3; // as above
    private float mTitleMargin = 10.0f; // space between title and tick labels

    // --

    private boolean mIsHeader = false;

    // Data --
    private ArrayList<TimeEntry> mData = new ArrayList<TimeEntry>();
    private Long mBlankSpaces = null;
    private ArrayList<TimeEntry> mTargets = new ArrayList<>();
    //

    // Constructors
    public DayRecordsDisplayBar(Context context) {
        super(context);
        init();
    }
    public DayRecordsDisplayBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }
    public void init(){

    }

    // Setters and getters
    public void setData(ArrayList<TimeEntry> data){
        mData = data;
        onDataChanged();
        invalidate();
    }
    public ArrayList<TimeEntry> getData(){
        return mData;
    }
    public ArrayList<TimeEntry> getTargets() {
        return mTargets;
    }

    public void setTargets(ArrayList<TimeEntry> targets) {
        this.mTargets = targets;
    }

    /**
     * Makes this view appear as a header row
     */
    public void makeHeader(){
        mIsHeader = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaintLabel.setTextSize(sizes.getTextSize());
        mPaintLabel.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        
        float centerLineY;
        if(mIsHeader) {
            centerLineY = mBounds.bottom - sizes.getBigTickLength() / 2;
        } else {
            centerLineY = mBounds.centerY();
        }

        // if there is no information to display for this day, don't draw a bar, just draw a note
        if(mBlankSpaces != null){
            String text;
            if(mBlankSpaces == 1) {
                text = getContext().getString(R.string.bracket_none);
            } else {
                text = getContext().getResources().getQuantityString(R.plurals.blankDays,mBlankSpaces.intValue(),mBlankSpaces.intValue());
            }
            mPaint.setTextSize(sizes.getTextSize());
            ShapeDrawer.drawLabel(canvas, mBounds.centerX(), centerLineY+sizes.getTextSize()/2, text, mPaint);
            return;
        }

        ShapeDrawer.TickSetting tickSetting = mIsHeader ? ShapeDrawer.TickSetting.BOTTOM_ONLY : ShapeDrawer.TickSetting.NORMAL;

        // draw center line
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(sizes.getCenterLineThickness());
        canvas.drawLine(mBounds.left,centerLineY,mBounds.right,centerLineY,mPaint);

        // draw ticks
        mPaint.setStrokeWidth(sizes.getTickWidth());
        float Xl = mLeftmostTickFraction*mBounds.width();
        float Xr = mRightmostTickFraction*mBounds.width();
        ShapeDrawer.drawTick(canvas, Xl, centerLineY, sizes.getTickLength(), tickSetting, mPaint);
        ShapeDrawer.drawTick(canvas, Xr, centerLineY, sizes.getTickLength(), tickSetting, mPaint);
        if(mRightmostTickTime < mLeftmostTickTime){
            throw new RuntimeException("Rightmost tick time must be greater than leftmost");
        }
        float tickSpacing = (Xr - Xl) / ((mRightmostTickTime - mLeftmostTickTime)/mTickFrequency);
        for(int i = mLeftmostTickTime; i <= mRightmostTickTime; i+=mTickFrequency){
            float tickLength;
            // put big ticks on 'special' times
            if(i == 0 || i == 12 || i == 24) {
                mPaint.setStrokeWidth(sizes.getBigTickWidth());
                tickLength = sizes.getBigTickLength();
            } else {
                mPaint.setStrokeWidth(sizes.getTickWidth());
                tickLength = sizes.getTickLength();
            }
            ShapeDrawer.drawTick(
                    canvas,
                    Xl + (i-mLeftmostTickTime)*tickSpacing,
                    centerLineY,
                    tickLength,
                    tickSetting,
                    mPaint
            );
        }

        // if this is a header row then draw labels
        if(mIsHeader) {
            // tick labels
            for (int i = mLeftmostTickTime; i <= mRightmostTickTime; i += mHeaderTimeLabelFrequency) {
                String label = String.valueOf(i % 24);
                ShapeDrawer.drawLabel(canvas, Xl + (i - mLeftmostTickTime) * tickSpacing, centerLineY, label, mPaintLabel);
            }
            // draw title
            mPaint.setTextSize(sizes.getTextSize());
            mPaint.setColor(Color.GRAY);
            String title = getContext().getString(R.string.time);
            mPaint.getTextBounds(title, 0, title.length(), textBounds);
            ShapeDrawer.drawLabel(canvas, mBounds.centerX(),mBounds.top + textBounds.height() + 10,title,mPaint);
        }

        // draw targets
        if(mTargets != null && mTargets.size() > 0){
            if(mTargets.size() > 2){
                throw new RuntimeException("Unexpected number of targets, maximum of 2 expected");
            }
            mPaint.setStrokeWidth(sizes.getTargetBarWidth());
            for (TimeEntry target : mTargets) {
                float t = TimeInHoursRelativeToLastMidnight(target);
                float x = hoursToX(t,mLeftmostTickTime,Xl,mRightmostTickTime,Xr);
                if(target.get_direction().equals(TimeEntry.Direction.BEDTIME)) {
                    mPaint.setColor(Color.BLUE);
                } else {
                    mPaint.setColor(Color.MAGENTA);
                }
                canvas.drawLine(x, mBounds.top, x, mBounds.bottom, mPaint);
            }
        }

        // draw arrows
        for(TimeEntry timeEntry: mData){
            float Yc;
            boolean invert;
            int color;
            if(timeEntry.get_direction().equals(TimeEntry.Direction.BEDTIME)) {
                Yc = centerLineY - sizes.getCenterLineThickness() / 2;
                color = Color.RED;
                invert = false;
            } else {
                Yc = centerLineY + sizes.getCenterLineThickness() / 2;
                color = Color.GREEN;
                invert = true;
            }
            // time in hours relative to 0:00
            float t = TimeInHoursRelativeToLastMidnight(timeEntry);
            float x = hoursToX(t,mLeftmostTickTime,Xl,mRightmostTickTime,Xr);
            ShapeDrawer.drawFilledArrow(canvas, sizes.getArrowHeight(), mArrowWidthFactor, x, Yc, color, invert, 0);

        }

    }

    /**
     * Gets time since last midnight from a time entry
     * @param timeEntry Time entry record.
     * @return Relative time in hours.
     */
    private float TimeInHoursRelativeToLastMidnight(TimeEntry timeEntry){
        return ((float)(timeEntry.get_time() - timeEntry.get_centerOfDay()) / (1000 * 60 * 60)) + 12;
    }

    /**
     * Convert hours to X position, based on the leftmost tick and rightmost tick
     * @param t Time in hours to convert
     * @param Tl Time in hours of leftmost tick
     * @param Xl X position of leftmost tick
     * @param Tr TIme in hours of rightmost tick
     * @param Xr X position of rightmost tick
     * @return X position
     */
    private float hoursToX(float t, int Tl, float Xl, int Tr, float Xr){
        return Xl + (((t - Tl) / (Tr-Tl)) * (Xr - Xl));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if(mIsHeader){
            Log.d(TAG, "onSizeChanged: header");
        }

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

    private void onDataChanged(){
        //todo
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // five unscaled arrows
        return (int)(sizes.getArrowHeight() / sizes.getScale() * 5);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        if(mIsHeader) {
            return (int) (sizes.getTextSize() * 1 + sizes.getBigTickLength() + mTitleMargin);
        } else if(mBlankSpaces != null){
            // if it is a blank row, return a fixed minimum row height
            return (int)sizes.getMinimumRowHeight();
        } else {
            // normal row; use 2 scaled arrows
            return (int)(sizes.getArrowHeight() * 2);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if(mIsHeader){
            Log.d(TAG, "onMeasure: header");
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int w;
        int h;

        int minWidth = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int minHeight = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight();

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            w = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            w = widthSize;
        } else {
            //Be whatever you want
            w = minWidth;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            h = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            h = Math.min(minHeight, heightSize);
        } else {
            //Be whatever you want
            h = minHeight;
        }

        setMeasuredDimension(w, h);
    }


    public void setBlank(Long blankSpaces) {
        this.mBlankSpaces = blankSpaces;
    }

    /**
     * Sets the rendering scale of the view components.
     * @param renderScale New scale.
     */
    public void setRenderScale(float renderScale) {
        this.sizes.setScale(renderScale);
        Log.d(TAG, "scale change: " + String.valueOf(this.sizes.getScale()));

        invalidate();

    }
}
