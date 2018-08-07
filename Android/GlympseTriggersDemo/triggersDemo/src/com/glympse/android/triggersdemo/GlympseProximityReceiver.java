package com.glympse.android.triggersdemo;

import com.glympse.android.hal.ProximityReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GlympseProximityReceiver extends BroadcastReceiver 
{
    @Override public void onReceive(Context context, Intent intent)
    {
        try
        {
            if ( ProximityReceiver.ACTION_APP_REGION.equals(intent.getAction()) )
            {        
                // Make sure platform is up and running. 
                GlympseWrapper.instance().start(context.getApplicationContext());
                
                // Propagate the event to the platform.
                ProximityReceiver.propagateGeofence(intent, GlympseWrapper.instance().getGlympse());
            }
        }
        catch ( Throwable e )
        {                
        }
    }
}
