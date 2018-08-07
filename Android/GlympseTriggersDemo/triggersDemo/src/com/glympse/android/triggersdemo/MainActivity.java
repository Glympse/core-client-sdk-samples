package com.glympse.android.triggersdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.glympse.android.api.GTicket;

import java.util.ArrayList;

public class MainActivity extends FragmentActivity
{
    private static final int REQUEST_CODE_LOCATION = 1;
    private static final int REQUEST_CODE_STORAGE = 2;
    private static final int REQUEST_CODE_CONTACTS = 3;
    private static final int REQUEST_CODE_SMS = 4;

    private ViewPager _viewPager;
    private Button _triggerButton;
    private Button _eventButton;
    
    @Override protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Start the platform. 
        GlympseWrapper.instance().start(this.getApplicationContext());
        GlympseWrapper.instance().setActive(true);
        
        // Setup ui
        _viewPager = (ViewPager) findViewById(R.id.view_pager);
        _viewPager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        _viewPager.setOnPageChangeListener(new PageChangeListener());
        
        _triggerButton = (Button) findViewById(R.id.trigger_button);
        _eventButton = (Button) findViewById(R.id.event_button);
    }
    
    @Override protected void onDestroy()
    {
        super.onDestroy();
    }
    
    @Override protected void onResume()
    {
        super.onResume();

        // Request location permission so that we can get location updates from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION);
        }

        // Request write external storage so we can use Google Maps Fragment
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_STORAGE);
        }

        // Request read contacts permission so we can get recipient emails / phone numbers via the picker
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_CODE_CONTACTS);
        }

        // Request send sms permission so we can send out invites via sms from the client
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},
                REQUEST_CODE_SMS);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) 
    {
        int id = item.getItemId();
        if ( id == R.id.action_expire ) 
        {
            // Expire all running glympses
            expireAll();            
            return true;
        }
        else if ( id == R.id.action_add_trigger )
        {
            // Show the activity for creating triggers
        	showAddTrigger();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if ( grantResults.length == 0 )
        {
            return;
        }

        if ( grantResults[0] == PackageManager.PERMISSION_DENIED )
        {
            if ( REQUEST_CODE_LOCATION == requestCode )
            {
                // If we don't have location permission we can't do anything useful
                finish();
            }
            else if ( REQUEST_CODE_STORAGE == requestCode )
            {
                // If we don't have this permission we can't show our map.
                finish();
            }
            else if ( REQUEST_CODE_CONTACTS == requestCode )
            {
                // If we don't have this permission we can't get recipient email / phone numbers
                finish();
            }
            else if ( REQUEST_CODE_SMS == requestCode )
            {
                // It's ok if we don't get this permission since invites can be send by the server.
            }
        }
    }
    
    /**
     * Menu actions 
     */
    
    private void expireAll()
    {
        for ( GTicket ticket : GlympseWrapper.instance().getGlympse().getHistoryManager().getTickets() )
        {
            if ( ticket.isActive() )
            {
                ticket.expire();
            }
            else
            {
                break;
            }
        }
    }

	private void showAddTrigger() 
	{
		Intent intent = new Intent(this, AddTriggerActivity.class);
		startActivity(intent);
	}
	
	/**
	 * ViewPager and buttons section
	 */
	
	public void onTriggerTap(View v)
	{
		_viewPager.setCurrentItem(0);
		_triggerButton.setEnabled(false);
		_eventButton.setEnabled(true);
	}
	
	public void onEventLogTap(View v)
	{
		_viewPager.setCurrentItem(1);
		_eventButton.setEnabled(false);
		_triggerButton.setEnabled(true);
	}
    
    class PageAdapter extends FragmentPagerAdapter
    {
    	private ArrayList<Fragment> _fragments;
    	
		public PageAdapter(FragmentManager fm) 
		{
			super(fm);
			_fragments = new ArrayList<Fragment>(2);
			_fragments.add(new TriggersListFragment());
			_fragments.add(new EventLogFragment());
		}

		@Override
		public int getCount() 
		{
			return _fragments.size();
		}

		@Override
		public Fragment getItem(int item) 
		{
			return _fragments.get(item);
		}
    }
    
    class PageChangeListener extends SimpleOnPageChangeListener
    {
    	@Override
    	public void onPageSelected(int position)
    	{
    		if ( position == 0 )
    		{
    			_triggerButton.setEnabled(false);
    			_eventButton.setEnabled(true);
    		}
    		else
    		{
    			_triggerButton.setEnabled(true);
    			_eventButton.setEnabled(false);
    		}
    	}
    }
}
