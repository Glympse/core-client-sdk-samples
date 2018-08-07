//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

#import "GTMOAuthViewControllerTouch.h"

#import <FacebookSDK/FacebookSDK.h>

// Facebook
// Defined in application Info.plist

// Twitter
#define TWITTER_API_KEY         @"kifqaaGilSGATTZquwYw3Ie5a"
#define TWITTER_API_SECRET      @"BDsCP8vkkY2jzoxSEtb080EV3gCrjf4U8XSDkTshAQL3xqw1qx"

// Google+
#define GOOGLE_PLUS_CLIENT_ID   @"45117468735-2a8h6a2i8knhp0fh3l0dt9tjt4tpofj7.apps.googleusercontent.com"

typedef void (^FBCompletionHandler)(FBSession * session, FBSessionState status);
typedef void (^TWCompletionHandler)(GTMOAuthViewControllerTouch * viewController, GTMOAuthAuthentication * auth);

@interface SocialHelper : NSObject

+ (void) loginToFacebook:(FBCompletionHandler)completion;
+ (void) loginToTwitter:(TWCompletionHandler)completion viewController:(UIViewController *)parent;

@end