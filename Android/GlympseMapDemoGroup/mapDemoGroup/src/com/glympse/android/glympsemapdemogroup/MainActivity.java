package com.glympse.android.glympsemapdemogroup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GGroup;
import com.glympse.android.api.GUser;
import com.glympse.android.api.GUserTicket;
import com.glympse.android.controls.map.glympsemap.GlympseMapFragment;
import com.glympse.android.controls.map.glympsemap.OnMapAvailableListener;

import com.glympse.android.core.CoreConstants;
import com.glympse.android.core.CoreFactory;
import com.glympse.android.core.GArray;
import com.glympse.android.core.GDrawable;
import com.glympse.android.core.GPrimitive;

import com.glympse.android.map.GMapLayer;
import com.glympse.android.map.GMapLayerConversation;
import com.glympse.android.map.GMapLayerConversationListener;
import com.glympse.android.map.GMapLayerGroup;
import com.glympse.android.map.GMapLayerGroupListener;
import com.glympse.android.map.GMapLayerWorld;
import com.glympse.android.map.GMapLayerWorldListener;
import com.glympse.android.map.GMapLockableLayer;
import com.glympse.android.map.GMapLockableLayerListener;
import com.glympse.android.map.GMapManager;
import com.glympse.android.map.MapConstants;
import com.glympse.android.map.MapFactory;
import com.glympse.android.map.MapRegion;
import com.glympse.android.map.GUserNameProvider;

public class MainActivity extends FragmentActivity implements GEventListener, GMapLayerGroupListener
{
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private Button _buttonViewGroup;
	private EditText _editTextGroupName;
	private GlympseMapFragment _mapFragment;
	private GMapLayerGroup _groupLayer;

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		GlympseWrapper.instance().createGlympse(MainActivity.this);
		GlympseWrapper.instance().getGlympse().setRestoreHistory(false);
		GlympseWrapper.instance().getGlympse().start();
		GlympseWrapper.instance().getGlympse().addListener(MainActivity.this);
		GlympseWrapper.instance().getGlympse().getGroupManager().addListener(MainActivity.this);
		
		_editTextGroupName = (EditText) this.findViewById(R.id.editTextGroupName);

        List<String> requiredPermissions = new LinkedList<String>();
        // Request location permission so that we can get location updates from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) )
        {
            requiredPermissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            requiredPermissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) )
        {
            requiredPermissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if ( requiredPermissions.isEmpty() )
        {
            initMap();
        }
        else
        {
            // Request any permissions that we require to properly show the map
            ActivityCompat.requestPermissions(this,
                requiredPermissions.toArray(new String[requiredPermissions.size()]),
                REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
	}

	@Override protected void onPause()
	{
		super.onPause();
		GlympseWrapper.instance().setActive(false);
	}

	@Override protected void onResume()
	{
		super.onResume();
		GlympseWrapper.instance().setActive(true);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if ( REQUEST_CODE_REQUIRED_PERMISSIONS == requestCode )
		{
			// Make sure we were granted all permissions we asked for
			boolean denied = false;
			for ( int i = 0; i < grantResults.length; i++ )
			{
				if ( grantResults[i] == PackageManager.PERMISSION_DENIED )
				{
					denied = true;
					break;
				}
			}
			// If so, initialize our map
			if ( !denied )
			{
				initMap();
			}
			else
			{
				finish();
			}
		}
	}

    private void initMap()
    {
        _mapFragment = (GlympseMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.map_fragment);

        _mapFragment.setOnMapAvailableListener(new OnMapAvailableListener()
        {
            @Override public void onMapAvailable()
            {
                // Create a Group layer
                _groupLayer = MapFactory.createMapLayerGroup();

                // Set ourselves as the MapLayerGroupListener. This will give us callbacks when a user's annotations are tapped
                _groupLayer.setMapLayerGroupListener(MainActivity.this);

                // Get a reference to the MapManager
                GMapManager manager = _mapFragment.getMapManager();

                // Optionally set the configuration of the manager
                GPrimitive config = CoreFactory.createPrimitive(CoreConstants.PRIMITIVE_TYPE_OBJECT);
                config.put(MapConstants.CONFIGURATION_SPEED_KEY(), MapConstants.CONFIGURATION_SPEED_MPH());
                manager.setConfiguration(config);

                // Add the group layer to the manager
                manager.addMapLayer(_groupLayer);

                // Set the map to a custom region
                manager.setMapRegion(new MapRegion(37.229235, -122.420654, 37.667516, -121.772461));
            }
        });

        // Attach the Glympse instance to the GlympseMapFragment
        _mapFragment.attachGlympse(GlympseWrapper.instance().getGlympse());

        _buttonViewGroup = (Button) findViewById(R.id.buttonViewGroup);
        _buttonViewGroup.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                String groupName = _editTextGroupName.getText().toString().trim();
                GGroup group = GlympseWrapper.instance().getGlympse().getGroupManager().viewGroup(groupName);
                if ( null != group )
                {
                    group.addListener(MainActivity.this);
                }
            }
        });
    }

	/**
	 * GEventListener
	 */
	
	@Override public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
	{
		if ( GE.LISTENER_PLATFORM == listener )
		{


		}
		else if ( GE.LISTENER_GROUPS == listener )
		{
			if ( 0 != (events & GE.GROUPS_GROUP_ADDED) )
			{
				GGroup group = (GGroup)obj;
				GlympseWrapper.instance().getGlympse().getGroupManager().startTracking(group);
				_groupLayer.setGroup(group);
			}
			if ( 0 != (events & GE.GROUPS_GROUP_REMOVED) )
			{
				GGroup group = (GGroup)obj;
				GlympseWrapper.instance().getGlympse().getGroupManager().stopTracking(group);
				_groupLayer.setGroup(null);
			}
		}
        if ( GE.LISTENER_GROUP == listener )
        {
            if ( 0 != ( GE.GROUP_INVALID_CODE & events ) )
            {                 
              
            }
        }
	}


	@Override public void userDestinationWasSelected(GMapLayerGroup layer, GUser user)
	{

	}

	@Override public void userWasSelected(GMapLayerGroup layer, GUser user)
	{
		_groupLayer.setActiveUser(user);
	}
}
