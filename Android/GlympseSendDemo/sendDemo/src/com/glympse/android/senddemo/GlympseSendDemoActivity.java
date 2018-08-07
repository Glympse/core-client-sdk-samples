//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.senddemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GUser;
import com.glympse.android.controls.GTimePickerFragment;
import com.glympse.android.controls.GTimePickerFragment.OnConfirmedListener;
import com.glympse.android.controls.GTimerView.TimeProvider;
import com.glympse.android.core.CoreFactory;
import com.glympse.android.core.GDrawable;
import com.glympse.android.hal.HalFactory;
import com.glympse.android.hal.Helpers;
import com.glympse.android.hal.Reflection;

public class GlympseSendDemoActivity extends FragmentActivity implements
        OnConfirmedListener, TimeProvider, GEventListener
{
    private static final int REQUEST_CODE_LOCATION = 1;

    // Start us off with a locked "G".
    private int _durationMs = -1;
    private GTicket _activeTicket = null;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize our Glympse wrapper.
        GlympseWrapper.instance().start(this);

        GlympseWrapper.instance().getGlympse().addListener(this);
    }
    
    @Override protected void onPause()
    {
        super.onPause();
        GlympseWrapper.instance().setActive(false);
        _handler.removeCallbacks(timerTask);
    }

    @Override protected void onResume()
    {
        super.onResume();
        GlympseWrapper.instance().setActive(true);
        _handler.postDelayed(timerTask, 1000);
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

    public void onDurationClick(View v)
    {
        // Prompt for a duration.
        GTimePickerFragment timePicker = new GTimePickerFragment();
        timePicker.setDismissOnConfirm(true);
        timePicker.setOnConfirmedListener(GlympseSendDemoActivity.this);
        timePicker.show(getSupportFragmentManager(), "time_picker");
    }

    public void onExpireClick(View v)
    {
        if (_activeTicket != null)
        {
            _activeTicket.modify(0, null, null);
            ((TextView) findViewById(R.id.line2)).setText("Expiring...");
        }
    }

    public void onPlus15Click(View v)
    {
        if (_activeTicket != null)
        {
            _activeTicket.modify(_activeTicket.getDuration() + (int) Helpers.MS_PER_MINUTE * 15,
                _activeTicket.getMessage(), null);
            Toast.makeText(this, "Modifying Glympse", Toast.LENGTH_SHORT).show();
        }
    }

    public void onModifyClick(View v)
    {
        if (_activeTicket != null)
        {
            _activeTicket.modify(_durationMs, ((EditText) findViewById(R.id.edit_message)).getText().toString(), null);
            Toast.makeText(this, "Modifying Glympse", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSendClick(View v)
    {
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

    public void onExitClick(View v)
    {
        hideGlympseInfo();

        // Shutdown our Glympse wrapper.
        GlympseWrapper.instance().stop();

        // Close our activity.
        finish();
    }

    // By providing a TimeProvider to the timer control, we can ensure it uses
    // "glympse time" instead of local time (which may not be accurate)
    @Override public long getTime()
    {
        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        return glympse != null ? glympse.getTime() : System.currentTimeMillis();
    }

    @Override public void onConfirmed(GTimePickerFragment timePickerFragment)
    {
        // Store away this new duration.
        _durationMs = timePickerFragment.getDuration();

        // Put this duration on the button
        String duration = Helpers.formatDuration(_durationMs);
        ((Button) findViewById(R.id.button_duration)).setText(duration);
    }

    private void sendGlympse()
    {
        showGlympseInfo();
        ((TextView) findViewById(R.id.line1)).setText(R.string.sending_glympse_label);
        // Create this Glympse and listen for the completion.
        GTicket ticket = GlympseWrapper.instance().createGlympse(
            _durationMs,
            ((EditText) findViewById(R.id.edit_recipients)).getText().toString(),
            ((EditText) findViewById(R.id.edit_message)).getText().toString());

        // Handle error.
        if (null == ticket)
        {
            TextView textView = (TextView) findViewById(R.id.line1);
            textView.setText(getString(R.string.failed_to_create));
            findViewById(R.id.glympse_buttons).setVisibility(View.GONE);
        }
    }
    
    private void setNicknameAndAvatarIfBlank()
    {
        // The values for nickname and avatar are only certain after the 
        // GE.PLATFORM_SYNCED_WITH_SERVER event has been fired
        GUser self = GlympseWrapper.instance().getGlympse().getUserManager().getSelf();
        
        // Nicknames and avatars only need to be set within an application once and they
        // will persist between sessions. Therefore, we check to see if it's set and only
        // assign a new value if not.
        if(self.getNickname() == null)
        {
            self.setNickname("Send demo user");
        }
        
        if(self.getAvatar().getUrl() == null)
        {
            Bitmap avatar = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            GDrawable drawable = CoreFactory.createDrawable(new BitmapDrawable(this.getResources(), avatar));
            self.setAvatar(drawable);
        }
        
    }

    private void showGlympseInfo()
    {
        findViewById(R.id.glympse_details).setVisibility(View.VISIBLE);
        findViewById(R.id.glympse_buttons).setVisibility(View.VISIBLE);
    }

    private void hideGlympseInfo()
    {
        findViewById(R.id.glympse_details).setVisibility(View.GONE);
        findViewById(R.id.glympse_buttons).setVisibility(View.GONE);
    }

    private void updateGlympseInfo(GTicket ticket)
    {
        ((TextView) findViewById(R.id.line1)).setText(getRecipientsAsString(ticket));
        ((TextView) findViewById(R.id.line2)).setText(getNumberOfWatchers(ticket));
    }

    private String getRecipientsAsString(GTicket ticket)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipients: ");
        for (GInvite invite : ticket.getInvites())
        {
            sb.append(invite.getName() != null ? invite.getName() : invite.getAddress());
            sb.append(", ");
        }
        return (sb.length() > 0) ? sb.delete(sb.length() - 2, sb.length()).toString() : "(No Recipients)";
    }

    private String getNumberOfWatchers(GTicket ticket)
    {
        int count = 0;
        long currentTime = getTime();
        for ( GInvite invite : ticket.getInvites() )
        {
            if (currentTime - invite.getLastViewTime() < 3L * Helpers.MS_PER_MINUTE)
                count++;
        }
        return String.valueOf(count) + " watching";
    }

    // Timer functionality
    private Handler _handler = new Handler();
    
    final Runnable timerTask = new Runnable()
    {
        public void run()
        {
            if(_activeTicket != null)
            {
                ((TextView) findViewById(R.id.line3)).setText(getTimeRemainingAsString(_activeTicket));
            }
            _handler.postDelayed(timerTask, 1000);
        }
    };
    
    private String getTimeRemainingAsString(GTicket ticket)
    {
        long expireTime = ticket.getExpireTime();
        long currentTime = 0;
        if (GlympseWrapper.instance().getGlympse() != null)
        {
            currentTime = GlympseWrapper.instance().getGlympse().getTime();
        }
        return Helpers.formatDuration(expireTime - currentTime) + " remaining";
    }


    private void onTicketCreated(GTicket ticket)
    {
        _activeTicket = ticket;
        showGlympseInfo();
        updateGlympseInfo(ticket);
    }

    private void onInviteUpdated(GTicket ticket)
    {
        updateGlympseInfo(ticket);
    }

    private void onTicketExpired(GTicket ticket)
    {
        if (ticket.getId() == _activeTicket.getId())
        {
            hideGlympseInfo();
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
            else if (0 != (events & GE.PLATFORM_SYNCED_WITH_SERVER))
            {
                setNicknameAndAvatarIfBlank();
                for (GTicket ticket : GlympseWrapper.instance().getGlympse().getHistoryManager().getTickets())
                {
                    if (ticket.isActive())
                    {
                        onTicketCreated(ticket);
                    }
                }
            }
        }
        else if (GE.LISTENER_TICKET == listener)
        {
            if (0 != (events & GE.TICKET_CREATED))
            {
                Log.d("", "Ticket Created");
                onTicketCreated((GTicket) obj);
            }
            else if (0 != (events & GE.TICKET_INVITE_UPDATED))
            {
                Log.d("", "Ticket Invite Updated");
                onInviteUpdated((GTicket) obj);
            }
            else if (0 != (events & GE.TICKET_EXPIRED))
            {
                Log.d("", "Ticket Expired");
                onTicketExpired((GTicket) obj);
            }
        }
    }

}
