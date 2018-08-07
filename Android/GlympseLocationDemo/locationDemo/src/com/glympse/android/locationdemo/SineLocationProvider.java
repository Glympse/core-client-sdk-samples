//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

package com.glympse.android.locationdemo;

import java.util.*;
import com.glympse.android.api.*;
import com.glympse.android.core.*;
import com.glympse.android.lib.*;
import com.glympse.android.lib.json.*;
import com.glympse.android.hal.*;
    
/*O*/public/**/ class SineLocationProvider implements GLocationProvider  
{
    private GHandler _handler;
    
    private GLocation _origin;
    
    private GLocationListener _locationListener;
    
    private GLocation _location;
    
    private boolean _started; 
    
    private long _createdAt;
    
    private Runnable _timer;
    
    public SineLocationProvider(GHandler handler)
    {    
        _handler = handler;
        _origin = CoreFactory.createLocation(0, 47.620635, -122.349254,
            Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
        _started = false;
        _createdAt = Concurrent.getTime();
    }
    
    public SineLocationProvider(GHandler handler, GLocation origin)
    {
        _handler = handler;
        _origin = origin;
        _started = false;
        _createdAt = Concurrent.getTime();
    }
    
    public void start()
    {
        if ( _started )
        {
            return;
        }
        _started = true;
        _timer = new LocationTimer(Helpers.wrapThis(this));
        _handler.postDelayed(_timer, 1000);
    }
    
    public void stop()
    {
        if ( !_started )
        {
            return;
        }
        _started = false;
        if ( _timer != null )
        {
            _handler.cancel(_timer);
            _timer = null;
        }
    }
    
    public boolean isStarted()
    {
        return _started;
    }
    
    public GLocation getLastKnownLocation()
    {
        return _location;
    }
    
    public void setLocationListener(GLocationListener locationListener)
    {
        _locationListener = locationListener;
    }
    
    public void applyProfile(GLocationProfile profile)
    {
        // Not implemented by design.
    }
        
    public void locationUpdated()
    {
        if ( !_started )
        {
            _timer = null;
            return;
        }
        if ( _locationListener != null )
        {
            long currentTime = Concurrent.getTime(); 
            double x = ((double)(currentTime - _createdAt))*3.14159625/21000.0;
            double latitude = _origin.getLatitude() + Math.sin(x) / 1000.0;
            double longitude = _origin.getLongitude() + x / 1000.0;
            float course = 0.0f; 
            _location = new Location(
                currentTime,
                latitude, longitude,
                10.0f, course, 0.0f, 3.0f,3.0f);            
            _locationListener.locationChanged(_location);            
        }
        if ( _timer == null )
        {
            _timer = new LocationTimer(Helpers.wrapThis(this));
        }
        _handler.postDelayed(_timer, 1000);        
    }
    
    public /*J*/static/**/ class LocationTimer implements Runnable 
    {
        private SineLocationProvider _locationProvider;
        
        public LocationTimer(SineLocationProvider locationProvider)
        {
            _locationProvider = locationProvider;
        }
        
        public void run()
        {
            _locationProvider.locationUpdated();
        }
    };
};
    


