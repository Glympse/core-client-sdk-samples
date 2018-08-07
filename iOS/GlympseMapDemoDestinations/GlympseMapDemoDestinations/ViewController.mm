//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "ViewController.h"
#import "GlympseWrapperARC.h"

@interface ViewController ()

@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    _isUsingCustomDrawables = NO;
    
    [self.map attachGlympse:[GlympseWrapper instance].glympse];
    
    // Create the Places layer and set desired options
    _manager = [_map mapManager];
    _placesLayer = Glympse::MapFactory::createMapLayerPlaces();
    
    // We want to get callbacks when events happen on the layer.
    [GLYMapHelper subscribeListener:self onMapLayerPlaces:_placesLayer];
    
    // Allow the user to select a custom place by tapping a location.
    _placesLayer->enableDroppedPin();
    
    // Add the layer to the map
    _manager->addMapLayer(_placesLayer);

    // Create GPlaces for all the destinations we want to show
    _placeSpaceNeedle = Glympse::GlympseFactory::createPlace(47.6204, -122.3491, Glympse::CoreFactory::createString("Space Needle"));
    _placePioneerSquare = Glympse::GlympseFactory::createPlace(47.6, -122.332, Glympse::CoreFactory::createString("Pioneer Square"));
    _placeSeattleLibrary = Glympse::GlympseFactory::createPlace(47.606667, -122.332778, Glympse::CoreFactory::createString("Seattle Public Library"));
    _placePikePlaceMarket = Glympse::GlympseFactory::createPlace(47.609425, -122.3417, Glympse::CoreFactory::createString("Pike Place Market"));
    _placeCenturyLinkField = Glympse::GlympseFactory::createPlace(47.5952, -122.3316, Glympse::CoreFactory::createString("Century Link Field"));
    _placeSafecoField = Glympse::GlympseFactory::createPlace(47.591389, -122.3325, Glympse::CoreFactory::createString("Safeco Field"));
    
    // A place with no title will not show a title bubble
    _placeSwedishMedicalCenter = Glympse::GlympseFactory::createPlace(47.608984, -122.32182, Glympse::CoreFactory::createString(""));
    
    _placesLayer->addPlace(_placeSpaceNeedle);
    _placesLayer->addPlace(_placePioneerSquare);
    _placesLayer->addPlace(_placeSeattleLibrary);
    
    // A place can also be added witha custom style to define the marker color
    Glympse::GPrimitive style = Glympse::CoreFactory::createPrimitive(Glympse::CoreConstants::PRIMITIVE_TYPE_OBJECT);
    style->put(Glympse::CoreFactory::createString("destination_color"), Glympse::CoreFactory::createString("#68a9ea"));
    
    _placesLayer->addPlace(_placePikePlaceMarket, style);
    _placesLayer->addPlace(_placeCenturyLinkField, style);
    _placesLayer->addPlace(_placeSafecoField, style);
    _placesLayer->addPlace(_placeSwedishMedicalCenter, style);
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void)placeWasSelected:(const Glympse::GPlace&)place onPlacesLayer:(const Glympse::GMapLayerPlaces&)layer
{
    _placesLayer->setFollowingMode(Glympse::MapConstants::FOLLOWING_MODE_SELECTED_PLACE);
}

-(void)pinWasDropped:(const Glympse::GPlace&)place onPlacesLayer:(const Glympse::GMapLayerPlaces&)layer
{
    
}

- (IBAction)buttonLogoPressed:(id)sender
{
    if( NO == _isUsingCustomDrawables )
    {
        // We can provide custom GDrawables to use inplace of the default markers
        UIImage *selectedLogo = [UIImage imageNamed:@"logo_selected"];
        Glympse::GDrawable drawableSelected = Glympse::CoreFactory::createDrawable((__bridge void *)selectedLogo);
        
        UIImage *unselectedLogo = [UIImage imageNamed:@"logo_unselected"];
        Glympse::GDrawable drawableUnselected = Glympse::CoreFactory::createDrawable((__bridge void *)unselectedLogo);
        
        _placesLayer->setPlacesStateDrawable(Glympse::MapConstants::PLACE_STATE_DRAWABLE_SELECTED, drawableSelected);
        _placesLayer->setPlacesStateDrawable(Glympse::MapConstants::PLACE_STATE_DRAWABLE_UNSELECTED, drawableUnselected);
        
        _isUsingCustomDrawables = YES;
    }
    else
    {
        // Setting the marker drawables to NULL will revert to the default markers
        _placesLayer->setPlacesStateDrawable(Glympse::MapConstants::PLACE_STATE_DRAWABLE_SELECTED, NULL);
        _placesLayer->setPlacesStateDrawable(Glympse::MapConstants::PLACE_STATE_DRAWABLE_UNSELECTED, NULL);
        
        _isUsingCustomDrawables = NO;
    }
}
@end
