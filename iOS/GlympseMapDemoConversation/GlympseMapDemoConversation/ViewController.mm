//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "ViewController.h"
#import "GlympseWrapperARC.h"
#import "Core/Core.h"

@interface ViewController () <GLYEventListener>

@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    _users = [NSMutableArray array];
    _userCells = [NSMutableArray array];
    
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    
    [self.map attachGlympse:[GlympseWrapper instance].glympse];
    
    // Create the Places layer and set desired options
    _manager = [_map mapManager];
    _conversationLayer = Glympse::MapFactory::createMapLayerConversation();
    _conversationLayer->setTrackingSelfUserEnabled(TRUE);
    
    // We want to get callbacks when events happen on the layer.
    [GLYMapHelper subscribeListener:self onMapLayerConversation:_conversationLayer];

    // Add the layer to the map
    _manager->addMapLayer(_conversationLayer);
    
    Glympse::GUser selfUser = [GlympseWrapper instance].glympse->getUserManager()->getSelf();
    
    _conversationLayer->addUser(selfUser);
    
    [_users addObject:[self wrapUser:selfUser]];
    
    [self refreshUserBar];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)refreshUserBar
{
    int offset = 0.0f;
    
    for(GLYUserBarCellViewController *userCell : _userCells)
    {
        [userCell.view removeFromSuperview];
    }
    
    [_userCells removeAllObjects];
    
    for( id<GCommonWrapper> wrappedUser : _users)
    {
        Glympse::GUser user = [self unwrapUser:wrappedUser];
        
        GLYUserBarCellViewController *userCell = [[GLYUserBarCellViewController alloc] initWithNibName:@"GLYUserBarCellViewController" bundle:[NSBundle mainBundle]];
        [userCell setSelectionListener:self];
        [userCell setUser:user];
        
        if( user == _activeUser )
        {
            [userCell setSelected:YES];
        }
        else
        {
            [userCell setSelected:NO];
        }
        
        CGRect frame = userCell.view.frame;
        [userCell.view setFrame:CGRectMake(offset, frame.origin.y, frame.size.width, frame.size.height)];
        offset += frame.size.width;
        
        [self.scrollViewUsers addSubview:userCell.view];
        [_userCells addObject:userCell];
    }
}

- (id<GCommonWrapper>)wrapUser:(Glympse::GUser)user
{
    id<GCommonWrapper> wrappedUser = [GLYGlympse wrapGCommon:user];
    return wrappedUser;
}

- (Glympse::GUser)unwrapUser:(id<GCommonWrapper>)wrappedUser
{
    Glympse::GUser user = (Glympse::GUser)[wrappedUser unwrap];
    return user;
}

- (void)selectUser:(Glympse::GUser)user
{
    _activeUser = user;
    _conversationLayer->setFocusedUser(_activeUser);
    [self refreshUserBar];
}

-(void)userCellWasSelected:(Glympse::GUser)user
{
    [self selectUser:user];
}

- (IBAction)buttonAddTickedPressed:(id)sender
{
    NSCharacterSet *charsToRemove = [NSCharacterSet characterSetWithCharactersInString:@" "];
    NSString * _strTrimmedGroup = [self.textFieldInviteCode.text stringByTrimmingCharactersInSet:charsToRemove];
    
    Glympse::GString code = Glympse::CoreFactory::createString([_strTrimmedGroup UTF8String]);
    
    Glympse::GEventSink inviteSink = [GlympseWrapper instance].glympse->decodeInvite(code, Glympse::GC::INVITE_MODE_PROMPT_BEFORE_VIEWING);
        
    [GLYGlympse subscribe:self onSink:inviteSink];
}

- (IBAction)buttonRemoveActivePressed:(id)sender
{
    if ( NULL != _activeUser )
    {
        _conversationLayer->removeUser(_activeUser);
        
        id<GCommonWrapper> wrappedUserToRemove = NULL;
        
        for( id<GCommonWrapper> wrappedUser : _users)
        {
            Glympse::GUser user = [wrappedUser unwrap];
            if( _activeUser == user )
            {
                wrappedUserToRemove = wrappedUser;
                break;
            }
        }
        
        if( NULL != wrappedUserToRemove )
        {
            [_users removeObject:wrappedUserToRemove];
        }
        
        _activeUser = NULL;
    }
    [self refreshUserBar];
}

- (void)userLocationWasTapped:(const Glympse::GUser&)user onConversationLayer:(const Glympse::GMapLayerConversation&)layer
{
    [self selectUser:user];
}

- (void)userDestinationWasTapped:(const Glympse::GUser&)user onConversationLayer:(const Glympse::GMapLayerConversation&)layer
{
    
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
            
            [_users addObject:[self wrapUser:user]];
            _conversationLayer->addUser(user);
            
            [self refreshUserBar];
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
        else if ( 0 != ( events &  Glympse::GE::PLATFORM_SYNCED_WITH_SERVER ) )
        {
            [self refreshUserBar];
        }
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    [textField resignFirstResponder];
    return NO;
}


@end
