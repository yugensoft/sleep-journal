package com.yugensoft.simplesleepjournal.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.View;

import com.yugensoft.simplesleepjournal.R;
import com.yugensoft.simplesleepjournal.ShapeDrawer;
import com.yugensoft.simplesleepjournal.database.TimeEntry;

import java.util.ArrayList;


public class DayRecordsDisplayBar extends View {
    // elements --
    private RectF mBounds = new RectF();

    private Paint mPaint = new Paint();

    // --
    // parameters --
    private float mTitleOffset = -20.0f;
    private int mArrowHeight = 40;
    private int mMinimumRowHeight = 40;
    private float mArrowWidthFactor = 0.25f;
    private float mCenterLineThickness = 2.0f;
    private float mTickWidth = 1.0f;
    private float mBigTickWidth = 2.0f;
    private float mTickLength = 20.0f;
    private float mBigTickLength = 30.0f;
    private int mLeftmostTickTime = 3; // time in hours for the first tick-bar (centered on prior midnight)
    private float mLeftmostTickFraction = 0.05f; // fraction of bar width for first tick bar position
    private int mRightmostTickTime = 27; // time in hours for the last tick-bar
    private float mRightmostTickFraction = 0.95f; // fraction of bar width from last tick bar position
    private int mTickFrequency = 1; // e.g. 3 means "one tick every 3 hours"
    private int mHeaderTimeLabelFrequency = 2; // as above
    private float mTextSize = 16.0f; // text size in case of "empty" etc
    private float mTargetBarWidth = 3.0f; // width of target-indicating bars
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

        //todo

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

        // if there is no information to display for this day, don't draw a bar, just draw a note
        if(mBlankSpaces != null){
            String text;
            if(mBlankSpaces == 1) {
                text = getContext().getString(R.string.bracket_none);
            } else {
                text = getContext().getResources().getQuantityString(R.plurals.blankDays,mBlankSpaces.intValue(),mBlankSpaces.intValue());
            }
            mPaint.setTextSize(mTextSize);
            ShapeDrawer.drawLabel(canvas, mBounds.centerX(), mBounds.centerY()+mTextSize/2, text, mPaint);
            return;
        }

        ShapeDrawer.TickSetting tickSetting = mIsHeader ? ShapeDrawer.TickSetting.BOTTOM_ONLY : ShapeDrawer.TickSetting.NORMAL;

        // draw center line
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(mCenterLineThickness);
        canvas.drawLine(mBounds.left,mBounds.centerY(),mBounds.right,mBounds.centerY(),mPaint);

        // draw ticks
        mPaint.setStrokeWidth(mTickWidth);
        float Xl = mLeftmostTickFraction*mBounds.width();
        float Xr = mRightmostTickFraction*mBounds.width();
        ShapeDrawer.drawTick(canvas, Xl, mBounds.centerY(), mTickLength, tickSetting, mPaint);
        ShapeDrawer.drawTick(canvas, Xr, mBounds.centerY(), mTickLength, tickSetting, mPaint);
        if(mRightmostTickTime < mLeftmostTickTime){
            throw new RuntimeException("Rightmost tick time must be greater than leftmost");
        }
        float tickSpacing = (Xr - Xl) / ((mRightmostTickTime - mLeftmostTickTime)/mTickFrequency);
        for(int i = mLeftmostTickTime; i <= mRightmostTickTime; i+=mTickFrequency){
            float tickLength;
            // put big ticks on 'special' times
            if(i == 0 || i == 12 || i == 24) {
                mPaint.setStrokeWidth(mBigTickWidth);
                tickLength = mBigTickLength;
            } else {
                mPaint.setStrokeWidth(mTickWidth);
                tickLength = mTickLength;
            }
            ShapeDrawer.drawTick(
                    canvas,
                    Xl + (i-mLeftmostTickTime)*tickSpacing,
                    mBounds.centerY(),
                    tickLength,
                    tickSetting,
                    mPaint
            );
        }

        // if this is a header row then draw labels
        if(mIsHeader) {
            for (int i = mLeftmostTickTime; i <= mRightmostTickTime; i += mHeaderTimeLabelFrequency) {
                String label = String.valueOf(i % 24);
                mPaint.setTextSize(16);
                ShapeDrawer.drawLabel(canvas, Xl + (i - mLeftmostTickTime) * tickSpacing, mBounds.centerY(), label, mPaint);
                mPaint.setColor(Color.GRAY);
                ShapeDrawer.drawLabel(canvas, mBounds.centerX(),mBounds.centerY()+mTitleOffset,getContext().getString(R.string.time),mPaint);
            }
        }

        // draw targets
        if(mTargets != null && mTargets.size() > 0){
            if(mTargets.size() > 2){
                throw new RuntimeException("Unexpected number of targets, maximum of 2 expected");
            }
            mPaint.setStrokeWidth(mTargetBarWidth);
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
                Yc = mBounds.centerY() - mCenterLineThickness / 2;
                color = Color.RED;
                invert = false;
            } else {
                Yc = mBounds.centerY() + mCenterLineThickness / 2;
                color = Color.GREEN;
                invert = true;
            }
            // time in hours relative to 0:00
            float t = TimeInHoursRelativeToLastMidnight(timeEntry);
            float x = hoursToX(t,mLeftmostTickTime,Xl,mRightmostTickTime,Xr);
            ShapeDrawer.drawFilledArrow(canvas, mArrowHeight, mArrowWidthFactor, x, Yc, color, invert, 0);

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
        return mArrowHeight*5;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        if(mBlankSpaces != null){
            return mMinimumRowHeight;
        } else {
            return mArrowHeight * 2;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        int minh = getPaddingBottom() + getPaddingTop() + getSuggestedMinimumHeight();
        int h = Math.max(MeasureSpec.getSize(heightMeasureSpec), minh);

        setMeasuredDimension(w, h);
    }


    public void setBlank(Long blankSpaces) {
        this.mBlankSpaces = blankSpaces;
    }

}
