//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "MainViewController.h"

#import "SocialHelper.h"
#import "NSString+Glympse.h"
#import "GlympseWrapperARC.h"
#import "GTMOAuthViewControllerTouch.h"

#import <FacebookSDK/FacebookSDK.h>
#import <GoogleOpenSource/GoogleOpenSource.h>
#import <GooglePlus/GooglePlus.h>

@interface MainViewController () < GLYEventListener, GPPSignInDelegate >

@end

@implementation MainViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if ( self )
    {

    }

    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    // Configure the Google+ singleton.
    GPPSignIn * google = [GPPSignIn sharedInstance];
    google.clientID = GOOGLE_PLUS_CLIENT_ID;
    google.scopes = @[kGTLAuthScopePlusLogin];
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    
    // Takeover as the Google+ delegate.
    [GPPSignIn sharedInstance].delegate = self;

    // Listen for events from the platform, such as PLATFORM_ACCOUNT_CREATE_FAILED and PLATFORM_SYNCED_WITH_SERVER.
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];

    // Log extra details for demo purposes.
    [GlympseWrapper instance].glympse->overrideLoggingLevels(Glympse::CC::CRITICAL, Glympse::CC::DUMP);
    
    if ( [GlympseWrapper instance].glympse->hasUserAccount() )
    {
        // If the user already has a Glympse account assigned, then just start the platform normally.
        [[GlympseWrapper instance] startWithHistory:NO];
    }
    else
    {
        // Otherwise, allow the user to choose how they want to create their Glympse account.
        [self setTitle:@"Account Linking" isLoading:NO];
    }
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    
    // When the view disappears, deregister for platform events.
    [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

#pragma mark IBActions

- (IBAction)facebookLoginButtonPressed:(id)sender
{
    // Initiate Facebook login.
    [SocialHelper loginToFacebook:^(FBSession *session, FBSessionState state) {
         
         if (( state == FBSessionStateOpen ) || ( state == FBSessionStateOpenTokenExtended ))
         {
             // Login using the provided access token before starting the platform to perform
             // a federated login using the Facebook identity of the authenticated user.
             Glympse::GPrimitive profile = Glympse::GlympseFactory::createFacebookAccountProfile(
                    Glympse::CoreFactory::createString([[[session accessTokenData] accessToken] UTF8String]));
             [GlympseWrapper instance].glympse->logout();
             [GlympseWrapper instance].glympse->login(profile);
             [[GlympseWrapper instance] startWithHistory:NO];

             [self setTitle:@"Logging in..." isLoading:YES];
         }
     }];
}

- (IBAction)twitterLoginButtonPressed:(id)sender
{
    // Initiate Twitter login.
    [SocialHelper loginToTwitter:^(GTMOAuthViewControllerTouch * viewController, GTMOAuthAuthentication * auth) {
         
         // Login using the provided access tokens before starting the platform to perform
         // a federated login using the Twitter identity of the authenticated user.
         Glympse::GPrimitive profile = Glympse::GlympseFactory::createTwitterAccountProfile(
             Glympse::CoreFactory::createString([TWITTER_API_KEY UTF8String]),
             Glympse::CoreFactory::createString([TWITTER_API_SECRET UTF8String]),
             Glympse::CoreFactory::createString([[auth token] UTF8String]),
             Glympse::CoreFactory::createString([[auth tokenSecret] UTF8String]));
         [GlympseWrapper instance].glympse->logout();
         [GlympseWrapper instance].glympse->login(profile);
         [[GlympseWrapper instance] startWithHistory:NO];
         
         [self setTitle:@"Logging in..." isLoading:YES];
     } viewController:self];
}

- (IBAction)googleLoginButtonPressed:(id)sender
{
    // Initiate Google+ login.
    [[GPPSignIn sharedInstance] authenticate];
}

- (IBAction)skipLoginButtonPressed:(id)sender
{
    // Skipping login creates a new anonymous Glympse user account with no linked accounts.
    [[GlympseWrapper instance] startWithHistory:NO];
    
    [self setTitle:@"Skipping..." isLoading:YES];
}

#pragma mark GPPSignInDelegate

- (void)finishedWithAuth:(GTMOAuth2Authentication *)auth error:(NSError *) error
{
    if ( error != nil )
    {
        UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                         message:@"Google+ login failed."
                                                        delegate:nil
                                               cancelButtonTitle:nil
                                               otherButtonTitles:@"OK", nil];
        [alert show];
    }
    else
    {
        // Login using the provided access tokens before starting the platform to perform
        // a federated login using the Google+ identity of the authenticated user.
        Glympse::GPrimitive profile = Glympse::GlympseFactory::createGoogleAccountProfile(
            Glympse::CoreFactory::createString([[auth accessToken] UTF8String]));
        [GlympseWrapper instance].glympse->logout();
        [GlympseWrapper instance].glympse->login(profile);
        [[GlympseWrapper instance] startWithHistory:NO];
        
        [self setTitle:@"Logging in..." isLoading:YES];
    }
}

#pragma mark Private Methods

- (void)setTitle:(NSString *)title isLoading:(BOOL)loading
{
    [self.titleLabel setText:title];
    [self.loadingActivity setHidden:!loading];
    [self.facebookLoginButton setHidden:loading];
    [self.twitterLoginButton setHidden:loading];
    [self.googleLoginButton setHidden:loading];
    [self.skipLoginButton setHidden:loading];
}

#pragma mark GLYEventListener

- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if ( Glympse::GE::LISTENER_PLATFORM == listener )
    {
        if ( 0 != ( events & Glympse::GE::PLATFORM_ACCOUNT_CREATE_FAILED ))
        {
            // Check for a failure to create a Glympse account. This can happen for several reasons,
            // but in this case, it's most likely invalid or expired tokens from a third-party service.
            UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                             message:@"Federated login failed."
                                                            delegate:nil
                                                   cancelButtonTitle:nil
                                                   otherButtonTitles:@"OK", nil];
            [alert show];
            
            [self setTitle:@"Account Linking" isLoading:NO];
        }
        if ( 0 != ( events & Glympse::GE::PLATFORM_SYNCED_WITH_SERVER ))
        {
            // If we successfully sync with the server, then we have a valid Glympse user account and
            // up-to-date access tokens, so show the "account linking" view from which the user can
            // choose to link new accounts, unlink existing accounts or logout.
            [self performSegueWithIdentifier:@"Main->Link" sender:self];

            [self setTitle:@"Account Linking" isLoading:NO];
        }
    }
}

@end
