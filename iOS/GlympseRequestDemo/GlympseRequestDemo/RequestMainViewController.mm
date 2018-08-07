//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "RequestMainViewController.h"

#import "GLYSMSJob.h"
#import "GlympseWrapperARC.h"

@interface RequestMainViewController () <GLYEventListener, GLYSMSJobDelegate, UITextFieldDelegate>
{
    NSString *_groupName;
}
@end


@implementation RequestMainViewController

#pragma mark - Object Lifecycle

- (void)dealloc
{
    [self enableObservingGlympsePlatformEvents:NO];
    
    [_requestGlympseBtn release];
    [_groupUrlCtrl release];
    [_explainGroupTypeLbl release];
    [_explainPrivateTypeLbl release];
    [_selectTypeCtrl release];
    [_requestAddressCtrl release];
    [_viewerAddressCtrl release];
    [super dealloc];
}

#pragma mark - View Lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self enableObservingGlympsePlatformEvents:YES];
    
    _groupName = [self restoreGroupName];
    self.groupUrlCtrl.text = [NSString stringWithFormat:@"http://%s/%@",
                              [GlympseWrapper instance].serverAddress->toCharArray(), _groupName];
}


#pragma mark - View Event Handlers

- (IBAction)requestGlympseBtn_TouchUpInside:(id)sender
{
    [self hideKeyboard];
    [self requestGlympse];
}

- (IBAction)selectTypeCtrl_ValueChanged:(id)sender
{
    [self hideKeyboard];
    
    if (self.selectTypeCtrl.selectedSegmentIndex == 0)
    {
        self.explainPrivateTypeLbl.hidden = NO;
        self.explainGroupTypeLbl.hidden = YES;
        self.viewerAddressCtrl.hidden = NO;
        self.groupUrlCtrl.hidden = YES;
        [self.viewerAddressCtrl becomeFirstResponder];
    }
    else
    {
        self.explainPrivateTypeLbl.hidden = YES;
        self.explainGroupTypeLbl.hidden = NO;
        self.viewerAddressCtrl.hidden = YES;
        self.groupUrlCtrl.hidden = NO;
        [self.viewerAddressCtrl resignFirstResponder];
    }
}

- (IBAction)tapRecognizer_selector:(UITapGestureRecognizer *)sender
{
    // Obnoxious keyboard dismissal code
    if (sender.state == UIGestureRecognizerStateEnded)
    {
        [self hideKeyboard];
    }
}

#pragma mark - View Helper Methods

- (void)showStatusAlert:(NSString *)title withMessage:(NSString *)message
{
    NSLog(@"StatusAlert '%@': %@", title, message);
    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:title
                                                        message:message
                                                       delegate:nil
                                              cancelButtonTitle:nil
                                              otherButtonTitles:@"OK", nil];
    [alertView show];
    [alertView autorelease];
}

- (void)hideKeyboard
{
    if (self.requestAddressCtrl.isFirstResponder)
    {
        [self.requestAddressCtrl resignFirstResponder];
    }
    if (self.viewerAddressCtrl.isFirstResponder)
    {
        [self.viewerAddressCtrl resignFirstResponder];
    }
}
#pragma mark - Glympse Helper Methods

/**
 * Glympse: toggle notification observing
 */
- (void)enableObservingGlympsePlatformEvents:(BOOL)doObserve
{
    if (doObserve)
    {
        [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    }
    else
    {
        [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse];
    }
}

- (void)requestGlympse
{
    NSLog(@"Requesting Glympse from '%@'...", self.requestAddressCtrl.text);

    if (self.selectTypeCtrl.selectedSegmentIndex == 0)
    {
        [self sendRegularRequest];
    }
    else
    {
        [self sendGroupRequest];
    }
}

- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if (listener == Glympse::GE::LISTENER_TICKET)
    {
        if (0 != (events & Glympse::GE::TICKET_REQUEST_CREATED))
        {
            Glympse::GTicket ticket = (Glympse::GTicket)object;
            [self processUnsentInvites:ticket];
        }
        else if(0 != (events & Glympse::GE::TICKET_REQUEST_SENT))
        {
            Glympse::GTicket ticket = (Glympse::GTicket)object;
            [self showStatusAlert:@"Request Sent!" withMessage:@""];
            [GLYGlympse unsubscribe:self onSink:ticket];
        }
        else if(0 != (events & Glympse::GE::TICKET_REQUEST_FAILED))
        {
            Glympse::GTicket ticket = (Glympse::GTicket)object;
            [self showStatusAlert:@"Request Failed!" withMessage:@""];
            [GLYGlympse unsubscribe:self onSink:ticket];
        }
    }
    else if(listener == Glympse::GE::LISTENER_PLATFORM)
    {
        if (0 != (events & Glympse::GE::PLATFORM_INVITE_TICKET))
        {
            Glympse::GUserTicket userTicket = (Glympse::GUserTicket)object;
            Glympse::GString inviteeName = userTicket->getUser()->getNickname();
            Glympse::GString inviteUrl = userTicket->getTicket()->getCode();
            self.groupUrlCtrl.text = [NSString stringWithUTF8String:inviteUrl->toCharArray()];
        }
    }
}

- (NSString *)restoreGroupName
{

    NSString *groupName = [[NSUserDefaults standardUserDefaults] stringForKey:@"GlympseRequestDemo_GroupName"];
    if ( groupName == nil || groupName.length == 0 )
    {
        groupName = [NSString stringWithFormat:@"!%@", [[NSUUID UUID] UUIDString]];
        [[NSUserDefaults standardUserDefaults] setObject:groupName forKey:@"GlympseRequestDemo_GroupName"];
    }
    return groupName;
}

- (BOOL)sendRegularRequest
{    
    // Check input:
    
    NSString *invitee = self.requestAddressCtrl.text;
    NSString *viewer = self.viewerAddressCtrl.text;
    Glympse::GString recipientPhone = Glympse::CoreFactory::createString([invitee UTF8String]);
    Glympse::GString requesterPhone = Glympse::CoreFactory::createString([viewer UTF8String]);
    
    // Create invites:
    
    // This invite goes to the recipient of the request. Instead of allowing recipient to view a glympse, it will
    //  be used to prompt them to send a glympse, instead. (NOTE: This invite should NOT be added to the ticket below.)
    Glympse::GInvite requestInvite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_UNKNOWN,
                                                                           NULL, recipientPhone);
    // Test if our passing INVITE_TYPE_UNKNOWN forced type-detection of a phone number:
    if (requestInvite == NULL || requestInvite->getType() != Glympse::GC::INVITE_TYPE_SMS)
    {
        NSString *error = [NSString stringWithFormat:@"The 1st entry '%@' is not a valid phone number.", invitee];
        [self showStatusAlert:@"Unable to Send Request" withMessage:error];
        return FALSE;
    }
    // Get self user for nickname.
    Glympse::GUser userMe = [GlympseWrapper instance].glympse->getUserManager()->getSelf();
    
    // This invite goes to the requester, which is "self". It will allow viewing of the requested glympse
    //  (NOTE: Add this invite to the ticket below.)
    Glympse::GInvite selfInvite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_UNKNOWN,
                                                                        userMe->getNickname(), requesterPhone);
    // Test if our passing INVITE_TYPE_UNKNOWN forced type-detection of a phone number:
    if (selfInvite == NULL || selfInvite->getType() != Glympse::GC::INVITE_TYPE_SMS)
    {
        NSString *error = [NSString stringWithFormat:@"The 2nd entry '%@' is not a valid phone number.", viewer];
        [self showStatusAlert:@"Unable to Send Request" withMessage:error];
        return FALSE;
    }
    
    // Create ticket:
    
    int duration = 3600000;
    Glympse::GTicket ticket = Glympse::GlympseFactory::createTicket(duration, NULL, NULL);
    
    // Add viewing-invite to ticket
    ticket->addInvite(selfInvite);

    // Use factory helper to construct a ticket sink.
    Glympse::GTicket sink = Glympse::GlympseFactory::createRequest(ticket, requestInvite);

    [GLYGlympse subscribe:self onSink:sink];

    // Request glympse: pass container sink ticket
    [GlympseWrapper instance].glympse->requestTicket(sink);
    
    return TRUE;
}

- (BOOL)sendGroupRequest
{
    // Check input:
    
    NSString *invitee = self.requestAddressCtrl.text;

    Glympse::GString recipientPhone = Glympse::CoreFactory::createString([invitee UTF8String]);
    Glympse::GString groupName = Glympse::CoreFactory::createString([_groupName UTF8String]);
    
    // Create invites:
    
    // This invite goes to the recipient of the request. Instead of allowing recipient to view a glympse, it will
    //  be used to prompt them to send a glympse, instead. (NOTE: This invite should NOT be added to the ticket below.)
    Glympse::GInvite requestInvite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_UNKNOWN,
                                                                           NULL, recipientPhone);
    // Test if our passing INVITE_TYPE_UNKNOWN forced type-detection of a phone number:
    if (requestInvite == NULL || requestInvite->getType() != Glympse::GC::INVITE_TYPE_SMS)
    {
        NSString *error = [NSString stringWithFormat:@"The 1st entry '%@' is not a valid phone number.", invitee];
        [self showStatusAlert:@"Unable to Send Request" withMessage:error];
        return FALSE;
    }
    
    // This invite goes to the group. Joining/viewing the group will allow viewing of the requested glympse.
    //  (NOTE: Add this invite to the ticket below.)
    Glympse::GInvite selfInvite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_GROUP,
                                                                        NULL, groupName);
    
    // Create ticket:
    
    int duration = 3600000;
    Glympse::GTicket ticket = Glympse::GlympseFactory::createTicket(duration, NULL, NULL);
    
    // Add viewing-invite to ticket
    ticket->addInvite(selfInvite);

    // Use factory helper to construct a ticket sink.
    Glympse::GTicket sink = Glympse::GlympseFactory::createRequest(ticket, requestInvite);

    [GLYGlympse subscribe:self onSink:sink];

    // Request glympse: pass container sink ticket
    [GlympseWrapper instance].glympse->requestTicket(sink);

    return TRUE;
}

- (void)processUnsentInvites:(Glympse::GTicket)ticket
{
    Glympse::GArray<Glympse::GInvite>::ptr invites = ticket->getInvites();
    int count = invites->length();
    for ( int index = 0 ; index < count ; ++index )
    {
        Glympse::GInvite invite = invites->at(index);
        if (Glympse::GC::INVITE_STATE_NEEDTOSEND == invite->getState() )
        {
            if(invite->getType()==Glympse::GC::INVITE_TYPE_SMS)
            {
                [GLYSMSJob schedule:invite forTicket:ticket asRequest:YES delegate:self];
            }
            else
            {
                [self showStatusAlert:@"Unhandled Invite"
                          withMessage:[NSString stringWithFormat:@"Invite of type '%d' needs client-side send \
                                       and wasn't handled.", invite->getType()]];
            }
        }
    }
    
}
#pragma mark - GLYSMSJobDelegate methods

- (void)jobCompleteWithSuccess:(id)sender forInvite:(const Glympse::GInvite &)invite forTicket:(const Glympse::GTicket &)ticket asRequest:(BOOL)isRequest
{
    [self glympseEvent:[GlympseWrapper instance].glympse listener:Glympse::GE::LISTENER_TICKET events:(isRequest ? Glympse::GE::TICKET_REQUEST_SENT : Glympse::GE::TICKET_INVITE_SENT) object:ticket];
}

- (void)jobCancelled:(id)sender forInvite:(const Glympse::GInvite &)invite forTicket:(const Glympse::GTicket &)ticket asRequest:(BOOL)isRequest
{
    [self glympseEvent:[GlympseWrapper instance].glympse listener:Glympse::GE::LISTENER_TICKET events:(isRequest ? Glympse::GE::TICKET_REQUEST_FAILED : Glympse::GE::TICKET_INVITE_FAILED) object:ticket];
}

- (void)jobCompleteWithFailure:(id)sender forInvite:(const Glympse::GInvite &)invite forTicket:(const Glympse::GTicket &)ticket asRequest:(BOOL)isRequest
{
    [self glympseEvent:[GlympseWrapper instance].glympse listener:Glympse::GE::LISTENER_TICKET events:(isRequest ? Glympse::GE::TICKET_REQUEST_FAILED : Glympse::GE::TICKET_INVITE_FAILED) object:ticket];
}

- (void)jobCompleteWithSuccess:(id)sender forInvite:(const Glympse::GInvite &)invite forGroup:(const Glympse::GGroup &)group
{
    [self glympseEvent:[GlympseWrapper instance].glympse listener:Glympse::GE::LISTENER_GROUPS events:Glympse::GE::GROUP_INVITE_SENT object:group];
}

- (void)jobCancelled:(id)sender forInvite:(const Glympse::GInvite &)invite forGroup:(const Glympse::GGroup &)group
{
    [self glympseEvent:[GlympseWrapper instance].glympse listener:Glympse::GE::LISTENER_GROUPS events:Glympse::GE::GROUP_INVITE_FAILED object:group];
}

- (void)jobCompleteWithFailure:(id)sender forInvite:(const Glympse::GInvite &)invite forGroup:(const Glympse::GGroup &)group
{
    [self glympseEvent:[GlympseWrapper instance].glympse listener:Glympse::GE::LISTENER_GROUPS events:Glympse::GE::GROUP_INVITE_FAILED object:group];
}

#pragma mark - UITextFieldDelegate
- (BOOL)textFieldShouldReturn:(UITextField *)textField
{
    [self hideKeyboard];
    return YES;
}

@end











