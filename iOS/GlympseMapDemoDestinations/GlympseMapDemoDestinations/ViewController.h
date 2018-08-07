//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>
#import <GLYMapKit/GLYMapKit.h>

@interface ViewController : UIViewController <GLYMapLayerPlacesListener>
{
    Glympse::GMapManager _manager;
    Glympse::GMapLayerPlaces _placesLayer;
    BOOL _isUsingCustomDrawables;
    
    Glympse::GPlace _placeSpaceNeedle;
	Glympse::GPlace _placePioneerSquare;
	Glympse::GPlace _placeSeattleLibrary;
	Glympse::GPlace _placePikePlaceMarket;
	Glympse::GPlace _placeCenturyLinkField;
	Glympse::GPlace _placeSafecoField;
	Glympse::GPlace _placeSwedishMedicalCenter;
}

@property (weak, nonatomic) IBOutlet GLYMapView *map;
@property (weak, nonatomic) IBOutlet UIButton *buttonLogo;

- (IBAction)buttonLogoPressed:(id)sender;

@end
