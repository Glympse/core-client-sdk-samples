//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "LinkedAccountsViewController.h"

#import "SocialHelper.h"
#import "NSString+Glympse.h"
#import "GlympseWrapperARC.h"
#import "GTMOAuthViewControllerTouch.h"

#import <FacebookSDK/FacebookSDK.h>
#import <GoogleOpenSource/GoogleOpenSource.h>
#import <GooglePlus/GooglePlus.h>

@interface LinkedAccountsViewController () < GLYEventListener, GPPSignInDelegate >
{

}

@end

@implementation LinkedAccountsViewController

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
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];

    // Takeover as the Google+ delegate.
    [GPPSignIn sharedInstance].delegate = self;

    // Listen for events from the the self user, such as USER_NICKNAME_CHANGED.
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse->getUserManager()->getSelf()];
    
    // Listen for events from the the linked accounts manager, such as ACCOUNT_LIST_REFRESH_SUCCEEDED.
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse->getLinkedAccountsManager()];
    
    // Size the scrollview.
    [(UIScrollView *)self.view setContentSize:self.view.frame.size];
    
    [self refresh];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
}

#pragma mark - IBActions

- (IBAction)logoutButtonPressed:(id)sender
{
    // Before stopping the platform, deregister for self user and linked accounts manager events.
    [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse->getUserManager()->getSelf()];
    [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse->getLinkedAccountsManager()];

    // Stop the platform. Once stopped, the platform cannot be restarted, so this helper creates a new
    // instance after stopping and disposing the current one.
    [[GlympseWrapper instance] stop];
    
    // Erase the stored Glympse user account credentials.
    [GlympseWrapper instance].glympse->logout();

    // Clear all cached Facebook token information.
    [FBSession.activeSession closeAndClearTokenInformation];
    [FBSession setActiveSession:nil];

    // Clear all cached Google+ token information.
    [[GPPSignIn sharedInstance] disconnect];

    [self performSegueWithIdentifier:@"Link->Main" sender:self];
}

- (IBAction)facebookButtonPressed:(id)sender
{
    if ( [self.facebookLinkButton.titleLabel.text isEqualToString:@"Unlink"] )
    {
        // Clear all cached Facebook token information and unlink Facebook from the Glympse user account.
        [FBSession.activeSession closeAndClearTokenInformation];
        [FBSession setActiveSession:nil];
        
        Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
        linkedAccountsManager->unlink(Glympse::GC::LINKED_ACCOUNT_TYPE_FACEBOOK());

        [self.statusLabel setText:@"Unlinking..."];
    }
    else
    {
        [SocialHelper loginToFacebook:^(FBSession *session, FBSessionState state) {
             
             if (( state == FBSessionStateOpen ) || ( state == FBSessionStateOpenTokenExtended ))
             {
                 // Link to Facebook using the provided access token.
                 Glympse::GPrimitive profile = Glympse::GlympseFactory::createFacebookAccountProfile(
                     Glympse::CoreFactory::createString([[[session accessTokenData] accessToken] UTF8String]));
                 Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
                 linkedAccountsManager->link(Glympse::GC::LINKED_ACCOUNT_TYPE_FACEBOOK(), profile);
                 
                 [self.statusLabel setText:@"Linking..."];
             }
         }];
    }
}

- (IBAction)twitterButtonPressed:(id)sender
{
    if ( [self.twitterLinkButton.titleLabel.text isEqualToString:@"Unlink"] )
    {
        // Unlink Twitter from the Glympse user account.
        Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
        linkedAccountsManager->unlink(Glympse::GC::LINKED_ACCOUNT_TYPE_TWITTER());

        [self.statusLabel setText:@"Unlinking..."];
    }
    else
    {
        // Initiate Twitter login.
        [SocialHelper loginToTwitter:^(GTMOAuthViewControllerTouch * viewController, GTMOAuthAuthentication * auth) {
         
             // Link to Twitter using the provided access tokens.
             Glympse::GPrimitive profile = Glympse::GlympseFactory::createTwitterAccountProfile(
                 Glympse::CoreFactory::createString([TWITTER_API_KEY UTF8String]),
                 Glympse::CoreFactory::createString([TWITTER_API_SECRET UTF8String]),
                 Glympse::CoreFactory::createString([[auth token] UTF8String]),
                 Glympse::CoreFactory::createString([[auth tokenSecret] UTF8String]));
             Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
             linkedAccountsManager->link(Glympse::GC::LINKED_ACCOUNT_TYPE_TWITTER(), profile);
             
             [self.statusLabel setText:@"Linking..."];
         } viewController:self];
    }
}

- (IBAction)googleButtonPressed:(id)sender
{
    if ( [self.googleLinkButton.titleLabel.text isEqualToString:@"Unlink"] )
    {
        // Clear all cached Google+ token information and unlink Google+ from the Glympse user account.
        [[GPPSignIn sharedInstance] disconnect];
        
        Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
        linkedAccountsManager->unlink(Glympse::GC::LINKED_ACCOUNT_TYPE_GOOGLE());
        
        [self.statusLabel setText:@"Unlinking..."];
    }
    else
    {
        // Request permissions from Google for basic acess.
        [[GPPSignIn sharedInstance] authenticate];
    }
}

- (IBAction)evernoteButtonPressed:(id)sender
{
    // TODO: Include Evernote account linking.
}

- (IBAction)refreshButtonPressed:(id)sender
{
    // Refresh the list of linked accounts. When this completes, the linked accounts manager will
    // fire the ACCOUNT_LIST_REFRESH_SUCCEEDED event.
    Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
    linkedAccountsManager->refresh();
    
    [self.statusLabel setText:@"Refreshing..."];
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
        // Link to Google+ using the provided access token.
        Glympse::GPrimitive profile = Glympse::GlympseFactory::createGoogleAccountProfile(
            Glympse::CoreFactory::createString([[auth accessToken] UTF8String]));
        Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
        linkedAccountsManager->link(Glympse::GC::LINKED_ACCOUNT_TYPE_GOOGLE(), profile);
        
        [self.statusLabel setText:@"Linking..."];
    }
}

#pragma mark Private Methods

- (void)refresh
{
    Glympse::GUser user = [GlympseWrapper instance].glympse->getUserManager()->getSelf();
    
    Glympse::GString userId = user->getId();
    if ( userId != NULL )
    {
        [self.usernameLabel setText:[NSString stringWithUTF8String:userId->getBytes()]];
    }
    
    Glympse::GString nickname = user->getNickname();
    if ( nickname != NULL )
    {
        [self.nicknameLabel setText:[NSString stringWithUTF8String:nickname->getBytes()]];
    }
    
    [self.avatarImage setDefault:[UIImage imageNamed:@"avatar"]];
    [self.avatarImage attachGImage:user->getAvatar()];
    
    Glympse::GLinkedAccountsManager linkedAccountsManager = [GlympseWrapper instance].glympse->getLinkedAccountsManager();
    
    [self.statusLabel setText:@"Linked Accounts"];
    
    Glympse::GLinkedAccount facebookAccount = linkedAccountsManager->getAccount(Glympse::GC::LINKED_ACCOUNT_TYPE_FACEBOOK());
    [self.facebookUsernameLabel setTextColor:[UIColor blackColor]];
    if (( facebookAccount != NULL ) && ( Glympse::GC::LINKED_ACCOUNT_STATE_LINKED == facebookAccount->getState() ))
    {
        [self.facebookLinkButton setTitle:@"Unlink" forState:UIControlStateNormal];
        [self.facebookUsernameLabel setText:[NSString stringWithGString:facebookAccount->getDisplayName()]];
        
        if ( facebookAccount->getStatus() == Glympse::GC::LINKED_ACCOUNT_STATUS_REFRESH_NEEDED )
        {
            [self.facebookUsernameLabel setTextColor:[UIColor redColor]];
        }
    }
    else
    {
        [self.facebookLinkButton setTitle:@"Link" forState:UIControlStateNormal];
        [self.facebookUsernameLabel setText:nil];
    }
    
    Glympse::GLinkedAccount twitterAccount = linkedAccountsManager->getAccount(Glympse::GC::LINKED_ACCOUNT_TYPE_TWITTER());
    [self.twitterUsernameLabel setTextColor:[UIColor blackColor]];
    if (( twitterAccount != NULL ) && ( Glympse::GC::LINKED_ACCOUNT_STATE_LINKED == twitterAccount->getState() ))
    {
        [self.twitterLinkButton setTitle:@"Unlink" forState:UIControlStateNormal];
        [self.twitterUsernameLabel setText:[NSString stringWithGString:twitterAccount->getDisplayName()]];

        if ( twitterAccount->getStatus() == Glympse::GC::LINKED_ACCOUNT_STATUS_REFRESH_NEEDED )
        {
            [self.twitterUsernameLabel setTextColor:[UIColor redColor]];
        }
    }
    else
    {
        [self.twitterLinkButton setTitle:@"Link" forState:UIControlStateNormal];
        [self.twitterUsernameLabel setText:nil];
    }

    Glympse::GLinkedAccount googleAccount = linkedAccountsManager->getAccount(Glympse::GC::LINKED_ACCOUNT_TYPE_GOOGLE());
    [self.googleUsernameLabel setTextColor:[UIColor blackColor]];
    if (( googleAccount != NULL ) && ( Glympse::GC::LINKED_ACCOUNT_STATE_LINKED == googleAccount->getState() ))
    {
        [self.googleLinkButton setTitle:@"Unlink" forState:UIControlStateNormal];
        [self.googleUsernameLabel setText:[NSString stringWithGString:googleAccount->getDisplayName()]];

        if ( googleAccount->getStatus() == Glympse::GC::LINKED_ACCOUNT_STATUS_REFRESH_NEEDED )
        {
            [self.googleUsernameLabel setTextColor:[UIColor redColor]];
        }
    }
    else
    {
        [self.googleLinkButton setTitle:@"Link" forState:UIControlStateNormal];
        [self.googleUsernameLabel setText:nil];
    }
}

#pragma mark GLYEventListener

- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if ( Glympse::GE::LISTENER_LINKED_ACCOUNTS == listener )
    {
        if ( 0 != ( events & Glympse::GE::ACCOUNT_LINK_FAILED ) )
        {
            // If linking the account fails, warn the user and refresh the view.
            UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                             message:@"Failed to link account."
                                                            delegate:nil
                                                   cancelButtonTitle:nil
                                                   otherButtonTitles:@"OK", nil];
            [alert show];

            [self refresh];
        }
        else if ( 0 != ( events & Glympse::GE::ACCOUNT_LINK_SUCCEEDED ) )
        {
            // Refresh the view with the linked account details.
            [self refresh];
        }
        else if ( 0 != ( events & Glympse::GE::ACCOUNT_UNLINK_FAILED ) )
        {
            // If unlinking the account fails, warn the user and refresh the view.
            UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                             message:@"Failed to unlink account."
                                                            delegate:nil
                                                   cancelButtonTitle:nil
                                                   otherButtonTitles:@"OK", nil];
            [alert show];

            [self refresh];
        }
        else if ( 0 != ( events & Glympse::GE::ACCOUNT_UNLINK_SUCCEEDED ) )
        {
            // Refresh the view to clear the linked account details.
            [self refresh];
        }
        else if ( 0 != ( events & Glympse::GE::ACCOUNT_LIST_REFRESH_FAILED ) )
        {
            // If refreshing the account list fails, warn the user and refresh the view.
            UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                             message:@"Failed to refresh linked accounts."
                                                            delegate:nil
                                                   cancelButtonTitle:nil
                                                   otherButtonTitles:@"OK", nil];
            [alert show];

            [self refresh];
        }
        else if ( 0 != ( events & Glympse::GE::ACCOUNT_LIST_REFRESH_SUCCEEDED ) )
        {
            // Refresh the view with all linked account details.
            [self refresh];
        }
    }
    else if ( Glympse::GE::LISTENER_USER == listener )
    {
        if ( 0 != ( events & Glympse::GE::USER_NICKNAME_CHANGED ) )
        {
            // If the user nickname changes, refresh the view.
            [self refresh];
        }
    }
}

@end
