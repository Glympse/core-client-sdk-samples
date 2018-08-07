//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "SocialHelper.h"

#import "GTMOAuthViewControllerTouch.h"

#import <FacebookSDK/FacebookSDK.h>

@implementation SocialHelper

+ (void) loginToFacebook:(FBCompletionHandler)completion
{
    // Request permissions from Facebook for basic access.
    [FBSession openActiveSessionWithReadPermissions:@[@"public_profile"]
                                       allowLoginUI:YES
                                  completionHandler:
     ^(FBSession * session, FBSessionState state, NSError * error) {

         if ( error != nil )
         {
             UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                              message:@"Facebook login failed."
                                                             delegate:nil
                                                    cancelButtonTitle:nil
                                                    otherButtonTitles:@"OK", nil];
             [alert show];
         }
         else
         {
             completion(session, state);
         }
     }];
}

+ (void) loginToTwitter:(TWCompletionHandler)completion viewController:(UIViewController *)parent
{
    // Request permissions from Twitter for basic access.
    GTMOAuthAuthentication * auth = [[GTMOAuthAuthentication alloc] initWithSignatureMethod:kGTMOAuthSignatureMethodHMAC_SHA1
                                                                                consumerKey:TWITTER_API_KEY
                                                                                 privateKey:TWITTER_API_SECRET];
    [auth setServiceProvider:@"Twitter"];
    [auth setCallback:@"http://www.glympse.com/oauth_callback"];
    
    NSURL * requestURL = [NSURL URLWithString:@"https://api.twitter.com/oauth/request_token"];
    NSURL * accessURL = [NSURL URLWithString:@"https://api.twitter.com/oauth/access_token"];
    NSURL * authorizeURL = [NSURL URLWithString:@"https://api.twitter.com/oauth/authenticate"];
    NSString * scope = @"http://api.twitter.com/";
    
    GTMOAuthViewControllerTouch * login = [[GTMOAuthViewControllerTouch alloc] initWithScope:scope
                                                                                    language:nil
                                                                             requestTokenURL:requestURL
                                                                           authorizeTokenURL:authorizeURL
                                                                              accessTokenURL:accessURL
                                                                              authentication:auth
                                                                              appServiceName:nil
                                                                           completionHandler:
     ^(GTMOAuthViewControllerTouch * viewController, GTMOAuthAuthentication * auth, NSError * error) {
         
         [parent dismissViewControllerAnimated:NO completion:nil];
         
         if ( error != nil )
         {
             UIAlertView * alert = [[UIAlertView alloc] initWithTitle:@"Error"
                                                              message:@"Twitter login failed."
                                                             delegate:nil
                                                    cancelButtonTitle:nil
                                                    otherButtonTitles:@"OK", nil];
             [alert show];
         }
         else
         {
             completion(viewController, auth);
         }
     }];
    
    // This will associate any cookies created with a different Twitter URL so that the next time
    // the user goes to www.twitter.com, it doesn't use these credentials.
    [login setBrowserCookiesURL:[NSURL URLWithString:@"http://glympse.twitter.com/"]];
    [login setModalTransitionStyle:UIModalTransitionStyleCrossDissolve];

    [parent presentViewController:login animated:NO completion:nil];
}

@end