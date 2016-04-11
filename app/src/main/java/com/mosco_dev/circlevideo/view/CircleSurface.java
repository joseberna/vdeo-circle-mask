package com.mosco_dev.circlevideo.view;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CircleSurface extends SurfaceView {

    private Path clipPath;
    private int color = 0;

    public CircleSurface(Context context) {
        super(context);
        init();
    }

    public CircleSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        clipPath = new Path();
        Display disp = ((WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        clipPath.addCircle(disp.getWidth()/2, disp.getHeight()/2, (disp.getWidth()/2)-(disp.getWidth()/50), Path.Direction.CW);
        color = Color.WHITE;
    }


    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.drawColor(color);
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }
}
