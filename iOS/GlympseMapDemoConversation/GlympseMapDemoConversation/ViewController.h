//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>
#import <GLYMapKit/GLYMapKit.h>
#import "GLYUserBarCellViewController.h"

@interface ViewController : UIViewController <UITextFieldDelegate, GLYMapLayerConversationListener, GLYUserBarCellListener>
{
    Glympse::GMapManager _manager;
    Glympse::GMapLayerConversation _conversationLayer;
    
    Glympse::GUser _activeUser;
    
    NSMutableArray *_users;
    NSMutableArray *_userCells;
}

@property (weak, nonatomic) IBOutlet UITextField *textFieldInviteCode;
@property (weak, nonatomic) IBOutlet UIButton *buttonAddTicket;
@property (weak, nonatomic) IBOutlet UIButton *buttonRemoveActive;

@property (weak, nonatomic) IBOutlet UIScrollView *scrollViewUsers;
@property (weak, nonatomic) IBOutlet GLYMapView *map;

- (IBAction)buttonAddTickedPressed:(id)sender;
- (IBAction)buttonRemoveActivePressed:(id)sender;

@end
