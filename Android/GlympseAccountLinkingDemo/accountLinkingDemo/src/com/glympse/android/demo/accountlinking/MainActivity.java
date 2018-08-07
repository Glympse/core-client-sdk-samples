//------------------------------------------------------------------------------
//
// Copyright (c) 2013 Glympse Inc.  All rights reserved. 
//
//------------------------------------------------------------------------------

package com.glympse.android.demo.accountlinking;

import junit.framework.Assert;

import twitter4j.auth.AccessToken;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.glympse.android.api.GE;
import com.glympse.android.api.GEventListener;
import com.glympse.android.api.GGlympse;
import com.glympse.android.api.GlympseFactory;
import com.glympse.android.core.CC;
import com.glympse.android.core.GPrimitive;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.plus.Plus;

public class MainActivity extends Activity implements GEventListener, ConnectionCallbacks, OnConnectionFailedListener
{   
    private GoogleApiClient _googleApi;

    private TextView _titleLabel        = null;
    private ProgressBar _activity       = null;
    private Button _facebookLoginButton = null;
    private Button _twitterLoginButton  = null;
    private Button _googleLoginButton   = null;
    private Button _skipLoginButton     = null;

    private static final int REQUEST_CODE_GET_ACCOUNTS = 1;
    private static final int REQUEST_CODE_READ_CONTACTS = 2;

    private CallbackManager _callbackManager;

    @Override 
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        _titleLabel          = (TextView)findViewById(R.id.title);
        _activity            = (ProgressBar)findViewById(R.id.activity);
        _facebookLoginButton = (Button)findViewById(R.id.facebook_login);
        _twitterLoginButton  = (Button)findViewById(R.id.twitter_login);
        _googleLoginButton   = (Button)findViewById(R.id.google_login);
        _skipLoginButton     = (Button)findViewById(R.id.skip_login);

        GlympseWrapper.instance().createGlympse(this);

        FacebookSdk.sdkInitialize(getApplicationContext());
        _callbackManager = CallbackManager.Factory.create();

        // Configure the Google+ API.
        _googleApi = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(Plus.API)
            .addScope(Plus.SCOPE_PLUS_LOGIN)
            .build();

    }
    
    @Override 
    public void onResume()
    {
        super.onResume();
        
        GlympseWrapper.instance().setActive(true);

        // Listen for events from the platform, such as PLATFORM_ACCOUNT_CREATE_FAILED and PLATFORM_SYNCED_WITH_SERVER.
        GlympseWrapper.instance().getGlympse().addListener(this);

        // Log extra details for demo purposes.
        GlympseWrapper.instance().getGlympse().overrideLoggingLevels(CC.CRITICAL, CC.DUMP);
        
        if ( GlympseWrapper.instance().getGlympse().hasUserAccount() )
        {
            // If the user already has a Glympse account assigned, then just start the platform normally.
            GlympseWrapper.instance().getGlympse().start();
        }
        else
        {
            // Otherwise, allow the user to choose how they want to create their Glympse account.
            setTitle("Account Linking", false);
        }
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        
        // When the view disappears, deregister for platform events.
        GlympseWrapper.instance().getGlympse().removeListener(this);

        GlympseWrapper.instance().setActive(false); 
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
            else if ( REQUEST_CODE_READ_CONTACTS == requestCode )
            {
                // Skipping login creates a new anonymous Glympse user account with no linked accounts.
                GlympseWrapper.instance().getGlympse().start();

                setTitle("Skipping...", true);
            }
        }
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
                // Login using the provided access tokens before starting the platform to perform
                // a federated login using the Google+ identity of the authenticated user.
                GPrimitive profile = GlympseFactory.createGoogleAccountProfile(token);
                GlympseWrapper.instance().getGlympse().logout();
                GlympseWrapper.instance().getGlympse().login(profile);
                GlympseWrapper.instance().getGlympse().start();

                setTitle("Logging in...", true);
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) 
    {
        _googleApi.connect();
    }

    /**
     * Action handlers
     */

    public void doFacebookLogin(View v)
    {
        SocialHelper.loginToFacebook(this, _callbackManager, new SocialHelper.FBCompletionCallback()
        {
            @Override
            public void onComplete(String token)
            {
                if ( null == token )
                {
                    return;
                }
                // Login using the provided access token before starting the platform to perform
                // a federated login using the Facebook identity of the authenticated user.
                GPrimitive profile = GlympseFactory.createFacebookAccountProfile(token);
                GlympseWrapper.instance().getGlympse().logout();
                GlympseWrapper.instance().getGlympse().login(profile);
                GlympseWrapper.instance().getGlympse().start();

                setTitle("Logging in...", true);
            }
        });
    }
    
    public void doTwitterLogin(View v)
    {
        SocialHelper.loginToTwitter(this, new SocialHelper.TWCompletionCallback()
        {
            @Override
            public void onComplete(AccessToken token)
            {
                // Login using the provided access token before starting the platform to perform
                // a federated login using the Facebook identity of the authenticated user.
                GPrimitive profile = GlympseFactory.createTwitterAccountProfile(
                    getResources().getString(R.string.twitter_api_key),
                    getResources().getString(R.string.twitter_api_secret),
                    token.getToken(),
                    token.getTokenSecret());
                GlympseWrapper.instance().getGlympse().logout();
                GlympseWrapper.instance().getGlympse().login(profile);
                GlympseWrapper.instance().getGlympse().start();

                setTitle("Logging in...", true);
            }
        });
    }
    
    public void doGoogleLogin(View v)
    {
        if ( PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS ) )
        {
            connectGoogle();
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_CODE_GET_ACCOUNTS);
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
    
    public void skipLogin(View v)
    {
        if ( PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS ) )
        {
            // Skipping login creates a new anonymous Glympse user account with no linked accounts.
            GlympseWrapper.instance().getGlympse().start();

            setTitle("Skipping...", true);
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_CODE_READ_CONTACTS);
        }
    }
    
    /**
     * Private Methods
     */

    private void setTitle(String title, boolean loading)
    {
        _titleLabel.setText(title);
        _activity.setVisibility(loading ? View.VISIBLE : View.GONE);
        _facebookLoginButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        _twitterLoginButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        _googleLoginButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        _skipLoginButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }
    
    /**
     * GEventListener
     */

    @Override public void eventsOccurred(GGlympse glympse, int listener, int events, Object obj)
    {
        if ( GE.LISTENER_PLATFORM == listener )
        {
            if ( 0 != ( events & GE.PLATFORM_ACCOUNT_CREATE_FAILED ))
            {
                // Check for a failure to create a Glympse account. This can happen for several reasons,
                // but in this case, it's most likely invalid or expired tokens from a third-party service.
                Toast.makeText(this, "Error: Federated login failed.", Toast.LENGTH_LONG).show();

                setTitle("Account Linking", false);
            }
            if ( 0 != ( events & GE.PLATFORM_SYNCED_WITH_SERVER ))
            {
                // If we successfully sync with the server, then we have a valid Glympse user account and
                // up-to-date access tokens, so show the "account linking" view from which the user can
                // choose to link new accounts, unlink existing accounts or logout.
                Intent intent = new Intent(this, LinkedAccountsActivity.class);
                startActivity(intent);
                
                setTitle("Account Linking", false);
            }
        }
    }
}
