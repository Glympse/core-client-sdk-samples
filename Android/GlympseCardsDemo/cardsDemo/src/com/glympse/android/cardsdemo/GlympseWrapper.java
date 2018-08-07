//------------------------------------------------------------------------------
//
// Copyright (c) 2017 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

package com.glympse.android.cardsdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.core.GPrimitive;
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

    public void start(Context context, GPrimitive profile)
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

            // Configure Glympse to enable cards
            _glympse.getConfig().setCardsEnabled(true);

            if ( null != profile )
            {
                _glympse.login(profile);
            }

            // Start Glympse platform
            _glympse.start();
        }
    }

    public void stop(boolean logout)
    {
        if (null != _glympse)
        {
            // Shutdown the Glympse API.
            _glympse.stop();

            if ( logout )
            {
                _glympse.logout();
            }
            
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

    public void restoreAccount(Context context, GPrimitive account)
    {
        stop(true);
        start(context, account);
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
                GlympseWrapper.instance().start(context, null);

                // Deactivate platform right away.
                GlympseWrapper.instance().getGlympse().setActive(false);
            }
        }
    }
}
