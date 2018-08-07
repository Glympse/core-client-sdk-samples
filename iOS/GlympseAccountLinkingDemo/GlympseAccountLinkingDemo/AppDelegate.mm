//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "AppDelegate.h"

#import "GlympseWrapperARC.h"

#import <FacebookSDK/FacebookSDK.h>
#import <GooglePlus/GooglePlus.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    return YES;
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{

}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Every time the application returns to the foreground, iOS applications should manually refresh
    // the list of linked accounts in order to discover changes that may have been made on another
    // device utilizing the same Glympse user account.
    if ( [GlympseWrapper instance].glympse->getLinkedAccountsManager() != NULL )
    {
        [GlympseWrapper instance].glympse->getLinkedAccountsManager()->refresh();
    }
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    [GlympseWrapper instance].glympse->setActive(true);
    
    // Handle the user leaving the app while the Facebook login dialog is being shown
    // For example: when the user presses the iOS "home" button while the login dialog is active
    [FBAppCall handleDidBecomeActive];
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    [GlympseWrapper instance].glympse->setActive(false);
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    [[GlympseWrapper instance] stop];
}

// During Facebook and Google+ login flow, your app passes control to the native iOS app or
// mobile browser. After authentication, your app will be called back with the session information.
- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation
{
    if ( [FBAppCall handleOpenURL:url sourceApplication:sourceApplication] )
    {
        return YES;
    }
    
    if ( [GPPURLHandler handleURL:url sourceApplication:sourceApplication annotation:annotation] )
    {
        return YES;
    }
    
    return NO;
}

@end
