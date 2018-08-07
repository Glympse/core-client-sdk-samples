package com.glympse.android.triggersdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver 
{

    @Override
    public void onReceive(Context context, Intent intent) 
    {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) 
        {
            // Make sure platform is up and running. 
            GlympseWrapper.instance().start(context.getApplicationContext());
        }
    }
}
