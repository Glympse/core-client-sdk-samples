package com.glympse.android.glympsemapdemodestinations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GPlace;
import com.glympse.android.api.GUser;
import com.glympse.android.api.GUserTicket;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.controls.map.glympsemap.GlympseMapFragment;
import com.glympse.android.core.CoreConstants;
import com.glympse.android.core.CoreFactory;
import com.glympse.android.core.GDrawable;
import com.glympse.android.core.GPrimitive;

import com.glympse.android.glympsemapdemodestinations.R;
import com.glympse.android.map.GMapLayer;
import com.glympse.android.map.GMapLayerConversation;
import com.glympse.android.map.GMapLayerConversationListener;
import com.glympse.android.map.GMapLayerPlaces;
import com.glympse.android.map.GMapLayerPlacesListener;
import com.glympse.android.map.GMapManager;
import com.glympse.android.map.MapConstants;
import com.glympse.android.map.MapFactory;

public class MainActivity extends FragmentActivity implements GMapLayerPlacesListener
{
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private Button _buttonAddPlaces;
	
	private GMapLayerPlaces _mapLayerPlaces;

	private GlympseMapFragment _mapFragment;
	
	private GPlace _placeSpaceNeedle;
	private GPlace _placePioneerSquare;
	private GPlace _placeSeattleLibrary;
	private GPlace _placePikePlaceMarket;
	private GPlace _placeCenturyLinkField;
	private GPlace _placeSafecoField;
	private GPlace _placeSwedishMedicalCenter;

	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		GlympseWrapper.instance().createGlympse(MainActivity.this);
		GlympseWrapper.instance().getGlympse().setEtaMode(GC.ETA_MODE_INTERNAL);
		GlympseWrapper.instance().getGlympse().setRestoreHistory(false);
		GlympseWrapper.instance().getGlympse().start();

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

        _buttonAddPlaces = (Button)this.findViewById(R.id.buttonAddPlaces);
        _buttonAddPlaces.setOnClickListener(new OnClickListener()
        {
            @Override public void onClick(View v)
            {
                // Create a conversation layer and add it to the manager's list
                GMapManager manager = _mapFragment.getMapManager();

                _mapLayerPlaces = MapFactory.createMapLayerPlaces();
                manager.addMapLayer(_mapLayerPlaces);

//				BitmapDrawable selectedBitmap = (BitmapDrawable) getResources().getDrawable(R.drawable.logo_selected);
//				GDrawable selectedDrawable = CoreFactory.createDrawable(selectedBitmap);
//
//				BitmapDrawable unselectedBitmap = (BitmapDrawable) getResources().getDrawable(R.drawable.logo_unselected);
//				GDrawable unselectedDrawable = CoreFactory.createDrawable(unselectedBitmap);
//
//				_mapLayerPlaces.setPlacesStateDrawable(MapConstants.PLACE_STATE_DRAWABLE_SELECTED, selectedDrawable);
//				_mapLayerPlaces.setPlacesStateDrawable(MapConstants.PLACE_STATE_DRAWABLE_UNSELECTED, unselectedDrawable);

                _mapLayerPlaces.enableDroppedPin();
                _mapLayerPlaces.setMapLayerPlacesListener(MainActivity.this);

                _placeSpaceNeedle = GlympseFactory.createPlace(47.6204, -122.3491, "Space Needle");
                _placePioneerSquare = GlympseFactory.createPlace(47.6, -122.332, "Pioneer Square");
                _placeSeattleLibrary = GlympseFactory.createPlace(47.606667, -122.332778, "Seattle Public Library");
                _placePikePlaceMarket = GlympseFactory.createPlace(47.609425, -122.3417, "Pike Place Market");
                _placeCenturyLinkField = GlympseFactory.createPlace(47.5952, -122.3316, "Century Link Field");
                _placeSafecoField = GlympseFactory.createPlace(47.591389, -122.3325, "Safeco Field");

                // A place with no title will not show a title bubble
                _placeSwedishMedicalCenter = GlympseFactory.createPlace(47.608984, -122.32182, "");

                _mapLayerPlaces.addPlace(_placeSpaceNeedle);
                _mapLayerPlaces.addPlace(_placePioneerSquare);
                _mapLayerPlaces.addPlace(_placeSeattleLibrary);

                GPrimitive style = CoreFactory.createPrimitive(CoreConstants.PRIMITIVE_TYPE_OBJECT);
                style.put("destination_color", "#68a9ea");

                _mapLayerPlaces.addPlace(_placePikePlaceMarket, style);
                _mapLayerPlaces.addPlace(_placeCenturyLinkField, style);
                _mapLayerPlaces.addPlace(_placeSafecoField, style);
                _mapLayerPlaces.addPlace(_placeSwedishMedicalCenter, style);

                _mapLayerPlaces.setFollowingMode(MapConstants.FOLLOWING_MODE_FREE);
            }
        });

        // Attach the Glympse instance to the GlympseMapFragment
        _mapFragment.attachGlympse(GlympseWrapper.instance().getGlympse());
    }

	/**
	 * GMapLayerPlacesListener
	 */
	
	@Override public void pinWasDropped(GMapLayerPlaces layer, GPlace place)
	{
		String message = "Pin was dropped at: " + place.getLatitude() + ", " + place.getLongitude();
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}

	@Override public void placeWasSelected(GMapLayerPlaces layer, GPlace place)
	{
		String message = "Place selected: " + place.getName();
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}

}
