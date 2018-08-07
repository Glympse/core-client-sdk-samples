package com.glympse.android.triggersdemo;

import java.util.Date;

import android.content.Context;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGeoTrigger;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GTrigger;
import com.glympse.android.core.CC;

public class ProximityListener implements GEventListener
{
    private Context _context;
    
    public ProximityListener(Context context)
    {
        _context = context;
    }

    @Override
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( 0 != ( GE.TRIGGERS_TRIGGER_ACTIVATED & events ) )
        {
            GTrigger trigger = (GTrigger)obj;
            
            String event = "fired";
            
            // Handle Geo triggers
            if ( GC.TRIGGER_TYPE_GEO == trigger.getType() )
            {
                GGeoTrigger geoTrigger = (GGeoTrigger)trigger;
                int transition = geoTrigger.getTransition();
                if ( 0 != ( CC.GEOFENCE_TRANSITION_ENTER & transition ) )
                    event = "entered";
                else if ( 0 != ( CC.GEOFENCE_TRANSITION_EXIT & transition ) )
                    event = "exited";
            }
            
            // Log event            
            logTrigger(trigger, event);            
            
            // Send a Glympse
            String message = generateMessage(trigger, event); 
            GTicket ticket = trigger.getTicket().clone();
            ticket.modify(GC.DURATION_NO_CHANGE, message, null);
            glympse.sendTicket(ticket);
        }
        else if ( 0 != ( GE.TRIGGERS_TRIGGER_REMOVED & events ) )
        {
            GTrigger trigger = (GTrigger)obj;
            logTrigger(trigger, "removed");
        }
        else if ( 0 != ( GE.TRIGGERS_TRIGGER_ADDED & events ) )
        {
            GTrigger trigger = (GTrigger)obj;
            logTrigger(trigger, "added");
        }
    }
    
    private String generateMessage(GTrigger trigger, String event)
    {
        return new Date(System.currentTimeMillis()).toString() + ": '" + trigger.getName() + "' " + event;
    }
    
    private void logTrigger(GTrigger trigger, String event)
    {
        Date date = new Date(System.currentTimeMillis());
        String newLogEntry = EventLogStorage.createNewEntry(date, trigger, event);
        EventLogStorage.appendLog(_context, newLogEntry); // Save the log entry
    }

}
