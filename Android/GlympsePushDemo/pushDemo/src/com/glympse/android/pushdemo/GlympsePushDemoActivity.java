//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.pushdemo;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GTicket;
import com.glympse.android.controls.GTimePickerFragment;
import com.glympse.android.controls.GTimePickerFragment.OnConfirmedListener;
import com.glympse.android.controls.GTimerView.TimeProvider;
import com.glympse.android.lib.GEP;
import com.glympse.android.lib.GGlympsePrivate;

/**
 * TODO: The Glympse server needs to know your app's Client ID, Client Secret, and Glympse API Key
 * in order to send push notifications to your app (this is true even when using this sample app).
 * You can send these to partnerdev@glympse.com
 * 
 * See the ADM section of the Partner Guide in the tutorials directory for more information.
 */
public class GlympsePushDemoActivity extends FragmentActivity implements 
    OnConfirmedListener, TimeProvider, GEventListener
{
    private static final int REQUEST_CODE_LOCATION = 1;

    // Start us off with a locked "G".
    private int _durationMs = -1;
    
    // Only keeping track of one ticket at a time
    private GTicket _ticket = null;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize our Glympse wrapper.
        GlympseWrapper.instance().start(this);
        GGlympsePrivate glympse = (GGlympsePrivate) GlympseWrapper.instance().getGlympse();
        glympse.addListener(this);
        
        // In order to receive push notifications we need to add a listener to NotificationCenter
        glympse.getNotificationCenter().addListener(this);
    }
    
    @Override protected void onDestroy()
    {
        super.onDestroy();
        
        // Stop listening for events
        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if (null != glympse)
        {
        	glympse.removeListener(this);
        }
    }    

    @Override public void onPause()
    {
        super.onPause();

        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if (null != glympse)
        {
            glympse.setActive(false);
        }
    }

    @Override public void onResume()
    {
        super.onResume();

        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if (null != glympse)
        {
            glympse.setActive(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if ( grantResults[0] == PackageManager.PERMISSION_GRANTED )
        {
            if ( REQUEST_CODE_LOCATION == requestCode )
            {
                sendGlympse();
            }
        }
    }

    public void onCreateClick(View v)
    {
        // Prompt for a duration.
        GTimePickerFragment timePicker = new GTimePickerFragment();
        timePicker.setDismissOnConfirm(true);
        timePicker.setOnConfirmedListener(GlympsePushDemoActivity.this);
        timePicker.show(getSupportFragmentManager(), "time_picker");
    }

    public void onExitClick(View v)
    {
        // Shutdown our Glympse wrapper.
        GlympseWrapper.instance().stop();

        // Close our activity.
        finish();
    }

    // By providing a TimeProvider to the timer control, we can ensure it uses
    // "glympse time" instead of local time (which may not be accurate)
    @Override public long getTime()
    {
        return GlympseWrapper.instance().getGlympse().getTime();
    }

    @Override public void onConfirmed(GTimePickerFragment timePickerFragment)
    {
        // Store away this new duration.
        _durationMs = timePickerFragment.getDuration();

        // Request location permission so that we can get location updates from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) )
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION);
        }
        else
        {
            sendGlympse();
        }
    }

    private void sendGlympse()
    {
        // Create this Glympse and listen for the completion.
        GTicket ticket = GlympseWrapper.instance().createGlympse(_durationMs);

        // Handle error.
        if (null == ticket)
        {
            TextView textView = (TextView) findViewById(R.id.text_url);
            textView.setText(getString(R.string.failed_to_create));
        }
    }

    private void onGlympseCreated(GTicket ticket)
    {
        TextView textView = (TextView) findViewById(R.id.text_url);
        textView.setText(ticket.getInvites().at(0).getUrl());
        
        _ticket = ticket;
        updateViewCounts();
    }
    
    private void updateViewCounts()
    {
        if (null != _ticket)
        {
            TextView viewCount = (TextView) findViewById(R.id.text_view_count);
            int viewing = _ticket.getInvites().at(0).getViewing(); // Viewers right now
            int views = _ticket.getInvites().at(0).getViewers(); // Total views ever
            viewCount.setText("Viewing: " + String.valueOf(viewing) + ", Total views: " + String.valueOf(views));
        }
    }

    @Override public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if (GE.LISTENER_PLATFORM == listener)
        {
            if (0 != (events & GE.PLATFORM_TICKET_ADDED))
            {
                GTicket ticket = (GTicket) obj;
                ticket.addListener(this);
            }
            else if (0 != (events & GE.PLATFORM_TICKET_REMOVED))
            {
                GTicket ticket = (GTicket) obj;
                ticket.removeListener(this);
            }
            else if (0 != (events & GE.PLATFORM_STOPPED))
            {
                GlympseWrapper.instance().clear();
            }
        }
        else if (GE.LISTENER_TICKET == listener)
        {
            if (0 != (events & GE.TICKET_INVITE_CREATED))
            {
                onGlympseCreated((GTicket) obj);
            }
            else if (0 != (events & GE.TICKET_INVITE_UPDATED))
            {
                Log.d("PushDemo", "Updating viewer count");
                updateViewCounts();
            }
        }
        // Listen for Push events
        else if ( GEP.LISTENER_PUSH == listener )
        {
            if (0 != (events & GEP.PUSH_VIEWER))
            {
                // When this event is fired, the platform may still be in the process of getting the total number of viewers.
                // GE.TICKET_INVITE_UPDATED should fire soon after this push is received.
                Log.d("PushDemo", "Viewer count changed");
            }
        }
    }
}
