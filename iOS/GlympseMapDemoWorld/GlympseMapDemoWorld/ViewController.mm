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

    _isFollowingLocked = FALSE;
    
    [self.buttonLock setImage:[UIImage imageNamed:@"unlocked.png"] forState:UIControlStateNormal];
    
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    [_map attachGlympse:[GlympseWrapper instance].glympse];
    
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse->getUserManager()];
    
    Glympse::GMapManager manager = [_map mapManager];
    Glympse::GPrimitive config = Glympse::CoreFactory::createPrimitive(Glympse::CC::PRIMITIVE_TYPE_OBJECT);
    config->put(Glympse::MapConstants::CONFIGURATION_SPEED_KEY(), Glympse::MapConstants::CONFIGURATION_SPEED_MPH());
    manager->setConfiguration(config);
    
    _worldLayer = Glympse::MapFactory::createMapLayerWorld();

    [GLYMapHelper subscribeListener:self onMapLayerWorld:_worldLayer];

    manager->addMapLayer(_worldLayer);
    
    [GlympseWrapper instance].glympse->getLocationManager()->startLocation();
    
    _worldLayer->setActiveUser([GlympseWrapper instance].glympse->getUserManager()->getSelf());
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


/**
 * IMapLayerWorldListener
 */

-(void)userLocationWasTapped:(const Glympse::GUser&)user onWorldLayer:(const Glympse::GMapLayerWorld&)layer
{
    if ( NULL != _worldLayer )
    {
        _worldLayer->setActiveUser(user);
    }
}

-(void)userDestinationWasTapped:(const Glympse::GUser&)user onWorldLayer:(const Glympse::GMapLayerWorld&)layer
{

}

/**
 * IMapLockableLayerListener
 */

-(void)lockWasBrokenOnLayer:(const Glympse::GMapLockableLayer&) layer
{    
    [self.buttonLock setImage:[UIImage imageNamed:@"unlocked.png"] forState:UIControlStateNormal];
    _isFollowingLocked = false;
    self.buttonFollowUser.enabled = YES;
    self.buttonFollowUserAndDestination.enabled = YES;
    self.buttonFollowAll.enabled = YES;
}

/**
 * Internal
 */

- (IBAction)buttonViewTicketPressed:(id)sender
{
    NSCharacterSet *charsToRemove = [NSCharacterSet characterSetWithCharactersInString:@" "];
    NSString * _strTrimmedGroup = [self.textFieldInviteCode.text stringByTrimmingCharactersInSet:charsToRemove];
    
    Glympse::GString code = Glympse::CoreFactory::createString([_strTrimmedGroup UTF8String]);
    
    Glympse::GEventSink inviteSink = [GlympseWrapper instance].glympse->decodeInvite(code, Glympse::GC::INVITE_MODE_PROMPT_BEFORE_VIEWING);
    
    [GLYGlympse subscribe:self onSink:inviteSink];
}

- (IBAction)buttonFollowUserPressed:(id)sender
{
    _worldLayer->setFollowingMode(Glympse::MapConstants::FOLLOWING_MODE_USER);
}

- (IBAction)buttonFollowUserAndDestinationPressed:(id)sender
{
    _worldLayer->setFollowingMode(Glympse::MapConstants::FOLLOWING_MODE_USER_AND_DESTINATION);
}

- (IBAction)buttonFollowAllPressed:(id)sender
{
    _worldLayer->setFollowingMode(Glympse::MapConstants::FOLLOWING_MODE_ALL);
}


- (IBAction)buttonLockPressed:(id)sender
{
    Glympse::GMapManager manager = [_map mapManager];
    
    if( _isFollowingLocked )
    {
        _worldLayer->unlockItems();

        [self.buttonLock setImage:[UIImage imageNamed:@"unlocked.png"] forState:UIControlStateNormal];
        _isFollowingLocked = false;
        self.buttonFollowUser.enabled = YES;
        self.buttonFollowUserAndDestination.enabled = YES;
        self.buttonFollowAll.enabled = YES;
    }
    else
    {
        if( NULL != _worldLayer )
        {
            [GLYMapHelper subscribeListener:self onMapLockableLayer:_worldLayer];
            _worldLayer->lockItems();
            
            [self.buttonLock setImage:[UIImage imageNamed:@"locked.png"] forState:UIControlStateNormal];
            _isFollowingLocked = true;
            self.buttonFollowUser.enabled = NO;
            self.buttonFollowUserAndDestination.enabled = NO;
            self.buttonFollowAll.enabled = NO;
        }
    }
}

- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if (  Glympse::GE::LISTENER_INVITE == listener )
    {
        if ( 0 != ( events &  Glympse::GE::INVITE_DECODED ) )
        {
            Glympse::GString code = (Glympse::GString)object;
            Glympse::GUser user = glympse->getUserManager()->findUserByInviteCode(code);
            
        }
        else if ( 0 != ( events &  Glympse::GE::INVITE_INVALID_CODE ) )
        {
            Glympse::GString code = (Glympse::GString)object;
        }
    }
    if (  Glympse::GE::LISTENER_PLATFORM == listener )
    {
        if ( 0 != ( events &  Glympse::GE::PLATFORM_INVITE_TICKET ) )
        {
            Glympse::GUserTicket userTicket = (Glympse::GUserTicket)object;
            glympse->viewTicket(userTicket);
            glympse->getUserManager()->startTracking(userTicket->getUser());
        }
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    [textField resignFirstResponder];
    return NO;
}

@end
