package com.glympse.android.glympsemapdemoconversation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import com.glympse.android.core.GDrawable;
import com.glympse.android.core.GPrimitive;

import com.glympse.android.glympsemapdemoconversation.UserCell.OnUserCellTappedListener;
import com.glympse.android.map.GMapLayerConversation;
import com.glympse.android.map.GMapLayerConversationListener;
import com.glympse.android.map.GMapLayerWorld;
import com.glympse.android.map.GMapLayerWorldListener;
import com.glympse.android.map.GMapLockableLayer;
import com.glympse.android.map.GMapLockableLayerListener;
import com.glympse.android.map.GMapManager;
import com.glympse.android.map.MapConstants;
import com.glympse.android.map.MapFactory;
import com.glympse.android.map.MapRegion;

import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.os.Build;

public class MainActivity extends FragmentActivity implements GEventListener, OnUserCellTappedListener, GMapLayerConversationListener
{
	private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

	private Button _buttonAdd;
	private Button _buttonRemove;
	private EditText _editTextCode;
	private Button _buttonLogo;
	
	private LinearLayout _layoutUserList;

	private GlympseMapFragment _mapFragment;
	private GMapLayerConversation _conversationLayer;

	private GUser _activeUser = null;
	
	private ArrayList<GUser> _users = new ArrayList<GUser>();
	private String[] _colorStrings = { "#ff0000", "#0000ff", "#fd6e8a", "#2c3b63", "#018b2a", "#c48dd1" };
	private int _colorIndex = 0;

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		GlympseWrapper.instance().createGlympse(MainActivity.this);
		GlympseWrapper.instance().getGlympse().setEtaMode(GC.ETA_MODE_INTERNAL);
		GlympseWrapper.instance().getGlympse().setRestoreHistory(false);
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
                // Create the conversation layer
                _conversationLayer = MapFactory.createMapLayerConversation();

                // We will handle tracking of self user ourselves.
                _conversationLayer.setTrackingSelfUserEnabled(false);

                // Set ourselves as the MapLayerConversationListener. This will give us callbacks when a user's annotations are tapped
                _conversationLayer.setMapLayerConversationListener(MainActivity.this);

                // Get a reference to the MapManager
                GMapManager manager = _mapFragment.getMapManager();

                // Optionally set the configuration of the manager
                GPrimitive config = CoreFactory.createPrimitive(CoreConstants.PRIMITIVE_TYPE_OBJECT);
                config.put(MapConstants.CONFIGURATION_SPEED_KEY(), MapConstants.CONFIGURATION_SPEED_MPH());
                manager.setConfiguration(config);

                // Add the world layer to the manager
                manager.addMapLayer(_conversationLayer);

                // Set the following mode of the coversation layer to follow all the users and destinations
                _conversationLayer.setFollowingMode(MapConstants.FOLLOWING_MODE_ALL);

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

        _buttonRemove = (Button) findViewById(R.id.buttonRemove);
        _buttonRemove.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Remove the last added user from the map.
                if( _users.size() > 0 && null != _activeUser)
                {
                    _conversationLayer.removeUser(_activeUser);
                    _users.remove(_activeUser);
                    updateUserBar();
                }
            }
        });

        _buttonLogo = (Button) findViewById(R.id.buttonLogo);
        _buttonLogo.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Create GDrawable from BitmapDrawable
                BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.logo);
                GDrawable drawable = CoreFactory.createDrawable(bitmapDrawable);

                // Set GDrawable on the last user in the list for the normal state (moving)
                if( _users.size() > 0 && null != _activeUser)
                {
                    _conversationLayer.setUserStateDrawable(_activeUser, MapConstants.USER_STATE_DRAWABLE_NORMAL, drawable);
                    _conversationLayer.setUserStateDrawable(_activeUser, MapConstants.USER_STATE_DRAWABLE_NO_HEADING, drawable);
                }

            }
        });
    }
	
	private void updateUserBar()
	{
		// Remove all the user bar list subviews
		_layoutUserList.removeAllViews();
		
		// For each user currenlty on the conversation layer, create a UserCell and add it to the User Bar.
		for(GUser user : _users)
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
		
		// Set the active user on the conversation layer
		_conversationLayer.setFocusedUser(_activeUser);
		
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
				String code = (String) obj;
				GUser user = glympse.getUserManager().findUserByInviteCode(code);
				
				GlympseWrapper.instance().getGlympse().getUserManager().startTracking(user);
				_conversationLayer.addUser(user, getNextStyle());
				_users.add(user);
				
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
	 * GMapLayerConversationListener
	 */

	@Override public void userDestinationWasSelected(GMapLayerConversation layer, GUser user)
	{
		
	}

	@Override public void userWasSelected(GMapLayerConversation layer, GUser user)
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

	private GPrimitive getNextStyle()
	{
		// This cycles through the hard-coded list of color codes and sets that color for each the accepted style values
		GPrimitive style = CoreFactory.createPrimitive(CoreConstants.PRIMITIVE_TYPE_OBJECT);

		String colorString = _colorStrings[_colorIndex % _colorStrings.length];

		// If style options are net set, the defaults will be used
		style.put("icon_color", colorString);
		style.put("trail_color", colorString);
		style.put("destination_color", colorString);
		
		// These control the visibility of different elements
		style.put("icon_visible", true);
		style.put("destination_visible", true);
		style.put("trail_visible", true);
		style.put("route_visible", true);
		
		// Setting this to true will add a label above the user with their nickname and speed
		style.put("user_bubble_visible", true);
		
		_colorIndex++;

		return style;
	}
}
