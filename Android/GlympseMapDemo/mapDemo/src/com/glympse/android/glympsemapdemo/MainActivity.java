package com.glympse.android.glympsemapdemo;

import java.util.LinkedList;
import java.util.List;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GUser;
import com.glympse.android.api.GUserTicket;
import com.glympse.android.controls.map.glympsemap.GlympseMapFragment;
import com.glympse.android.controls.map.glympsemap.OnMapAvailableListener;

import com.glympse.android.core.CoreConstants;
import com.glympse.android.core.CoreFactory;
import com.glympse.android.core.GArray;
import com.glympse.android.core.GPrimitive;
import com.glympse.android.glympsemapdemo.UserCell.OnUserCellTappedListener;
import com.glympse.android.map.GMapLayerWorld;
import com.glympse.android.map.GMapLayerWorldListener;
import com.glympse.android.map.GMapLockableLayer;
import com.glympse.android.map.GMapLockableLayerListener;
import com.glympse.android.map.GMapManager;
import com.glympse.android.map.MapConstants;
import com.glympse.android.map.MapFactory;
import com.glympse.android.map.MapRegion;


public class MainActivity extends FragmentActivity implements GEventListener, GMapLayerWorldListener, OnUserCellTappedListener, GMapLockableLayerListener
{
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private Button _buttonAdd;
	private EditText _editTextCode;
	private Button _buttonFollowUser;
	private Button _buttonFollowUserAndDestination;
	private Button _buttonFollowAll;
	private ImageButton _buttonLock;
	private LinearLayout _layoutUserList;

	private GlympseMapFragment _mapFragment;
	private GMapLayerWorld _worldLayer;

	private GUser _activeUser = null;
	
	private boolean _isFollowingLocked = false;

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		GlympseWrapper.instance().createGlympse(MainActivity.this);
		GlympseWrapper.instance().getGlympse().setRestoreHistory(false);
		GlympseWrapper.instance().getGlympse().setEtaMode(GC.ETA_MODE_INTERNAL);
		GlympseWrapper.instance().getGlympse().start();
		GlympseWrapper.instance().getGlympse().addListener(MainActivity.this);

		_editTextCode = (EditText) this.findViewById(R.id.editTextCode);
		_layoutUserList = (LinearLayout) this.findViewById(R.id.layoutUserList);

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

		GlympseWrapper.instance().getGlympse().getLocationManager().stopLocation(true);
		GlympseWrapper.instance().setActive(false);
	}

	@Override protected void onResume()
	{
		super.onResume();
		GlympseWrapper.instance().setActive(true);
		GlympseWrapper.instance().getGlympse().getLocationManager().startLocation();
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
                // Create a World layer
                _worldLayer = MapFactory.createMapLayerWorld();

                // Set ourselves as the MapLayerWorldListener. This will give us callbacks when a user's annotations are tapped
                _worldLayer.setMapLayerWorldListener(MainActivity.this);

                // Get a reference to the MapManager
                GMapManager manager = _mapFragment.getMapManager();

                // Optionally set the configuration of the manager
                GPrimitive config = CoreFactory.createPrimitive(CoreConstants.PRIMITIVE_TYPE_OBJECT);
                config.put(MapConstants.CONFIGURATION_SPEED_KEY(), MapConstants.CONFIGURATION_SPEED_MPH());
                manager.setConfiguration(config);

                // Add the world layer to the manager
                manager.addMapLayer(_worldLayer);

                // Set the map to a custom region
                manager.setMapRegion(new MapRegion(37.229235, -122.420654, 37.667516, -121.772461));

                // To begin with, we will set the self user as the active user.
                // When the Platform syncs with the server, we will update the user bar.
                setActiveUser(GlympseWrapper.instance().getGlympse().getUserManager().getSelf());
            }
        });

        // Attach the Glympse instance to the GlympseMapFragment
        _mapFragment.attachGlympse(GlympseWrapper.instance().getGlympse());

        _buttonAdd = (Button) findViewById(R.id.buttonAdd);
        _buttonAdd.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Decode the entered invite. The corresponding user will show up on the World Layer once the invite has been decoded
                String code = _editTextCode.getText().toString().trim();
                GlympseWrapper.instance().getGlympse().decodeInvite(code, GC.INVITE_MODE_PROMPT_BEFORE_VIEWING).addListener(MainActivity.this);
            }
        });

        _buttonFollowUser = (Button) findViewById(R.id.buttonFollowUser);
        _buttonFollowUser.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Set the following mode of the world layer to follow just the active user, if we have set one.
                _worldLayer.setFollowingMode(MapConstants.FOLLOWING_MODE_USER);
            }
        });

        _buttonFollowUserAndDestination = (Button) findViewById(R.id.buttonFollowUserAndDestination);
        _buttonFollowUserAndDestination.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Set the following mode of the world layer to follow just the active user and their destination, if we have set one.
                _worldLayer.setFollowingMode(MapConstants.FOLLOWING_MODE_USER_AND_DESTINATION);
            }
        });

        _buttonFollowAll = (Button) findViewById(R.id.buttonFollowAll);
        _buttonFollowAll.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Set the following mode of the world layer to follow all the users and destinations
                _worldLayer.setFollowingMode(MapConstants.FOLLOWING_MODE_ALL);
            }
        });

        _buttonLock = (ImageButton) findViewById(R.id.buttonLock);
        _buttonLock.setOnClickListener(new View.OnClickListener()
        {
            @Override public void onClick(View v)
            {
                if( _isFollowingLocked )
                {
                    _worldLayer.unlockItems();

                    _buttonLock.setImageResource(R.drawable.img_unlocked);
                    _isFollowingLocked = false;
                }
                else
                {
                    _worldLayer.setMapLockableLayerListener(MainActivity.this);
                    _worldLayer.lockItems();

                    _buttonLock.setImageResource(R.drawable.img_locked);
                    _isFollowingLocked = true;
                }
            }
        });
    }
	
	private void updateUserBar()
	{
		// Remove all the user bar list subviews
		_layoutUserList.removeAllViews();
		
		// For each user registered on the system, create a UserCell and add it to the User Bar. This will include the self user
		GArray<GUser> users = GlympseWrapper.instance().getGlympse().getUserManager().getUsers();
		for(GUser user : users)
		{
			// Create the UserCell
			UserCell userCell = buildUserCellForUser(user);
			
			// Register for a callback when a UserCell is tapped.
			userCell.setOnUserCellTappedListener(this);
			
			// Add the UserCell to the User Bar
			_layoutUserList.addView(userCell);
			
			// All the UserCells start out in the inactive state. For the _active user, we need to set them to the active state.
			if( user == _activeUser )
			{
				userCell.setActiveState(true);
			}
		}
	}
	
	private UserCell buildUserCellForUser(GUser user)
	{
		// Create the UserCell and set the user
		UserCell userCell = new UserCell(getBaseContext());
		userCell.setUser(user);
		return userCell;
	}
	
	private void setActiveUser(GUser user)
	{
		// Set our local field
		_activeUser = user;
		
		// Set the active user on the world layer
		_worldLayer.setActiveUser(user);
		
		// Update
		updateUserBar();
	}

	/**
	 * GEventListener
	 */
	
	@Override public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
	{
		if ( GE.LISTENER_INVITE == listener )
		{
			if ( 0 != (events & GE.INVITE_DECODED) )
			{
				// The invite has been decoded
				updateUserBar();	
			}
		}
		if ( GE.LISTENER_PLATFORM == listener )
		{
			if ( 0 != (events & GE.PLATFORM_INVITE_TICKET) )
			{
				// The ticket invite was decoded, tell the system to view it
				GUserTicket userTicket = (GUserTicket) obj;
				GlympseWrapper.instance().getGlympse().viewTicket(userTicket);
				GlympseWrapper.instance().getGlympse().getUserManager().startTracking(userTicket.getUser());
			}
			if ( 0 != (events & GE.PLATFORM_USER_ADDED) )
			{
				// A user was added to the system
				GUser user = (GUser) obj;
				updateUserBar();
			}
			if ( 0 != (events & GE.PLATFORM_USER_REMOVED) )
			{
				// A user was removed from the system
				GUser user = (GUser) obj;
				if ( user == _activeUser )
				{
					setActiveUser(null);
				}
				updateUserBar();
			}
			if ( 0 != (events & GE.PLATFORM_SYNCED_WITH_SERVER) )
			{
				// Platform was synced with the server. The self user's details are now available
				updateUserBar();
			}
		}
	}
	
	/**
	 * GMapLayerWorldListener
	 */

	@Override public void userDestinationWasSelected(GMapLayerWorld layer, GUser user)
	{
		
	}

	@Override public void userWasSelected(GMapLayerWorld layer, GUser user)
	{
		setActiveUser(user);
	}

	/**
	 * OnUserCellTappedListener
	 */
	
	@Override public void onUserCellTapped(UserCell userCell)
	{
		setActiveUser(userCell.getUser());
	}

	/**
	 * GMapLockableLayerListener
	 */
	@Override public void lockWasBroken(GMapLockableLayer arg0)
	{
		_buttonLock.setImageResource(R.drawable.img_unlocked);
		_isFollowingLocked = false;
	}
}
