//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

@interface MainViewController : UIViewController
{

}

@property (weak, nonatomic) IBOutlet UILabel * titleLabel;
@property (weak, nonatomic) IBOutlet UIActivityIndicatorView * loadingActivity;
@property (weak, nonatomic) IBOutlet UIButton * facebookLoginButton;
@property (weak, nonatomic) IBOutlet UIButton * twitterLoginButton;
@property (weak, nonatomic) IBOutlet UIButton * googleLoginButton;
@property (weak, nonatomic) IBOutlet UIButton * skipLoginButton;

- (IBAction)facebookLoginButtonPressed:(id)sender;
- (IBAction)twitterLoginButtonPressed:(id)sender;
- (IBAction)googleLoginButtonPressed:(id)sender;
- (IBAction)skipLoginButtonPressed:(id)sender;

@end
