//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.createdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.glympse.android.api.GC;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.hal.Helpers;

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

            // Start Glympse platform right away.
            _glympse.start();

            // Glympse functionality will be limited until consent is provided via the Consent Manager.
            // See ConsentDialog class for proper handling of yes/no responses.
            ConsentDialog.showIfNeeded(_glympse, context);
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
            String action = intent.getAction();
            if (Helpers.isEmpty(action))
            {
                return;
            }

            if (action.equals("com.glympse.android.hal.service.STARTED"))
            {
                // Instantiate and start Glympse platform.
                GlympseWrapper.instance().start(context);

                // Deactivate platform right away.
                GlympseWrapper.instance().getGlympse().setActive(false);
            }
        }
    }
    
    public GTicket createGlympse(int duration)
    {
        if (_glympse == null)
        {
            return null;
        }

        // Create the ticket for the given duration.
        GTicket ticket = GlympseFactory.createTicket(duration, null, null);

        // For the recipient list, we create a single "LINK" recipient. This
        // means we want a recipient URL for the new Glympse without having
        // the Glympse API actually send the invite out to anyone.
        GInvite recipient = GlympseFactory.createInvite(GC.INVITE_TYPE_LINK, null, null);
        ticket.addInvite(recipient);

        // Call sendTicket to create the ticket and the recipient URL.
        _glympse.sendTicket(ticket);

        // Return the ticket.
        return ticket;
    }
}
