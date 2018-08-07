//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "AppDelegate.h"
#import <UIKit/UIAlert.h>

#import "RequestMainViewController.h"
#import "GlympseWrapperARC.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    // Override point for customization after application launch.
    self.window.backgroundColor = [UIColor whiteColor];
    
    // Initialize & start the Glympse platform object without history of expired glympses
    [[GlympseWrapper instance] startWithHistory:NO];

    self.window.rootViewController = [[RequestMainViewController alloc] initWithNibName:@"RequestMainViewController"
                                                                                bundle:nil];
    
    [self.window makeKeyAndVisible];
    return YES;
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    if ( Glympse::GC::SMS_SEND_NOT_SUPPORTED == [GlympseWrapper instance].glympse->canDeviceSendSms() )
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Error" message:@"Demo needs to run on device with SMS capability" delegate:nil cancelButtonTitle:nil otherButtonTitles:nil, nil];
        [alert show];
        [alert release];
        return;
    }
    [GlympseWrapper instance].glympse->setActive(true);
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    [GlympseWrapper instance].glympse->setActive(false);
}

- (void)applicationWillTerminate:(UIApplication *)application
{  
    [[GlympseWrapper instance] stop];
}

- (void)dealloc
{
    [_window release];
    [super dealloc];
}


@end
