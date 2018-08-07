package com.glympse.android.triggersdemo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.glympse.android.api.GC;
import com.glympse.android.api.GPlace;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GTrigger;
import com.glympse.android.api.GTriggersManager;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.controls.GTimePickerFragment;
import com.glympse.android.controls.GTimePickerFragment.OnConfirmedListener;
import com.glympse.android.core.CC;
import com.glympse.android.hal.Helpers;
import com.glympse.android.triggersdemo.RecipientHelper.AddressData;
import com.glympse.android.triggersdemo.RecipientHelper.RecipientListener;
import com.glympse.android.triggersdemo.controls.GeofenceOverlay;

public class AddTriggerActivity extends FragmentActivity implements OnConfirmedListener, 
RecipientListener {
	
	private static final int REQUEST_PICK_RECIPIENT  = 700;
		
	private GoogleMapFragment _mapFragment;
	private GeofenceOverlay _fenceOverlay;
	
	// Ticket properties
	private long   _durationMs;
	private String _recipientAddress;
	
	@Override protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trigger);
        _durationMs = Helpers.MS_PER_MINUTE; // default ticket duration is 1 minute
        _mapFragment = (GoogleMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        _fenceOverlay = (GeofenceOverlay) findViewById(R.id.fence_overlay);
    }
    
    @Override protected void onDestroy()
    {
        super.onDestroy();
    }
    
    @Override public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.trigger_config, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) 
    {
        int id = item.getItemId();
        if ( id == R.id.action_save_trigger ) 
        {
            onSaveTrigger();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
	public void onDurationTap(View v)
	{
	    // Show a G Timer fragment for picking a duration
		GTimePickerFragment timePicker = new GTimePickerFragment();
		timePicker.setDismissOnConfirm(true);
		timePicker.setOnConfirmedListener(this);
		timePicker.setDurationMode((int) Helpers.MS_PER_MINUTE);
		timePicker.show(getSupportFragmentManager(), "time_picker");
	}
	
	public void onRecipientTap(View v)
    {
	    // Open the system's contact picker to select a contact
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_RECIPIENT);
    }
	
	private void onSaveTrigger()
	{
	    // Make sure we have enough data to create this geofence
	    if ( !preSaveCheck() )
	    {
	        return;
	    }
	    
	    // Calculate the radius of the geofence currently displayed on the map
		double radius = round(_fenceOverlay.getRadiusInMeters(_mapFragment.getCenterPoint(), _mapFragment.getZoomLevel()), 0);
		// Create a location based on the center point of the currently displayed map
		GPlace location = GlympseFactory.createPlace(_mapFragment.getCenterPoint().latitude, 
		    _mapFragment.getCenterPoint().longitude, "");
		// Create a ticket to send when the geofence is triggered
		GTicket ticket = GlympseFactory.createTicket((int) _durationMs, null, null);
		ticket.addInvite(GlympseFactory.createInvite(GC.INVITE_TYPE_UNKNOWN, null, _recipientAddress));
		
		GTriggersManager triggersManager = GlympseWrapper.instance().getGlympse().getTriggersManager();
		// Save the onExit trigger
		GTrigger trigger = GlympseFactory.createGeoTrigger(getTriggerName(), false, ticket, 
            location, radius, CC.GEOFENCE_TRANSITION_EXIT);
		triggersManager.addLocalTrigger(trigger);
		
		// Save the onEnter trigger
		trigger = GlympseFactory.createGeoTrigger(getTriggerName(), false, ticket.clone(), 
            location, radius, CC.GEOFENCE_TRANSITION_ENTER);
		triggersManager.addLocalTrigger(trigger);
		
		finish();
	}

	public void onConfirmed(GTimePickerFragment timePickerFragment) 
	{
	    // The user has picked a duration from the G timer. Save the value and update ui
		_durationMs = timePickerFragment.getDuration();
		String duration = Helpers.formatDuration(_durationMs);
		((Button) findViewById(R.id.duration_button)).setText(duration);
	}
	
	private void setRecipientData(AddressData data)
	{
	    // The user has selected a recipient. Save the value and update ui
	    _recipientAddress = data.getAddress();
	    ((Button) findViewById(R.id.recipient_button)).setText(data.getAddress());
	    ((Button) findViewById(R.id.recipient_button)).setTextColor(Color.BLACK);
	}
	
	private String getTriggerName()
	{
	    // Get the trigger name from the EditText field
	    EditText nameField = ((EditText) findViewById(R.id.name));
	    String name = nameField.getText().toString();
	    if ( name.isEmpty() )
	    {
	        // If it's not set, create one based on the current center point of the map
	        StringBuilder sb = new StringBuilder();
	        sb.append(round(_mapFragment.getCenterPoint().latitude, 3));
	        sb.append(", ");
	        sb.append(round(_mapFragment.getCenterPoint().longitude, 3));
	        name = sb.toString();
	    }
	    return name;
	}
	
	// Round a double to the given number of decimal places
	public static double round(double value, int places) 
	{
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	private boolean preSaveCheck()
	{
	    boolean pass = true;
	    // Must at least have a recipient picked.
	    if ( null ==  _recipientAddress )
	    {
	        ((Button) findViewById(R.id.recipient_button)).setTextColor(Color.RED);
	        pass = false;
	    }
	    
	    return pass;
	}
	
	/**
	 * Recipient picker section
	 */
	
	@Override 
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if ( RESULT_OK == resultCode )
        {
            switch (requestCode)
            {
                case REQUEST_PICK_RECIPIENT:
                    if ( null != intent)
                    {
                        RecipientHelper.getAddressData(this, intent.getData(), this);
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, intent);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onRecipientSelected(AddressData data)
    {
        setRecipientData(data);
    }
}
