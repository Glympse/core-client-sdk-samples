package com.glympse.android.demo.accountlinking;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class SocialHelper
{
    // Request code used to invoke sign in user interactions. Value is arbitrary, but
    // ensure that it does not conflict with Facebook request codes, as both requests
    // must be distinguishable in onActivityResult().
    public static final int GOOGLE_SIGN_IN = 6143972;

    public interface FBCompletionCallback 
    {
        public void onComplete(String token);
    }

    public interface TWCompletionCallback 
    {
        public void onComplete(AccessToken token);
    }

    public interface GPCompletionCallback 
    {
        public void onComplete(String token);
    }

    private static class FacebookTokenTask implements FacebookCallback<LoginResult>
    {
        final Context _context;
        final CallbackManager _callbackManager;
        final FBCompletionCallback _callback;

        private AccessTokenTracker _accessTokenTracker;
        private com.facebook.AccessToken _accessToken;

        public FacebookTokenTask(final Context context, final CallbackManager callbackManager,
                                 final FBCompletionCallback callback)
        {
            super();

            _context = context;
            _callbackManager = callbackManager;
            _callback = callback;
        }

        protected void onFinished(String token)
        {
            if ( token == null )
            {
                Toast.makeText(_context, "Error: Facebook login failed.", Toast.LENGTH_LONG).show();
            }
            else
            {
                _callback.onComplete(token);
            }
        }

        public void run()
        {
            LoginManager.getInstance().registerCallback(_callbackManager, this);

            _accessTokenTracker = new AccessTokenTracker() {
                @Override
                protected void onCurrentAccessTokenChanged(
                        com.facebook.AccessToken oldAccessToken,
                        com.facebook.AccessToken currentAccessToken)
                {
                    // Set the access token using
                    // currentAccessToken when it's loaded or set.
                    _accessToken = currentAccessToken;

                    // Link the account if we now ave an access token
                    if ( null != _accessToken )
                    {
                        onFinished(_accessToken.getToken());
                    }
                    // Otherwise we just logged out, so unlink
                    else
                    {
                        onFinished(null);
                    }
                }
            };

            _accessToken = com.facebook.AccessToken.getCurrentAccessToken();
        }

        // FacebookCallback<LoginResult> Methods
        @Override
        public void onSuccess(LoginResult loginResult)
        {
            // Login was successful. Now try and actually get the token.
            Toast.makeText(_context, "Error: Facebook login succeeded.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancel()
        {
            onFinished(null);
        }

        @Override
        public void onError(FacebookException error)
        {
            onFinished(null);
        }

    }

    public static void loginToFacebook(final Context context, final CallbackManager callbackManager,
                                       final FBCompletionCallback callback)
    {
        new FacebookTokenTask(context, callbackManager, callback).run();
    }

    public static void logoutFacebook()
    {
        com.facebook.AccessToken fbAccessToken = com.facebook.AccessToken.getCurrentAccessToken();
        if ( fbAccessToken != null )
        {
            com.facebook.AccessToken.setCurrentAccessToken(null);
        }
    }
    
    public static void loginToTwitter(final Activity activity, final TWCompletionCallback callback)
    {       
        new TwitterRequestTokenTask(activity, callback).execute();
    }
    
    public static void requestGoogleToken(final Activity activity, final GoogleApiClient apiClient, final GPCompletionCallback callback)
    {
        new GoogleTokenTask(activity, apiClient, callback).execute();
    }
    
    private static class TwitterRequestTokenTask extends AsyncTask<String, String, RequestToken>
    {
        Context _context;
        TWCompletionCallback _callback;
        
        public TwitterRequestTokenTask(Context context, TWCompletionCallback callback)
        {
            super();
            
            _context = context;
            _callback = callback;
        }
        
        @Override
        protected void onPostExecute(RequestToken requestToken)
        {
            if ( null == requestToken )
            {
                Toast.makeText(_context, "Error: Twitter login failed.", Toast.LENGTH_LONG).show();
                return;
            }

            Dialog twitterDialog = new Dialog(_context);
            twitterDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            
            LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.oauth_web_view, null);
            twitterDialog.setContentView(view);
            twitterDialog.setCancelable(true);
            
            WebView webView = (WebView)view.findViewById(R.id.webView);
            TwitterAuthorizationWebViewClient webViewClient = new TwitterAuthorizationWebViewClient(_context, _callback, twitterDialog);
            webViewClient.setRequestToken(requestToken);
            webView.setWebViewClient(webViewClient);
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(twitterDialog.getWindow().getAttributes());
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;

            twitterDialog.show();
            twitterDialog.getWindow().setAttributes(params);
            
            webView.loadUrl(requestToken.getAuthenticationURL());
        }
     
        @Override
        protected RequestToken doInBackground(String... params)
        {
            try
            {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(_context.getResources().getString(R.string.twitter_api_key));
                builder.setOAuthConsumerSecret(_context.getResources().getString(R.string.twitter_api_secret));

                Configuration configuration = builder.build(); 
                TwitterFactory factory = new TwitterFactory(configuration);
                Twitter twitter = factory.getInstance();

                return twitter.getOAuthRequestToken(_context.getResources().getString(R.string.twitter_oauth_url));
            }
            catch ( TwitterException e )
            {

            }
            
            return null;
        }
    }
    
    private static class TwitterAuthorizationWebViewClient extends WebViewClient
    {
        private Context _context;
        private TWCompletionCallback _callback;
        private Dialog _dialog;
        private RequestToken _token;
        
        public TwitterAuthorizationWebViewClient(Context context, TWCompletionCallback callback, Dialog dialog)
        {
            super();
            
            _context = context;
            _callback = callback;
            _dialog = dialog;
        }

        public void setRequestToken(RequestToken requestToken)
        {
            _token = requestToken;
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            if ( url.startsWith(_context.getResources().getString(R.string.twitter_oauth_url)) )
            {
                _dialog.dismiss();
                
                // Only try to log in if the returned URL contains a verifier string
                if ( url.contains("&oauth_verifier=") )
                {
                    new TwitterAccessTokenTask(_context, _callback).execute(_token, url);
                }
            }
        }
    }

    private static class TwitterAccessTokenTask extends AsyncTask<Object, String, AccessToken>
    {
        Context _context;
        TWCompletionCallback _callback;
        
        public TwitterAccessTokenTask(Context context, TWCompletionCallback callback)
        {
            super();
            
            _context = context;
            _callback = callback;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken)
        {
            if ( null == accessToken )
            {
                Toast.makeText(_context, "Error: Twitter login failed.", Toast.LENGTH_LONG).show();
            }

            _callback.onComplete(accessToken);
        }
     
        @Override
        protected AccessToken doInBackground(Object... params)
        {
            try
            {
                RequestToken requestToken = (RequestToken)params[0];
                String url = (String)params[1];
                String verifier = url.substring(url.indexOf("&oauth_verifier=") + 16);
                
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(_context.getResources().getString(R.string.twitter_api_key));
                builder.setOAuthConsumerSecret(_context.getResources().getString(R.string.twitter_api_secret));

                Configuration configuration = builder.build();
                TwitterFactory factory = new TwitterFactory(configuration);
                Twitter twitter = factory.getInstance();

                return twitter.getOAuthAccessToken(requestToken, verifier);
            }
            catch ( TwitterException e )
            {

            }
            
            return null;
        }
    }
    
    private static class GoogleTokenTask extends AsyncTask<String, Integer, String>
    {
        Context _context;
        GoogleApiClient _googleApi;
        GPCompletionCallback _callback;
        
        public GoogleTokenTask(Context context, GoogleApiClient googleApi, GPCompletionCallback callback)
        {
            super();
            
            _context = context;
            _googleApi = googleApi;
            _callback = callback;
        }
        
        @Override
        protected void onPostExecute(String token)
        {
            if ( token == null )
            {
                Toast.makeText(_context, "Error: Google+ login failed.", Toast.LENGTH_LONG).show();
            }
            else
            {
                _callback.onComplete(token);
            }
        }
     
        @Override
        protected String doInBackground(String... params)
        {
            String scope = "oauth2:" + Scopes.PLUS_LOGIN;
            try 
            {
                return GoogleAuthUtil.getToken(_context, Plus.AccountApi.getAccountName(_googleApi), scope);
            }
            catch ( Exception e )
            {
                
            }
            
            return null;
        }
    }
}
