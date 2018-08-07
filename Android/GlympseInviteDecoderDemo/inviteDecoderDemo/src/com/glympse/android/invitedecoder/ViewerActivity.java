package com.glympse.android.invitedecoder;

import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GEventSink;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GInvite;
import com.glympse.android.api.GTicket;
import com.glympse.android.api.GUserTicket;
import com.glympse.android.core.GArray;
import com.glympse.android.lib.GEP;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

public class ViewerActivity extends Activity implements GEventListener
{
    private static final int REQUEST_CODE_LOCATION = 1;

    private EditText _uriEdit;
    private GTicket _ticket;

    @Override protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        
        // Initialize Glympse platform.
        GlympseWrapper.instance().start(this);        
        
        // Initialize UI. 
        _uriEdit = (EditText)findViewById(R.id.edit_invite_uri);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if ( grantResults[0] == PackageManager.PERMISSION_GRANTED )
        {
            if ( REQUEST_CODE_LOCATION == requestCode )
            {
                // Send ticket back to requester.
                GlympseWrapper.instance().getGlympse().sendTicket(_ticket);
            }
        }
    }
    
    public void analyze(View v)
    {
        // Check input.
        String uri = _uriEdit.getText().toString().trim();
        if ( uri.length() == 0 )
        {
            alert(R.string.error_invalid_uri);
            return;
        }         
        
        // Let Glympse API do the job.
        GlympseWrapper.instance().getGlympse().openUrl(uri, GC.INVITE_MODE_PROMPT_BEFORE_VIEWING, null);
    }
    
    public void expireAll(View v)
    {    
        for ( GTicket ticket : GlympseWrapper.instance().getGlympse().getHistoryManager().getTickets() )
        {
            if ( ticket.isActive() )
            {
                ticket.modify(0, null, null);
            }
        }
    }
    
    private void replyToRequest(GTicket ticket)
    {
        // Request location permission so that we can get location updates from this device
        if ( PackageManager.PERMISSION_DENIED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) )
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_CODE_LOCATION);
        }
        else
        {
            // Send ticket back to requester.
            GlympseWrapper.instance().getGlympse().sendTicket(ticket);
        }
    }
    
    private void showRequestPrompt(GUserTicket request)
    {
        _ticket = request.getTicket();
        GInvite invite = _ticket.getInvites().at(0);
        if ( null == invite )
        {
            alert(R.string.request_without_invite);
            return;
        }
        
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Glympse request to share location with " + invite.getAddress() + " was received. Do you want to reply?");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
        {
            public void onClick(DialogInterface dialog, int id) 
            {
                replyToRequest(_ticket);
            }
        });
        alert.setNegativeButton("No", new DialogInterface.OnClickListener() 
        {
            public void onClick(DialogInterface dialog, int id) 
            {
                // if this button is clicked, just close
                // the dialog box and do nothing
                dialog.cancel();
            }
        });
        alert.show();
    }
    
    @SuppressWarnings("unchecked")
    public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_PLATFORM == listener )
        {
            if ( 0 != ( GE.PLATFORM_INVITE_TICKET & events ) )
            {
                alert(R.string.ticket_invite_encountered);
            }
            else if ( 0 != ( GE.PLATFORM_INVITE_REQUEST & events ) )
            {
                showRequestPrompt((GUserTicket)obj);
            }            
        }
        if ( GE.LISTENER_GROUPS == listener )
        {
            if ( 0 != ( GE.GROUPS_INVITE & events ) )
            {
                alert(R.string.group_invite_encountered);
            }
        }
        if ( GE.LISTENER_INVITE == listener )
        {
            if ( 0 != ( GE.INVITE_INVALID_CODE & events ) )
            {
                alert(R.string.invalid_invite_code);
            }
        }
        if ( GEP.LISTENER_PLATFORM == listener )
        {
            if ( 0 != ( GEP.PLATFORM_URL_INVITES & events ) )
            {
                // Some invites were found in the specified URI. 
                // Start listening to invite events to handle invalid code errors.   
                for ( GEventSink sink : (GArray<GEventSink>)obj )
                {
                    sink.addListener(this);
                }
            }
            else if ( 0 != ( GEP.PLATFORM_WRONG_SERVER & events ) )
            {
                alert(R.string.wrong_server);
            }
        }
    }
    
    @Override public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);       
        
        processIntent(intent);    
    }

    @Override protected void onResume()
    {
        super.onResume();
        
        GlympseWrapper.instance().getGlympse().setActive(true);
        GlympseWrapper.instance().getGlympse().addListener(this);
        GlympseWrapper.instance().getGlympse().getGroupManager().addListener(this);
        
        Intent intent = getIntent();
        if (null != intent)
        {
            setIntent(null);            
            processIntent(intent);            
        }        
    }
    
    @Override protected void onPause()
    {
        super.onPause();    
        
        GlympseWrapper.instance().getGlympse().getGroupManager().removeListener(this);
        GlympseWrapper.instance().getGlympse().removeListener(this);
        GlympseWrapper.instance().getGlympse().setActive(false);
    }
    
    private void processIntent(Intent intent)
    {
        if ( Intent.ACTION_VIEW.equalsIgnoreCase(intent.getAction()) )
        {     
            _uriEdit.setText(intent.getDataString());
            analyze(null);
        }        
    }
    
    private void alert(int id)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(id);
        alert.show();
    }    
}
