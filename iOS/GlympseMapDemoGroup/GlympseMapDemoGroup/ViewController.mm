//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "ViewController.h"
#import "GlympseWrapperARC.h"

@interface ViewController () <GLYEventListener>

@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse->getGroupManager()];
    [self.map attachGlympse:[GlympseWrapper instance].glympse];
    
    // Create the Places layer and set desired options
    _manager = [_map mapManager];
    _groupLayer = Glympse::MapFactory::createMapLayerGroup();
    
    // We want to get callbacks when events happen on the layer.
    [GLYMapHelper subscribeListener:self onMapLayerGroup:_groupLayer];
    
    // Add the layer to the map
    _manager->addMapLayer(_groupLayer);
    
    // Create the basic styling we want for all users
    _baseStyle = Glympse::CoreFactory::createPrimitive(Glympse::CC::PRIMITIVE_TYPE_OBJECT);
    
    _baseStyle->put(Glympse::MapConstants::STYLE_SELF_USER_ICON_COLOR_KEY(), Glympse::CoreFactory::createString("#4d2961"));
    _baseStyle->put(Glympse::MapConstants::STYLE_SELF_DESTINATION_COLOR_KEY(), Glympse::CoreFactory::createString("#6d95be"));
    _baseStyle->put(Glympse::MapConstants::STYLE_SELF_TRAIL_COLOR_KEY(), Glympse::CoreFactory::createString("#163f68"));
    _baseStyle->put(Glympse::MapConstants::STYLE_SELF_ROUTE_COLOR_KEY(), Glympse::CoreFactory::createString("#163f68"));

    _baseStyle->put(Glympse::MapConstants::STYLE_USER_ICON_COLOR_KEY(), Glympse::CoreFactory::createString("#10267b"));
    _baseStyle->put(Glympse::MapConstants::STYLE_DESTINATION_COLOR_KEY(), Glympse::CoreFactory::createString("#8d6480"));
    _baseStyle->put(Glympse::MapConstants::STYLE_TRAIL_COLOR_KEY(), Glympse::CoreFactory::createString("#645471"));
    _baseStyle->put(Glympse::MapConstants::STYLE_ROUTE_COLOR_KEY(), Glympse::CoreFactory::createString("#645471"));
    
    _baseStyle->put(Glympse::MapConstants::STYLE_EXPIRED_COLOR_KEY(), Glympse::CoreFactory::createString("#d2d8d8"));
    
    _baseStyle->put(Glympse::MapConstants::STYLE_USER_ICON_VISIBLE_KEY(), true);
    _baseStyle->put(Glympse::MapConstants::STYLE_USER_BUBBLE_VISIBLE_KEY(), true);
    _baseStyle->put(Glympse::MapConstants::STYLE_DESTINATION_VISIBLE_KEY(), true);
    _baseStyle->put(Glympse::MapConstants::STYLE_DESTINATION_BUBBLE_VISIBLE_KEY(), true);
    
    _baseStyle->put(Glympse::MapConstants::STYLE_TRAIL_VISIBLE_KEY(), true);
    _baseStyle->put(Glympse::MapConstants::STYLE_ROUTE_VISIBLE_KEY(), true);
    
    // Apply the defautl style
    _groupLayer->setDefaultStyle(_baseStyle);
    
    // Create the accent style we will apply to the active user
    _accentStyle = Glympse::CoreFactory::createPrimitive(Glympse::CC::PRIMITIVE_TYPE_OBJECT);
    
    _accentStyle->put(Glympse::MapConstants::STYLE_SELF_USER_ICON_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    _accentStyle->put(Glympse::MapConstants::STYLE_SELF_DESTINATION_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    _accentStyle->put(Glympse::MapConstants::STYLE_SELF_TRAIL_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    _accentStyle->put(Glympse::MapConstants::STYLE_SELF_ROUTE_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    
    _accentStyle->put(Glympse::MapConstants::STYLE_USER_ICON_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    _accentStyle->put(Glympse::MapConstants::STYLE_DESTINATION_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    _accentStyle->put(Glympse::MapConstants::STYLE_TRAIL_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));
    _accentStyle->put(Glympse::MapConstants::STYLE_ROUTE_COLOR_KEY(), Glympse::CoreFactory::createString("#ff0000"));

}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)buttonViewGroupPressed:(id)sender
{
    // Clean up entered group name
    NSCharacterSet *charsToRemove = [NSCharacterSet characterSetWithCharactersInString:@" !"];
    NSString *cleaned = [self.textFieldGroupName.text stringByTrimmingCharactersInSet:charsToRemove];
    Glympse::GString groupName = Glympse::CoreFactory::createString([cleaned UTF8String]);
    
    // Check to see if a group with this name has already been view.
    Glympse::GGroup group = NULL;
    
    Glympse::GArray<Glympse::GGroup>::ptr groups = [GlympseWrapper instance].glympse->getGroupManager()->getGroups();
    Glympse::int32 count = groups->length();
    
    for( int i = 0; i < count; i++ )
    {
        // Check if group names match
        Glympse::GGroup candidateGroup = groups->at(i);
        Glympse::GString candidateName = candidateGroup->getName();
        if( NULL != candidateName && candidateName->equals(groupName) )
        {
            group = candidateGroup;
            break;
        }
    }
    
    if( NULL == group )
    {
        // Group hasn't been viewed before, so we will do it now. The group will be added to the layer from the GROUPS_GROUP_ADDED event
        group = [GlympseWrapper instance].glympse->getGroupManager()->viewGroup(groupName);
    }
    else
    {
        // Group already exists in the GroupManager. We just need to add it to the GroupLayer.
        [self viewGroup:group];
    }
}

-(void)viewGroup:(const Glympse::GGroup&)group
{
    [GLYGlympse subscribe:self onSink:group];
    _groupLayer->setGroup(group);
    [GlympseWrapper instance].glympse->getGroupManager()->startTracking(group);
}

- (void)userLocationWasTapped:(const Glympse::GUser&)user onGroupLayer:(const Glympse::GMapLayerGroup&)layer
{
    // Apply the base style to the previous active user.
    if ( NULL != _activeUser )
    {
        _groupLayer->setUserStyle(_activeUser, _baseStyle);
    }
    
    // Apply the accent style to the new active user.
    _groupLayer->setUserStyle(user, _accentStyle);
    
    // Set the new active user.
    _activeUser = user;
    _groupLayer->setActiveUser(user);
}

- (void)userDestinationWasTapped:(const Glympse::GUser&)user onGroupLayer:(const Glympse::GMapLayerGroup&)layer
{
    
}

- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if (  Glympse::GE::LISTENER_GROUPS == listener )
    {
        if ( 0 != ( events &  Glympse::GE::GROUPS_GROUP_ADDED ) )
        {
            Glympse::GGroup group = (Glympse::GGroup)object;
            [self viewGroup:group];
        }
        else if ( 0 != ( events &  Glympse::GE::GROUPS_GROUP_REMOVED ) )
        {
            Glympse::GGroup group = (Glympse::GGroup)object;
        }
    }
    if (  Glympse::GE::LISTENER_GROUP == listener )
    {
        if ( 0 != ( events &  Glympse::GE::GROUP_INVALID_CODE ) )
        {
            
        }
    }
    if (  Glympse::GE::LISTENER_PLATFORM == listener )
    {

    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    [textField resignFirstResponder];
    return NO;
}



@end
