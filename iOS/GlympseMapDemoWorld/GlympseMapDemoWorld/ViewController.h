//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>
#import <GLYMapKit/GLYMapKit.h>

@interface ViewController : UIViewController <UITextFieldDelegate, GLYMapLayerWorldListener, GLYLockableLayerListener>
{
    BOOL _isFollowingLocked;
    
    Glympse::GMapLayerWorld _worldLayer;
}

@property (weak, nonatomic) IBOutlet GLYMapView *map;
@property (weak, nonatomic) IBOutlet UITextField *textFieldInviteCode;
@property (weak, nonatomic) IBOutlet UIButton *buttonViewTicket;
@property (weak, nonatomic) IBOutlet UIButton *buttonFollowUser;
@property (weak, nonatomic) IBOutlet UIButton *buttonFollowUserAndDestination;
@property (weak, nonatomic) IBOutlet UIButton *buttonFollowAll;
@property (weak, nonatomic) IBOutlet UIButton *buttonLock;

- (IBAction)buttonViewTicketPressed:(id)sender;
- (IBAction)buttonFollowUserPressed:(id)sender;
- (IBAction)buttonFollowUserAndDestinationPressed:(id)sender;
- (IBAction)buttonFollowAllPressed:(id)sender;
- (IBAction)buttonLockPressed:(id)sender;


@end
