//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.createdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GTicket;
import com.glympse.android.controls.GTimePickerFragment;
import com.glympse.android.controls.GTimePickerFragment.OnConfirmedListener;
import com.glympse.android.controls.GTimerView.TimeProvider;

public class GlympseCreateDemoActivity extends FragmentActivity implements 
    OnConfirmedListener, TimeProvider, GEventListener
{
    private static final int REQUEST_CODE_LOCATION = 1;

    // Start us off with a locked "G".
    private int _durationMs = -1;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize our Glympse wrapper.
        GlympseWrapper.instance().start(this);
        GlympseWrapper.instance().getGlympse().addListener(this);
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
        timePicker.setOnConfirmedListener(GlympseCreateDemoActivity.this);
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
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},
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
        String text = textView.getText().toString();
        textView.setText(ticket.getInvites().at(0).getUrl() + ((text.length() > 0) ? ("\n" + text) : ""));
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
        }
    }
}
