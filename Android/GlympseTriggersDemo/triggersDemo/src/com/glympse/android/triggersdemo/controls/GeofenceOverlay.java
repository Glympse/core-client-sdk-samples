package com.glympse.android.triggersdemo.controls;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class GeofenceOverlay extends View
{
    private ShapeDrawable _fence;
    private static final int MIN_BORDER = 80;
    
    int _radius;

    public GeofenceOverlay(Context context)
    {
        this(context, null);
    }
    
    public GeofenceOverlay(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public GeofenceOverlay(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        _radius = 0;
        _fence = new ShapeDrawable(new OvalShape());
        Paint paint = _fence.getPaint();
        paint.setColor(Color.rgb(0, 0, 55));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.0f);
    }
    
    @Override protected void onDraw(Canvas canvas)
    {
        if ( 0 == _radius )
        {
            // Calculate the size of the fence
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            _radius = (Math.min(canvasWidth, canvasHeight) / 2) - MIN_BORDER;
            int centerX = canvasWidth / 2;
            int centerY = canvasHeight / 2;
            int x1, y1, x2, y2;
            
            x1 = centerX - _radius;
            y1 = centerY - _radius;
            x2 = centerX + _radius;
            y2 = centerY + _radius;
            _fence.setBounds(x1, y1, x2, y2);
        }
        
        // Draw fence
        _fence.draw(canvas);
    }
    
    public double getRadiusInMeters(LatLng centerPoint, float zoomLevel)
    {
        double EQUATOR_LENGTH = 40075004;
        double currentEarthWidthInDp = 256 * Math.pow(2.0, zoomLevel);
        double latitudeAdjustment = Math.cos( Math.PI * centerPoint.latitude / 180.0);
        double metersPerDp = EQUATOR_LENGTH / currentEarthWidthInDp * latitudeAdjustment;
        
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float radiusInDp = _radius / metrics.scaledDensity;
        return radiusInDp * metersPerDp;
    }

}
