package com.glympse.android.demo.accountlinking;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.ProfileTracker;
import com.glympse.android.api.GC;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GLinkedAccount;
import com.glympse.android.api.GLinkedAccountsManager;
import com.glympse.android.api.GUser;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.core.GPrimitive;
import com.glympse.android.ui.GLYAvatarView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

import junit.framework.Assert;

import twitter4j.auth.AccessToken;

public class LinkedAccountsActivity extends Activity implements GEventListener, ConnectionCallbacks, OnConnectionFailedListener
{
    private GoogleApiClient _googleApi;

    private GLYAvatarView _avatarImage      = null;
    private TextView _usernameLabel         = null;
    private TextView _nicknameLabel         = null;
    private TextView _statusLabel           = null;
    private TextView _facebookUsernameLabel = null;
    private TextView _twitterUsernameLabel  = null;
    private TextView _googleUsernameLabel   = null;
    private Button _facebookLinkButton      = null;
    private Button _twitterLinkButton       = null;
    private Button _googleLinkButton        = null;

    private static final int REQUEST_CODE_GET_ACCOUNTS = 1;


    // Facebook objects
    private CallbackManager _callbackManager;

    @Override 
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.linked_accounts);
        
        _avatarImage           = (GLYAvatarView)findViewById(R.id.avatar);
        _usernameLabel         = (TextView)findViewById(R.id.uid);
        _nicknameLabel         = (TextView)findViewById(R.id.nickname);
        _statusLabel           = (TextView)findViewById(R.id.status);
        _facebookUsernameLabel = (TextView)findViewById(R.id.facebook_username);
        _twitterUsernameLabel  = (TextView)findViewById(R.id.twitter_username);
        _googleUsernameLabel   = (TextView)findViewById(R.id.google_username);
        _facebookLinkButton    = (Button)findViewById(R.id.facebook_link);
        _twitterLinkButton     = (Button)findViewById(R.id.twitter_link);
        _googleLinkButton      = (Button)findViewById(R.id.google_link);
        
        // Configure the Google+ API.
        _googleApi = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(Plus.API)
            .addScope(Plus.SCOPE_PLUS_LOGIN)
            .build();

        FacebookSdk.sdkInitialize(getApplicationContext());
        _callbackManager = CallbackManager.Factory.create();
    }
    
    @Override 
    protected void onResume()
    {
        super.onResume();
        
        // Listen for events from the the self user, such as USER_NICKNAME_CHANGED.
        GlympseWrapper.instance().getGlympse().getUserManager().getSelf().addListener(this);
        
        // Listen for events from the the linked accounts manager, such as ACCOUNT_LIST_REFRESH_SUCCEEDED.
        GlympseWrapper.instance().getGlympse().getLinkedAccountsManager().addListener(this);
        
        GlympseWrapper.instance().setActive(true);

        refresh();
    }
    
    @Override 
    protected void onPause()
    {
        super.onPause();
        
        GlympseWrapper.instance().setActive(false);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        
        // If Google+ connection resolution has completed, try connecting if a connection is
        // not already in progress.
        if ( requestCode == SocialHelper.GOOGLE_SIGN_IN ) 
        {
            if ( resultCode == RESULT_OK )
            {
                if ( !_googleApi.isConnecting() )
                {
                    _googleApi.connect();
                }
            }
            else if ( resultCode == RESULT_CANCELED )
            {
                // NOTE: If you hit this assert, then it means that you have not properly
                // created and configured a client identity in the Google developer console
                // (http://console.developers.google.com). Android applications are identified
                // to Google+ sign-in by a combination of package identifier and the signing 
                // certificate finger print and is therefore unique to every developer.
                // 
                // To configure your Google client identity, follow the directions here
                // (https://developers.google.com/+/mobile/android/getting-started) and ensure
                // that you specify com.glympse.android.demo.accountlinking as the package
                // name and the SHA1 finger print of your Android debug keystore (instructions
                // are provided in the link above) as the signing certificate finger print.
                //
                // Once you have a properly configured your client identity in the Google 
                // developer console, remember to comment out the assert below, as you will 
                // also hit it when choosing to cancel an attempt to grant permissions in the 
                // Google+ authorization dialogs.
                Assert.fail("Google+ configuration missing");
            }
            
            return;
        }
    }
    
    /**
     * Google+ handlers
     */
    
    @Override
    public void onConnectionFailed(ConnectionResult result) 
    {
        // If connecting to Google+ failed, but a resolution is available, then proceed with 
        // the resolution. This may be because the user needs to select a Google account
        // and/or accept the requested permissions.
        if ( result.hasResolution() ) 
        {
            try 
            {
                result.startResolutionForResult(this, SocialHelper.GOOGLE_SIGN_IN);
            } 
            catch ( SendIntentException e ) 
            {
                _googleApi.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint)
    {
        SocialHelper.requestGoogleToken(this, _googleApi, new SocialHelper.GPCompletionCallback()
        {
            @Override
            public void onComplete(String token)
            {
                // Link to Google+ using the provided access token.
                GPrimitive profile = GlympseFactory.createGoogleAccountProfile(token);
                GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
                linkedAccountsManager.link(GC.LINKED_ACCOUNT_TYPE_GOOGLE(), profile);

                _statusLabel.setText("Linking...");
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) 
    {
        _googleApi.connect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if ( grantResults[0] == PackageManager.PERMISSION_GRANTED )
        {
            if ( REQUEST_CODE_GET_ACCOUNTS == requestCode )
            {
                connectGoogle();
            }
        }
    }

    /**
     * Action handlers
     */

    public void doLogOut(View v)
    {
        // Before stopping the platform, deregister for self user and linked accounts manager events.
        GlympseWrapper.instance().getGlympse().getUserManager().getSelf().removeListener(this);
        GlympseWrapper.instance().getGlympse().getLinkedAccountsManager().removeListener(this);

        // Stop the platform. Once stopped, the platform cannot be restarted, so after stopping and 
        // disposing the current one, we need to create a new one.
        GlympseWrapper.instance().stop();
        GlympseWrapper.instance().createGlympse(this);
        
        // Erase the stored Glympse user account credentials.
        GlympseWrapper.instance().getGlympse().logout();

        // Clear all cached Facebook token information.
        SocialHelper.logoutFacebook();

        // Clear all cached Google+ token information.
        if ( _googleApi.isConnected() )
        {
            Plus.AccountApi.clearDefaultAccount(_googleApi);
            Plus.AccountApi.revokeAccessAndDisconnect(_googleApi);

            _googleApi.disconnect();
        }
        
        finish();
    }
    
    public void toggleFacebook(View v)
    {
        if ( _facebookLinkButton.getText().equals("Unlink") )
        {
            // Clear all cached Facebook token information and unlink Facebook from the Glympse user account.
            SocialHelper.logoutFacebook();
            
            GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
            linkedAccountsManager.unlink(GC.LINKED_ACCOUNT_TYPE_FACEBOOK());

            _statusLabel.setText("Unlinking...");
        }
        else
        {
            SocialHelper.loginToFacebook(this, _callbackManager, new SocialHelper.FBCompletionCallback ()
            {
                @Override
                public void onComplete(String token)
                {
                    if ( null == token )
                    {
                        return;
                    }
                    // Link to Facebook using the provided access token.
                    GPrimitive profile = GlympseFactory.createFacebookAccountProfile(token);
                    GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
                    linkedAccountsManager.link(GC.LINKED_ACCOUNT_TYPE_FACEBOOK(), profile);

                    _statusLabel.setText("Linking...");
                }
            });
        }
    }
    
    public void toggleTwitter(View v)
    {
        if ( _twitterLinkButton.getText().equals("Unlink") )
        {
            GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
            linkedAccountsManager.unlink(GC.LINKED_ACCOUNT_TYPE_TWITTER());

            _statusLabel.setText("Unlinking...");
        }
        else
        {
            SocialHelper.loginToTwitter(this, new SocialHelper.TWCompletionCallback()
            {
                @Override
                public void onComplete(AccessToken token)
                {
                    // Link to Facebook using the provided access tokens.
                    GPrimitive profile = GlympseFactory.createTwitterAccountProfile(
                        getResources().getString(R.string.twitter_api_key),
                        getResources().getString(R.string.twitter_api_secret),
                        token.getToken(),
                        token.getTokenSecret());
                    GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
                    linkedAccountsManager.link(GC.LINKED_ACCOUNT_TYPE_TWITTER(), profile);

                    _statusLabel.setText("Linking...");
                }
            });
        }
    }

    public void toggleGoogle(View v)
    {
        if ( _googleLinkButton.getText().equals("Unlink") )
        {
            // Clear all cached Google+ token information.
            if ( _googleApi.isConnected() )
            {
                Plus.AccountApi.clearDefaultAccount(_googleApi);
                Plus.AccountApi.revokeAccessAndDisconnect(_googleApi);

                _googleApi.disconnect();
            }
            
            GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
            linkedAccountsManager.unlink(GC.LINKED_ACCOUNT_TYPE_GOOGLE());

            _statusLabel.setText("Unlinking...");
        }
        else
        {
            if ( PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS) )
            {
                connectGoogle();
            }
            else
            {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.GET_ACCOUNTS},
                    REQUEST_CODE_GET_ACCOUNTS);
            }
        }
    }

    private void connectGoogle()
    {
        if ( !_googleApi.isConnected() )
        {
            _googleApi.connect();
        }
        else
        {
            onConnected(null);
        }
    }

    public void doRefresh(View v)
    {
        GlympseWrapper.instance().getGlympse().getLinkedAccountsManager().refresh();
        
        _statusLabel.setText("Refreshing...");
    }
    
    /**
     * Private Methods
     */

    private void refresh()
    {
        GUser user = GlympseWrapper.instance().getGlympse().getUserManager().getSelf();
        
        String userId = user.getId();
        if ( userId != null )
        {
            _usernameLabel.setText(userId);
        }
        
        String nickname = user.getNickname();
        if ( nickname != null )
        {
            _nicknameLabel.setText(nickname);
        }

        _avatarImage.setDefault((BitmapDrawable)getResources().getDrawable(R.drawable.avatar));
        _avatarImage.attachImage(user.getAvatar());
        
        GLinkedAccountsManager linkedAccountsManager = GlympseWrapper.instance().getGlympse().getLinkedAccountsManager();
        
        _statusLabel.setText("Linked Accounts");
        
        GLinkedAccount facebookAccount = linkedAccountsManager.getAccount(GC.LINKED_ACCOUNT_TYPE_FACEBOOK());
        _facebookUsernameLabel.setTextColor(Color.BLACK);
        if (( facebookAccount != null ) && ( GC.LINKED_ACCOUNT_STATE_LINKED == facebookAccount.getState() ))
        {
            _facebookLinkButton.setText("Unlink");
            _facebookUsernameLabel.setText(facebookAccount.getDisplayName());
            
            if ( facebookAccount.getStatus() == GC.LINKED_ACCOUNT_STATUS_REFRESH_NEEDED )
            {
                _facebookUsernameLabel.setTextColor(Color.RED);
            }
        }
        else
        {
            _facebookLinkButton.setText("Link");
            _facebookUsernameLabel.setText(null);
        }
        
        GLinkedAccount twitterAccount = linkedAccountsManager.getAccount(GC.LINKED_ACCOUNT_TYPE_TWITTER());
        _twitterUsernameLabel.setTextColor(Color.BLACK);
        if (( twitterAccount != null ) && ( GC.LINKED_ACCOUNT_STATE_LINKED == twitterAccount.getState() ))
        {
            _twitterLinkButton.setText("Unlink");
            _twitterUsernameLabel.setText(twitterAccount.getDisplayName());
            
            if ( twitterAccount.getStatus() == GC.LINKED_ACCOUNT_STATUS_REFRESH_NEEDED )
            {
                _twitterUsernameLabel.setTextColor(Color.RED);
            }
        }
        else
        {
            _twitterLinkButton.setText("Link");
            _twitterUsernameLabel.setText(null);
        }

        GLinkedAccount googleAccount = linkedAccountsManager.getAccount(GC.LINKED_ACCOUNT_TYPE_GOOGLE());
        _googleUsernameLabel.setTextColor(Color.BLACK);
        if (( googleAccount != null ) && ( GC.LINKED_ACCOUNT_STATE_LINKED == googleAccount.getState() ))
        {
            _googleLinkButton.setText("Unlink");
            _googleUsernameLabel.setText(googleAccount.getDisplayName());
            
            if ( googleAccount.getStatus() == GC.LINKED_ACCOUNT_STATUS_REFRESH_NEEDED )
            {
                _googleUsernameLabel.setTextColor(Color.RED);
            }
        }
        else
        {
            _googleLinkButton.setText("Link");
            _googleUsernameLabel.setText(null);
        }
    }
    
    /**
     * GEventListener
     */

    @Override public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_LINKED_ACCOUNTS == listener )
        {
            if ( 0 != ( events & GE.ACCOUNT_LINK_FAILED ) )
            {
                // If linking the account fails, warn the user and refresh the view.
                Toast.makeText(this, "Error: Failed to link account.", Toast.LENGTH_LONG).show();

                refresh();
            }
            else if ( 0 != ( events & GE.ACCOUNT_LINK_SUCCEEDED ) )
            {
                // Refresh the view with the linked account details.
                refresh();
            }
            else if ( 0 != ( events & GE.ACCOUNT_UNLINK_FAILED ) )
            {
                // If unlinking the account fails, warn the user and refresh the view.
                Toast.makeText(this, "Error: Failed to unlink account.", Toast.LENGTH_LONG).show();

                refresh();
            }
            else if ( 0 != ( events & GE.ACCOUNT_UNLINK_SUCCEEDED ) )
            {
                // Refresh the view to clear the linked account details.
                refresh();
            }
            else if ( 0 != ( events & GE.ACCOUNT_LIST_REFRESH_FAILED ) )
            {
                // If refreshing the account list fails, warn the user and refresh the view.
                Toast.makeText(this, "Error: Failed to refresh linked accounts.", Toast.LENGTH_LONG).show();

                refresh();
            }
            else if ( 0 != ( events & GE.ACCOUNT_LIST_REFRESH_SUCCEEDED ) )
            {
                // Refresh the view with all linked account details.
                refresh();
            }
        }
        else if ( GE.LISTENER_USER == listener )
        {
            if ( 0 != ( events & GE.USER_NICKNAME_CHANGED ) )
            {
                // If the user nickname changes, refresh the view.
                refresh();
            }
        }
    }
}
