package com.yugensoft.simplesleepjournal;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.annotation.Nullable;

public class ShapeDrawer {
    public enum TickSetting {
        NORMAL, TOP_ONLY, BOTTOM_ONLY
    }

    /**
     * Draws a filled arrow
     * @param canvas Canvas to draw on
     * @param H Arrow height
     * @param Wf Width factor. Arrow stem width = Wf * Arrow height
     * @param Xc X co-ordinate of arrow tip
     * @param Yc Y co-ordinate of arrow tip
     * @param color Arrow fill color
     * @param inverted True for upward-pointing, False for downward-pointing
     * @param rotate Angle to rotate the arrow (about the tip)
     */
    public static void drawFilledArrow(Canvas canvas, float H, float Wf, float Xc, float Yc, int color, boolean inverted, float rotate){
        // shape settings
        final double THETA = 0.5 * (Math.PI / 2);
        final float Fh = 0.4f; // arrow-head height factor
        final float Ws = H * Wf;    // stem width
        final float Fi = (inverted ? -1 : 1); // inversion factor, makes arrow flip on vertical axis

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);

        Path path = new Path();
        path.moveTo(Xc, Yc);

        // plot out arrow shape
        final float t = (float)(H*Fh*Math.tan(THETA));
        final PointF[] p = new PointF[6];
        p[0] = new PointF(Xc - t, Yc - Fi * H*Fh);
        p[1] = new PointF(Xc - Ws/2, Yc - Fi * H*Fh);
        p[2] = new PointF(Xc - Ws/2, Yc - Fi * H);
        p[5] = new PointF(Xc + t, Yc - Fi * H*Fh);
        p[4] = new PointF(Xc + Ws/2, Yc - Fi * H*Fh);
        p[3] = new PointF(Xc + Ws/2, Yc - Fi * H);

        for (PointF q: p) {
            path.lineTo(q.x,q.y);
        }
        path.close();

        // rotation
        if(Float.compare(0,rotate) != 0){
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate,Xc,Yc);
            path.transform(matrix);
        }

        canvas.drawPath(path,paint);
    }

    /**
     * Draw a tick (a mark on a horizontal line)
     * @param canvas Canvas to draw on
     * @param x X position of tick
     * @param y Y position of the line to be drawn on
     * @param l Length of the tick (top to bottom)
     * @param paint Paint to use to make the line
     */
    public static void drawTick(Canvas canvas, float x, float y, float l, TickSetting setting, Paint paint){
        switch (setting){
            case NORMAL:
                canvas.drawLine(
                        x,
                        y - l/2,
                        x,
                        y + l/2,
                        paint
                );
                break;
            case TOP_ONLY:canvas.drawLine(
                    x,
                    y - l/2,
                    x,
                    y,
                    paint
            );
                break;
            case BOTTOM_ONLY:
                canvas.drawLine(
                        x,
                        y,
                        x,
                        y + l/2,
                        paint
                );
                break;
        }

    }

    public static void drawLabel(Canvas canvas, float x, float y, String s, Paint paint){
        float xOffset = paint.measureText(s) / 2;
        float yOffset = 3;
        canvas.drawText(s,x-xOffset,y-yOffset,paint);
    }
}
