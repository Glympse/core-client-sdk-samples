//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.historydemo;

import java.util.LinkedList;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GTicket;
import com.glympse.android.controls.GTimePickerFragment;
import com.glympse.android.controls.GTimePickerFragment.OnConfirmedListener;
import com.glympse.android.controls.GTimerView.TimeProvider;
import com.glympse.android.hal.Helpers;


public class GlympseHistoryDemoActivity extends FragmentActivity implements
        OnConfirmedListener, TimeProvider, GEventListener
{
    private static final int REQUEST_CODE_LOCATION = 1;

    // Start us off with a locked "G".
    private static int _durationMs = -1;
    
    private List<HistoryItem> _historyItems;
    public static HistoryAdapter _historyAdapter;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialize history list
        ListView historyList = ((ListView) findViewById(R.id.history_list));
        _historyItems = new LinkedList<HistoryItem>();
        _historyAdapter = new HistoryAdapter(historyList, _historyItems, this);
        historyList.setAdapter(_historyAdapter);

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
    
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.history_options, menu);
        return true;
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        switch ( itemId )
        {
        case R.id.menu_clear_history:
            clearHistory();
            break;
        }
        return true;
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

    private void clearHistory()
    {
        for(GTicket ticket : GlympseWrapper.instance().getGlympse().getHistoryManager().getTickets())
        {
            ticket.deleteTicket();
        }
        _historyItems.clear();
        _historyAdapter.notifyDataSetChanged();
    }

    public void onDurationClick(View v)
    {
        // Prompt for a duration.
        GTimePickerFragment timePicker = new GTimePickerFragment();
        timePicker.setDismissOnConfirm(true);
        timePicker.setOnConfirmedListener(GlympseHistoryDemoActivity.this);
        timePicker.show(getSupportFragmentManager(), "time_picker");
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
        // Create this Glympse and listen for the completion.
        GTicket ticket = GlympseWrapper.instance().createGlympse(getDuration(),
                getRecipients(),
                getMessage());
        
        if(ticket != null)
        {
            ((TextView) findViewById(R.id.history_message)).setVisibility(View.GONE);
            _historyItems.add(0, new HistoryItem(ticket, this));
            _historyAdapter.notifyDataSetChanged();
        }
        else
        {
            Toast.makeText(this, "Failed to create Glympse", Toast.LENGTH_SHORT).show();
        }
    }
    
    public int getDuration()
    {
        return _durationMs;
    }
    
    public String getRecipients()
    {
        return ((EditText) findViewById(R.id.edit_recipients)).getText().toString();
    }
    
    public String getMessage()
    {
        return ((EditText) findViewById(R.id.edit_message)).getText().toString();
    }

    private void onTicketCreated(GTicket ticket)
    {
        _historyAdapter.updateTicket(ticket);
    }

    private void onInviteUpdated(GTicket ticket)
    {
        _historyAdapter.updateTicket(ticket);
    }

    private void onTicketExpired(GTicket ticket)
    {
        _historyAdapter.updateTicket(ticket);
    }
    
    private void onHistorySync()
    {
        // History may be empty
        if (GlympseWrapper.instance().getGlympse().getHistoryManager().getTickets().length() == 0)
        {
            ((TextView) findViewById(R.id.history_message)).setText("No Glympses found");
            return;
        }
        
        // hide the message view
        ((TextView) findViewById(R.id.history_message)).setVisibility(View.GONE);

        // Build list of History Items
        _historyItems.clear();
        for (GTicket ticket : GlympseWrapper.instance().getGlympse().getHistoryManager().getTickets())
        {
            _historyItems.add(new HistoryItem(ticket, this));
        }
        _historyAdapter.notifyDataSetChanged();
    }
    
    // Timer functionality
    private Handler _handler = new Handler();
    
    final Runnable timerTask = new Runnable()
    {
        public void run()
        {
            _historyAdapter.notifyDataSetChanged();
            _handler.postDelayed(timerTask, 1000);
        }
    };

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
                onHistorySync();
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
