//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>
#import <GLYMapKit/GLYMapKit.h>

@interface ViewController : UIViewController <UITextFieldDelegate, GLYMapLayerGroupListener>
{
    Glympse::GMapManager _manager;
    Glympse::GMapLayerGroup _groupLayer;
    
    Glympse::GUser _activeUser;
    Glympse::GPrimitive _baseStyle;
    Glympse::GPrimitive _accentStyle;
}


@property (weak, nonatomic) IBOutlet GLYMapView *map;
@property (weak, nonatomic) IBOutlet UITextField *textFieldGroupName;
@property (weak, nonatomic) IBOutlet UIButton *buttonViewGroup;

- (IBAction)buttonViewGroupPressed:(id)sender;

@end
