package com.glympse.android.triggersdemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GLocationManager;
import com.glympse.android.api.GUserManager;
import com.glympse.android.core.GLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

public class GoogleMapFragment extends SupportMapFragment implements GEventListener, OnMapReadyCallback
{
    private GoogleMap _map;
    private GLocationManager _locationManager;
    private GUserManager _userManager;
    private GLocation _myLocation;
    
    private int DEFAULT_ZOOM = 13;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        getMapAsync(this);
        return view;
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
        startLocationManager();
    }
    
    @Override
    public void onStop()
    {
        super.onStop();
        stopLocationManager();
    }
    
    public LatLng getCenterPoint()
    {
        return _map.getCameraPosition().target;
    }
    
    public float getZoomLevel()
    {
        return _map.getCameraPosition().zoom;
    }
    
    private void startLocationManager()
    {
        if ( null == _locationManager )
        {
            _locationManager = GlympseWrapper.instance().getGlympse().getLocationManager();
            _userManager = GlympseWrapper.instance().getGlympse().getUserManager();
            // Subscribe on location events
            _locationManager.addListener(this);
            // If we don't have our location, start listening for updates
            if ( _locationManager.getLocation() == null && 
                _userManager.getSelf().getLocation() == null )
            {
                _locationManager.startLocation();
            }
            else
            {
                zoomToSelf();
            }
        }
    }
    
    private void stopLocationManager()
    {
        if ( null != _locationManager )
        {
            // Unsubscribe from location events
            _locationManager.removeListener(this);
            _locationManager.stopLocation(false);
            _locationManager = null;
            _userManager = null;
        }
    }
    
    private void zoomToSelf()
    {
        if ( null != _locationManager && null != _map )
        {
            // Update the camera to point at our location
            _myLocation = _locationManager.getLocation();
            if ( _myLocation == null )
            {
                _myLocation = _userManager.getSelf().getLocation();
            }
            if ( _myLocation != null )
            {
                LatLng newLocation = new LatLng(_myLocation.getLatitude(), _myLocation.getLongitude());
                _map.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, DEFAULT_ZOOM));
            }
        }
    }

    @Override
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( listener == GE.LISTENER_LOCATION )
        {
            if ( 0 != (GE.LOCATION_STATE_CHANGED & events) )
            {
                zoomToSelf();
                //stopLocationManager();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        // Setup map and customize its ui
        _map = googleMap;
        UiSettings uiSettings = _map.getUiSettings();
        uiSettings.setTiltGesturesEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        zoomToSelf();
    }
}
