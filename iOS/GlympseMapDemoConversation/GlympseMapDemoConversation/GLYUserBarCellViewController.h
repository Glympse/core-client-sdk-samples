//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

@protocol GLYUserBarCellListener

-(void)userCellWasSelected:(Glympse::GUser)user;

@end

@interface GLYUserBarCellViewController : UIViewController
{
    id<GLYUserBarCellListener> _delegate;
    Glympse::GUser _user;
    BOOL _selected;
}

@property (weak, nonatomic) IBOutlet UIView *viewContent;
@property (weak, nonatomic) IBOutlet UILabel *labelNickname;
@property (weak, nonatomic) IBOutlet GLYAvatarView *avatarView;

- (void)setSelectionListener:(id<GLYUserBarCellListener>)delegate;
- (void)setUser:(Glympse::GUser)user;
- (void)setSelected:(BOOL)selected;

@end
