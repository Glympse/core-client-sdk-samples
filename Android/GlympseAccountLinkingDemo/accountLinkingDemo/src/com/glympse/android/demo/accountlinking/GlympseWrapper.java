//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.demo.accountlinking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.hal.Helpers;

import junit.framework.Assert;

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
     * If you need a key, you can request one by visiting https://developer.glympse.com.
     */        
    public static final String API_KEY = "<< Your API key >>";

    private static GGlympse _glympse = null;

    public void start(Context context)
    {
        if ( _glympse == null )
        {
            createGlympse(context);
        }

        // Mark this application as exempt from needing user consent (since this is a dev app)
        // See GlympseCreateDemo for an example of handling user consent
        _glympse.getConsentManager().exemptFromConsent(true);
        
        // Start Glympse platform right away.
        _glympse.start();
    }

    public void stop()
    {
        if ( null != _glympse )
        {
            // Shutdown the Glympse API.
            _glympse.stop();
            
            // Perform required cleanup.
            clear();
        }
    }
    
    public void createGlympse(Context context)
    {
        if ( _glympse == null )
        {
            if ( API_KEY.equals("<< Your API key >>"))
            {
                Log.d("GlympseWrapper", "UNABLE TO RUN DEMO: You must pass the Glympse platform a valid API key.");
                Assert.fail("Invalid API key");
            }
            
            _glympse = GlympseFactory.createGlympse(context, BASE_URL, API_KEY);
        }
    }
    
    public void setActive(boolean active)
    {
        if ( _glympse != null )
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
        if ( null == _instance )
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
            if ( Helpers.isEmpty(action) )
            {
                return;
            }

            if ( action.equals("com.glympse.android.hal.service.STARTED") )
            {
                // Instantiate and start Glympse platform.
                GlympseWrapper.instance().start(context);
            }
        }
    }
}
