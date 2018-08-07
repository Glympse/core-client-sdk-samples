//------------------------------------------------------------------------------
//
// Copyright (c) 2012 Glympse Inc.  All rights reserved.  Glympse Confidential.
//
//------------------------------------------------------------------------------

package com.glympse.android.contactsdemo;

import java.util.LinkedList;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.glympse.android.api.GGlympse;

public class GlympseContactsDemoActivity extends FragmentActivity
{
    private static final int REQUEST_CODE_SEND_SMS = 1;
    private static final int REQUEST_CODE_LOCATION = 2;

    // In this example Smart G-Button is hosted by list view items inside ContactAdapter
    private ContactAdapter _contactAdapter;
    private List<ContactItem> _contacts;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
	    try
	    {
	        setContentView(R.layout.activity_test_glympse_button);

	        // Startup the Glympse API and pass in our application key.
	        // This should be done, when application is started. Shared version of 
	        // Glympse platform will be available for all application activities. 
	        GlympseWrapper.instance().start(this);

	        // Configure "Attach" button.
	        Button attach = (Button) findViewById(R.id.attach);
	        attach.setOnClickListener(new AttachButtonListener());
	        
	        // Initialize the contact list
	        ListView contactList = ((ListView) findViewById(R.id.contact_list));
	        _contacts = new LinkedList<ContactItem>();
	        _contactAdapter = new ContactAdapter(contactList, _contacts);
	        contactList.setAdapter(_contactAdapter);

	        // Display version information. 
		    String app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
			int ver_num = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
			String ver = String.format("%s [%d]", app_ver, ver_num);
			TextView version = (TextView) findViewById(R.id.version);
			version.setText(String.format("Glympse Smart Button tester version %s (api version %s)", 
			    ver, GlympseWrapper.instance().apiVersion()));
		}
		catch ( Exception e )
		{
		}
	}
	
    @Override public void onPause()
    {
        super.onPause();
        
        // Deactivate Glympse platform, when application is sent to the background. 
        // It is application's responsibility to track its status and notify Glympse platform on changes. 
        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if ( null != glympse )
        {
            glympse.setActive(false);
        }
    }
    
    @Override public void onResume()
    {
        super.onResume();
        
        // Activate Glympse platform, when application comes to foreground. 
        // It is application's responsibility to track its status and notify Glympse platform on changes.
        GGlympse glympse = GlympseWrapper.instance().getGlympse();
        if ( null != glympse )
        {
            glympse.setActive(true);
        }

        // Request send_sms permission so that we can send Glympse invites from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},
                REQUEST_CODE_SEND_SMS);
        }

        // Request location permissions so we can get location updates from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION);
        }
    }    

	@Override public void onDestroy()
	{
	    // It is required to cleanup Smart G-Button, when host list item 
	    // or activity goes our of scope. 
	    for(ContactItem contact : _contacts)
	    {
	        contact._smartButton.attachGlympse(null);
	    }
		
		super.onDestroy();
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if ( grantResults[0] == PackageManager.PERMISSION_DENIED )
        {
            if ( REQUEST_CODE_LOCATION == requestCode )
            {
                // We need this permission to share location
                finish();
            }
            if ( REQUEST_CODE_SEND_SMS == requestCode )
            {
                // Not a big deal. Invites can be sent server-side if needed.
            }
        }
    }

	private class AttachButtonListener implements OnClickListener
    {
        @Override public void onClick(View v)
        {
            TextView nameView = (TextView) findViewById(R.id.name);
            TextView phoneView = (TextView) findViewById(R.id.phone);
            String name = nameView.getText().toString();
            String phone = phoneView.getText().toString();
            if ((phone.length() > 0) && (name.length() > 0))
            {
                _contacts.add(new ContactItem(name, phone));
                _contactAdapter.notifyDataSetChanged();
            }
        }
    }
}
