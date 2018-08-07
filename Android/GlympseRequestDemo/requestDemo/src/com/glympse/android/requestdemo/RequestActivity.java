package com.glympse.android.requestdemo;

import java.util.UUID;

import com.glympse.android.api.GC;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GlympseFactory;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class RequestActivity extends Activity 
{
    private static final int REQUEST_CODE_PHONE_STATE = 1;
    private static final int REQUEST_CODE_SMS = 2;

    private EditText _phoneEdit;
    private TextView _groupNametext;
        
    private String _groupName;
    private String _selfPhoneNumber;
    private GTicket _sink;

    @Override protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        
        // Initialize Glympse platform.
        GlympseWrapper.instance().start(this);
        
        // Generate group name.        
        _groupName = restoreGroupName();

        // Request location permission so that we can get location updates from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},
                REQUEST_CODE_PHONE_STATE);
        }
        else
        {
            getSelfPhoneNumber();
        }
        
        // Initialize UI. 
        _phoneEdit = (EditText)findViewById(R.id.edit_request_recipient);
        _groupNametext = (TextView)findViewById(R.id.text_group_name);        
        _groupNametext.setText("http://" + GlympseWrapper.BASE_URL + "/" + _groupName);
    }
    
    @Override public void onPause()
    {
        super.onPause();
        
        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if ( null != glympse )
        {
            glympse.setActive(false);
        }
    }
    
    @Override public void onResume()
    {
        super.onResume();
        
        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if ( null != glympse )
        {
            glympse.setActive(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if ( grantResults[0] == PackageManager.PERMISSION_GRANTED )
        {
            if ( REQUEST_CODE_PHONE_STATE == requestCode )
            {
                getSelfPhoneNumber();
            }
            else if ( REQUEST_CODE_SMS == requestCode )
            {
                // Send the request.
                GlympseWrapper.instance().getGlympse().requestTicket(_sink);
            }
        }
    }

    private void getSelfPhoneNumber()
    {
        // Get self phone number.
        // NOTE: This may not work under certain circumstances. Do not depend on that in production code.
        TelephonyManager telephoneNumber = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        _selfPhoneNumber = telephoneNumber.getLine1Number();
    }
    
    public static final String PREFS_NAME = "com.glympse.android.requestdemo.PREFS";
    
    private String restoreGroupName()
    {      
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String groupName = preferences.getString("group_name", null);
        if ( null == groupName )
        {
            groupName = "!" + UUID.randomUUID().toString();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("group_name", groupName);
            editor.commit();
        }
        return groupName;
    }

    public void sendRegularRequest(View v)
    {
        // Check input.
        String phoneNumber = _phoneEdit.getText().toString().trim();
        if ( phoneNumber.length() == 0 )
        {
            alert(R.string.error_invalid_number);
            return;
        }           
        
        // Create invite, which represents self user. 
        // This invite will be used to send ticket to requester. 
        GInvite selfInvite = GlympseFactory.createInvite(GC.INVITE_TYPE_SMS, null, _selfPhoneNumber);
        
        // Create ticket object. 
        int duration = 3600000;
        GTicket request = GlympseFactory.createTicket(duration, null, null);
        request.addInvite(selfInvite);

        // Create invite, which represents recipient of the request. 
        // This invite will be used to send ticket request.
        GInvite requestInvite = GlympseFactory.createInvite(GC.INVITE_TYPE_SMS, null, phoneNumber);

        // Create a container ticket to act as a sink to listen for events on.
        _sink = GlympseFactory.createRequest(request, requestInvite);

        // Subscribe on ticket events.
        // NOTE: To implement this implement the GEventListenerInterface on this class.
		// _sink.addListener(this);

        // Request SMS permission so that we can send the request from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},
                REQUEST_CODE_SMS);
        }
        else
        {
            // Send the request.
            GlympseWrapper.instance().getGlympse().requestTicket(_sink);
        }
    }    
    
    public void sendGroupRequest(View v)
    {
        // Check input.
        String phoneNumber = _phoneEdit.getText().toString().trim();
        if ( phoneNumber.length() == 0 )
        {
            alert(R.string.error_invalid_number);
            return;
        }        
        
        // Create invite, which represents self user. 
        // This invite will be used to send ticket to requester. 
        GInvite selfInvite = GlympseFactory.createInvite(GC.INVITE_TYPE_GROUP, null, _groupName);
        
        // Create ticket object. 
        int duration = 3600000;
        GTicket request = GlympseFactory.createTicket(duration, null, null);
        request.addInvite(selfInvite);

        // Create invite, which represents recipient of the request. 
        // This invite will be used to send ticket request.
        GInvite requestInvite = GlympseFactory.createInvite(GC.INVITE_TYPE_SMS, null, phoneNumber);

        // Create a container ticket to act as a sink to listen for events on.
        _sink = GlympseFactory.createRequest(request, requestInvite);

        // Subscribe on ticket events.
        // NOTE: To implement this implement the GEventListenerInterface on this class.
		// _sink.addListener(this);


        // Request SMS permission so that we can send the request from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},
                REQUEST_CODE_SMS);
        }
        else
        {
            // Send the request.
            GlympseWrapper.instance().getGlympse().requestTicket(_sink);
        }
    }
    
    private void alert(int id)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(id);
        alert.show();
    }
}

