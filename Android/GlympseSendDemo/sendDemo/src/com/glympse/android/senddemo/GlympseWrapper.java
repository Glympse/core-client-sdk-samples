//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.senddemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.glympse.android.api.GC;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GlympseFactory;

import java.util.Vector;

/**
 * Class to wrap Glympse API into singleton for convenience.
 */
public class GlympseWrapper
{
    /**
     * Specify proper server URL here.
     */
    public static final String BASE_URL = "api.glympse.com";
    
    /**  
     * TODO: You need to place your Glympse assigned application key in the API_KEY variable below.
     * If you need a key, you can request one by visiting https://developer.glympse.com/.
     */        
    public static final String API_KEY = "<< YOUR API KEY >>";

    private static GGlympse _glympse = null;

    public void start(Context context)
    {
        if (_glympse == null)
        {
            // TODO: This can be removed, when project is configured.
            if (API_KEY.contains(" "))
            {
                throw new RuntimeException("Valid API key should be specified in GlympseWrapper.API_KEY");
            }
            
            // Create Glympse platform and pass in server URL and API key.
            _glympse = GlympseFactory.createGlympse(context, BASE_URL, API_KEY);

            // Mark this application as exempt from needing user consent (since this is a dev app)
            // See GlympseCreateDemo for an example of handling user consent
            _glympse.getConsentManager().exemptFromConsent(true);

            // Start Glympse platform right away.
            _glympse.start();
        }
    }

    public void stop()
    {
        if (null != _glympse)
        {
            // Shutdown the Glympse API.
            _glympse.stop();
            
            // Perform required cleanup.
            clear();
        }
    }
    
    public void setActive(boolean active)
    {
        if (_glympse != null)
        {
            _glympse.setActive(active);
        }
    }
    
    public GGlympse getGlympse()
    {
        return _glympse;
    }

    public void clear()
    {
        _glympse = null;
    }
    
    /**
     * @name Singleton Section
     */
    
    private static GlympseWrapper _instance;

    public static synchronized GlympseWrapper instance()
    {
        if (null == _instance)
        {
            _instance = new GlympseWrapper();
        }
        return _instance;
    }

    /**
     * Listens to the intent broadcasted by Glympse service, when it is
     * restarted by the OS. TODO: Make sure to mention this receiver in the
     * application manifest file.
     */
    public static class ServiceReceiver extends BroadcastReceiver
    {
        @Override public void onReceive(Context context, Intent intent)
        {
            if ( "com.glympse.android.hal.service.STARTED".equals(intent.getAction()) )
            {
                // Instantiate and start Glympse platform.
                GlympseWrapper.instance().start(context);
            }
        }
    }
    
    public GTicket createGlympse(int duration, String recipients, String message)
    {
        if (_glympse == null)
        {
            return null;
        }

        // Split the recipients up.
        String[] recipientStringArray = recipients.split(",");

        // Create a vector to hold the list of Recipient objects.
        Vector<GInvite> recipientList = new Vector<GInvite>();

        // Loop through each recipient string we parsed.
        for (String address : recipientStringArray)
        {
            // Create a new recipient from the address we parsed. By
            // specifying a type of "ERecipientType_Unknown", the Recipient
            // constructor will examine the text and choose a type.
            GInvite recipient = GlympseFactory.createInvite(GC.INVITE_TYPE_UNKNOWN, null, address);

            // If the recipient type could not be determined, then bail out.
            if (recipient == null || GC.INVITE_TYPE_UNKNOWN == recipient.getType())
            {
                return null;
            }

            // Add this recipient object to our list.
            recipientList.add(recipient);
        }

        // Create the ticket for the given duration.
        GTicket ticket = GlympseFactory.createTicket(duration, message, null);
        for (GInvite recipient : recipientList)
        {
            ticket.addInvite(recipient);
        }

        // Call sendTicket to create the ticket and the recipient URL.
        _glympse.sendTicket(ticket);

        // Return the ticket.
        return ticket;
    }
}
