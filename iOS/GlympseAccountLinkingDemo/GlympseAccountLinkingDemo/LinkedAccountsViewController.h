//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

@interface LinkedAccountsViewController : UIViewController
{
    
}

@property (weak, nonatomic) IBOutlet GLYAvatarView * avatarImage;
@property (weak, nonatomic) IBOutlet UILabel * usernameLabel;
@property (weak, nonatomic) IBOutlet UILabel * nicknameLabel;
@property (weak, nonatomic) IBOutlet UILabel * statusLabel;
@property (weak, nonatomic) IBOutlet UILabel * facebookUsernameLabel;
@property (weak, nonatomic) IBOutlet UILabel * twitterUsernameLabel;
@property (weak, nonatomic) IBOutlet UILabel * googleUsernameLabel;
@property (weak, nonatomic) IBOutlet UILabel * evernoteUsernameLabel;
@property (weak, nonatomic) IBOutlet UIButton * facebookLinkButton;
@property (weak, nonatomic) IBOutlet UIButton * twitterLinkButton;
@property (weak, nonatomic) IBOutlet UIButton * googleLinkButton;
@property (weak, nonatomic) IBOutlet UIButton * evernoteLinkButton;

- (IBAction)logoutButtonPressed:(id)sender;
- (IBAction)facebookButtonPressed:(id)sender;
- (IBAction)twitterButtonPressed:(id)sender;
- (IBAction)googleButtonPressed:(id)sender;
- (IBAction)evernoteButtonPressed:(id)sender;
- (IBAction)refreshButtonPressed:(id)sender;

@end
