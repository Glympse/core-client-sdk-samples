//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "AppDelegate.h"

#import "CreateMainViewController.h"
#import "GlympseWrapperARC.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    // Override point for customization after application launch.
    self.window.backgroundColor = [UIColor whiteColor];
    
    // Initialize & start the Glympse platform object without history of expired glympses
    [[GlympseWrapper instance] startWithHistory:NO withConsentExemption:NO];
    
    self.window.rootViewController = [[CreateMainViewController alloc] initWithNibName:@"CreateMainViewController"
                                                                                bundle:nil];
    
    [self.window makeKeyAndVisible];
    return YES;
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
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

@end
