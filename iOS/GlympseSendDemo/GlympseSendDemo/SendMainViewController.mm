//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "SendMainViewController.h"

#import "GLYTimerVC.h"
#import "GLYSMSJob.h"
#import "GlympseWrapperARC.h"

@interface SendMainViewController ()
<GLYEventListener, GLYTimerVCDelegate, GLYSMSJobDelegate, UITextViewDelegate, UIAlertViewDelegate>
{
    Glympse::GTicket _glympseTicket;
}
@end

@implementation SendMainViewController

#pragma mark - Object Lifecycle

- (void)dealloc
{
    [self enableObservingGlympsePlatformEvents:NO];
    
    if (self.refreshTimer != nil)
    {
        [self.refreshTimer invalidate];
        self.refreshTimer = nil;
    }
    
    [_sendGlympseBtn release];
    [_recipientsCtrl release];
    [_durationStatusCtrl release];
    [_watchedStatusCtrl release];
    [_actionsCtrl release];
    [_invitesCtrl release];
    [_messageCtrl release];
    [_configureCtrl release];
    [_recipientsLbl release];
    [super dealloc];
}

#pragma mark - View Lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self updateGlympseTicketUi];
    
    [self enableObservingGlympsePlatformEvents:YES];
    
}

#pragma mark - View/Control Event Handlers

- (IBAction)sendGlympseBtn_TouchUpInside:(id)sender
{
    [self hideKeyboard];
    
    // if we dont have a valid, unsent ticket: bail
    if ( ![self isTicketInConfig:_glympseTicket] )
    {
        return;
    }

    if (![_invitesCtrl.text hasPrefix:@"email address"])
    {
        NSArray *substrings = [self.invitesCtrl.text componentsSeparatedByString:@","];
        
        Glympse::GString recipientAddress = NULL;
        Glympse::GInvite invite = NULL;
        bool added = false;
        for (NSString *inviteText in substrings)
        {
            recipientAddress = Glympse::CoreFactory::createString([inviteText UTF8String]);
            
            // Pass INVITE_TYPE_UNKNOWN to perform type-autodetection based on the address
            // Only works for these types: INVITE_TYPE_SMS, INVITE_TYPE_EMAIL, INVITE_TYPE_GROUP, INVITE_TYPE_TWITTER
            invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_UNKNOWN, NULL, recipientAddress);
            if (invite == NULL)
            {
                
                NSLog(@"Unable to detect invite type from the address: '%@'", inviteText);
            }
            
            added = _glympseTicket->addInvite(invite);
            if (!added)
            {
                NSLog(@"Unable to add invite: an invite with matching address probably already exists in the ticket.");
            }
        }
    }
    
    [self sendGlympseTicket];
}

- (IBAction)configureCtrl_ValueChanged:(id)sender
{
    [self hideKeyboard];
    
    switch (_configureCtrl.selectedSegmentIndex)
    {
        case 0:
            [self showGlympseTimer];
            break;
        case 1:
            [self addHardcodedInvites];
            break;
        default:
            break;
    }
}

- (IBAction)actionsCtrl_ValueChanged:(id)sender
{
    [self hideKeyboard];
    
    switch (_actionsCtrl.selectedSegmentIndex)
    {
        case 0:
            [self expireGlympse];
            break;
        case 1:
            [self plusFifteenMins];
            break;
        case 2:
            [self modifyGlympse];
            break;
        case 3:
            [self showGlympseTimer];
            break;
        default:
            break;
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

- (void)hideKeyboard
{
    if (_invitesCtrl.isFirstResponder)
    {
        [_invitesCtrl resignFirstResponder];
    }
    
    if (_messageCtrl.isFirstResponder)
    {
        [_messageCtrl resignFirstResponder];
    }
}

- (void)showStatusAlert:(NSString *)title withMessage:(NSString *)message
{
    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:title
                                                        message:message
                                                       delegate:nil
                                              cancelButtonTitle:nil
                                              otherButtonTitles:@"OK", nil];
    [alertView show];
    [alertView autorelease];
}

#pragma mark - Glympse: Platform Event Handler

/**
 * Respond to Glympse platform events here
 * Glympse sends this message on the same thread you started it, so its safe to update the UI directly from here
 */


- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if (0 != (listener & Glympse::GE::LISTENER_TICKET))
    {
        if (0 != (events & Glympse::GE::TICKET_CREATED))
        {
            if (_glympseTicket != NULL && _glympseTicket->equals(object))
            {
                [self updateGlympseTicketUi];
            }
        }
        else if (0 != (events & Glympse::GE::TICKET_INVITE_CREATED))
        {
            // This event only happens for invites that need to be delivered by the client (e.g. GC::INVITE_TYPE_SMS)
            if (_glympseTicket != NULL && _glympseTicket->equals(object))
            {
                [self processGlympseTicketUnsentInvites];

                [self updateGlympseTicketUi];
            }
        }
        else if (0 != (events & Glympse::GE::TICKET_INVITE_SENT)) //fired after ticket_created
        {
            if (_glympseTicket != NULL && _glympseTicket->equals(object))
            {
                // Update UI for each invite sent: this will catch newly added invites from a "Modify"
                [self updateGlympseTicketUi];
            }
        }
        else if (0 != (events & Glympse::GE::TICKET_EXPIRED))
        {
            // If the expired ticket is our Demo's ticket: stop refresh timer and update UI one last time.
            //  Note: This is necessary because a Glympse account could have multiple active tickets that expired!
            //        However, for this Demo, we are trying to illustrate how to interact with just a single ticket.
            if (_glympseTicket != NULL && _glympseTicket->equals(object))
            {
                
                [self clearGlympseTicketWithMessage:@"Ticket Expired"];
                
            }
        }
        else if (0 != (events & Glympse::GE::TICKET_FAILED))
        {
            if (_glympseTicket != NULL && _glympseTicket->equals(object))
            {
                if (0 != (_glympseTicket->getState() & Glympse::GC::TICKET_STATE_FAILED_TO_CREATE))
                {
                    [self clearGlympseTicketWithMessage:@"Ticket creation failed."];
                }
            }
        }
        else
        {
            // Log intentionally unhandled events just to give a better picture of ticket event activity
            NSLog(@"Unhandled Ticket Event: %4x", events);
        }
    }
    else if(0 != (listener & Glympse::GE::LISTENER_PLATFORM))
    {
        if (0 != (events & Glympse::GE::PLATFORM_SYNCED_WITH_SERVER))
        {
            [self restoreOrCreateTicketForUse];
            
            [self updateGlympseTicketUi];
        }
        else if (0 != (events & Glympse::GE::PLATFORM_TICKET_REMOVED))
        {
            // If the removed ticket is our Demo's ticket: stop refresh timer and update UI one last time.
            if (_glympseTicket != NULL && _glympseTicket->equals(object))
            {
                [self clearGlympseTicketWithMessage:@"Ticket was removed by platform."];
            }
        }
    }
    
    
}

#pragma mark - Glympse: Platform Methods
/**
 * Glympse: toggle notification observing
 */
- (void)enableObservingGlympsePlatformEvents:(BOOL)doObserve
{
    if (doObserve)
    {
        // Check if platform already Synced up: we started platform before this VC started listening to its events
        if ([GlympseWrapper instance].glympse->getHistoryManager()->isSynced())
        {
            [self restoreOrCreateTicketForUse];
            
            [self updateGlympseTicketUi];
        }
        
        [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    }
    else
    {
        [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse];
    }
}

- (BOOL)isTicketInConfig:(Glympse::GTicket)aTicket
{
    return ( aTicket != NULL && aTicket->getState() == Glympse::GC::TICKET_STATE_NONE );
}

- (BOOL)isTicketActive:(Glympse::GTicket)aTicket
{
    return ( aTicket != NULL && aTicket->isActive() );
}

- (BOOL)isTicketExpired:(Glympse::GTicket)aTicket
{
    return ( aTicket != NULL &&  _glympseTicket->getState() == Glympse::GC::TICKET_STATE_EXPIRED);
}

- (BOOL)isTicketFailed:(Glympse::GTicket)aTicket
{
    return ( aTicket != NULL &&  _glympseTicket->getState() == Glympse::GC::TICKET_STATE_FAILED_TO_CREATE);
}

/**
 * Glympse: get ticket object for sending or modifying: look for an active one that already exists, or create new one
 */
- (BOOL)restoreOrCreateTicketForUse
{
    // If the platform is synced, and we have no ticket yet, or our ticket isn't active...
    if ( [GlympseWrapper instance].glympse->getHistoryManager()->isSynced() && ![self isTicketActive:_glympseTicket] )
    {
        // Check existing tickets for an active one first:
        Glympse::GArray<Glympse::GTicket>::ptr tickets =
            [GlympseWrapper instance].glympse->getHistoryManager()->getTickets();
        
        for (int i = 0; i < tickets->length(); i++)
        {
            Glympse::GTicket aTicket = tickets->at(i);
            if ([self isTicketActive:aTicket])
            {
                _glympseTicket = aTicket;
                break; // Found a living ticket, stop checking tickets
            }
        }
        
        // If the above loop didn't find an active ticket...
        if (![self isTicketActive:_glympseTicket])
        {
            int startingDurationMs = 1000 * 60 * 5; // for this demo we pre-set duration of new ticket to 5 minutes
            // Create new empty ticket object for configuration
            _glympseTicket = Glympse::GlympseFactory::createTicket(startingDurationMs, NULL, NULL);
            
            assert([self isTicketInConfig:_glympseTicket]);
        }
        
        // Listen to ticket object for events
        [GLYGlympse subscribe:self onSink:_glympseTicket];
        
        return YES;
    }
    return NO;
}

- (void)processGlympseTicketUnsentInvites
{
    Glympse::GArray<Glympse::GInvite>::ptr invites = _glympseTicket->getInvites();
    int count = invites->length();
    for ( int index = 0 ; index < count ; ++index )
    {
        Glympse::GInvite invite = invites->at(index);
        if (Glympse::GC::INVITE_STATE_NEEDTOSEND == invite->getState() )
        {
            if(invite->getType()==Glympse::GC::INVITE_TYPE_SMS)
            {
                [GLYSMSJob schedule:invite forTicket:_glympseTicket asRequest:NO delegate:self];
            }
            else if(invite->getType()==Glympse::GC::INVITE_TYPE_CLIPBOARD)
            {
                // Update invite state to GC::INVITE_STATE_SUCCEEDED.
                invite->completeClientSideSend(true);
                
                UIPasteboard *pasteboard = [UIPasteboard generalPasteboard];
                pasteboard.string = [NSString stringWithUTF8String:invite->getUrl()->getBytes()];
            }
            else if(invite->getType()==Glympse::GC::INVITE_TYPE_LINK )
            {
                invite->completeClientSideSend(true);
                NSLog(@"Invite of type Link generated: %s", invite->getUrl()->getBytes());
                
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

- (void)clearGlympseTicketWithMessage:(NSString *)alertMessage
{
    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"Clearing Ticket"
                                                         message:alertMessage
                                                        delegate:self
                                               cancelButtonTitle:nil
                                               otherButtonTitles:@"OK", nil];
    [alertView show];
    //delegate always calls "clearGlympseTicket" on button press
}

- (void)clearGlympseTicket
{
    // Timer was created on the MainThread, so its safe to invalidate it here
    [self.refreshTimer invalidate];
    // Release timer
    self.refreshTimer = nil;
    
    // Stop listening to old ticket object
    [GLYGlympse unsubscribe:self onSink:_glympseTicket];
    
    // Release old ticket
    _glympseTicket = NULL;
    
    // Make a new ticket for configuration
    [self restoreOrCreateTicketForUse];
    
    // Manually update UI
    [self updateGlympseTicketUi];
}

/**
 * Glympse: begin location sharing and send invites
 */
- (void)sendGlympseTicket
{
    // if we dont have a valid, unsent ticket: bail
    if ( ![self isTicketInConfig:_glympseTicket] )
    {
        return;
    }
    if (_glympseTicket->getInvites()->length() <= 0)
    {
        NSLog(@"Ticket send aborted: Had no invites!");
        [self showStatusAlert:@"Ticket Send Aborted" withMessage:@"Ticket had no invites!"];
        return;
    }
    bool succeeded = [GlympseWrapper instance].glympse->sendTicket(_glympseTicket);
    if (!succeeded)
    {
        NSLog(@"Ticket send failed. Possible causes: Platform not started, NULL ticket passed, \
              ticket already sent, or ticket mid-send.");
        assert(false);
    }

}

#pragma mark -- Active Glympse actions
- (void)showGlympseTimer
{
    if (_glympseTicket == NULL)
    {
        return;
    }
    
    GLYTimerVC *timerVC = [[GLYTimerVC alloc] initWithNibName:@"GLYTimerVC" bundle:nil];
    timerVC.delegate = self;
    
    //For active tickets, get time remaining rather than starting duration
    if ([self isTicketActive:_glympseTicket])
    {
        Glympse::int64 currentTimeMs = [GlympseWrapper instance].glympse->getTime();
        Glympse::int64 expireTimeMs = _glympseTicket->getExpireTime();
        Glympse::int64 durationMs = expireTimeMs - currentTimeMs;
        timerVC.duration = (NSTimeInterval)durationMs/1000.0;
        timerVC.realTimeCountDown = YES;
    }
    else
    {
        timerVC.duration = (NSTimeInterval)_glympseTicket->getDuration()/1000.0; //convert from millisec to seconds
        timerVC.realTimeCountDown = NO;
    }
    
    [self presentViewController:timerVC animated:YES completion:NULL];
}

-(void)addHardcodedInvites
{
    if (![self isTicketInConfig:_glympseTicket])
    {
        return; // This method is for pre-send addition of invites only. 
    }
    
    Glympse::GString recipientName = NULL;
    Glympse::GString recipientAddress = NULL;
    Glympse::GInvite invite = NULL;
    bool succeeded = false;

    recipientName = Glympse::CoreFactory::createString("LINK");
    invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_LINK, recipientName, NULL);
    
    succeeded = _glympseTicket->addInvite(invite);
    if (!succeeded)
    {
        NSLog(@"LINK Invite creation failed.");
    }

    recipientName = Glympse::CoreFactory::createString("CLIPBOARD");
    invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_CLIPBOARD, recipientName, NULL);
    
    succeeded = _glympseTicket->addInvite(invite);
    if (!succeeded)
    {
        NSLog(@"CLIPBOARD Invite creation failed.");
    }
    
    recipientAddress = Glympse::CoreFactory::createString("!test");
    invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_GROUP, NULL, recipientAddress);
    
    succeeded = _glympseTicket->addInvite(invite);
    if (!succeeded)
    {
        NSLog(@"GROUP Invite creation failed.");
    }
    [self showStatusAlert:@"Added Hardcoded invites"
              withMessage:@"Added LINK, CLIPBOARD (try pasting the link into safari), and GROUP (!test) invites"];
}

- (void)expireGlympse
{
    if ([self isTicketActive:_glympseTicket])
    {
        _glympseTicket->modify(0, NULL, NULL);
    }
}

- (void)plusFifteenMins
{
    if ([self isTicketActive:_glympseTicket])
    {   
        // Calculate remaining time.
        int remainingMs = 0;
        if ( _glympseTicket->getId() == NULL )
        {
            // Ticket creation is already scheduled but request has not been received from server yet.
            // We already have Ticket::_expireTime populated (even though it is not yet synced with server).
            // In this case network lags can affect the accuracy of calculations below.
            Glympse::int64 currentTimeMs = [GlympseWrapper instance].glympse->getTime();
            remainingMs = (int)( _glympseTicket->getExpireTime() - currentTimeMs );
        }
        else
        {
            // One of the following 3 cases applies:

            // Ticket is not yet sent.
            // Ticket::_duration contains original ticket duration.

            // We are dealing with regular standalone ticket (regular flow).
            // This is the safest scenario. We can calculate remaining time reliably.
            
            // Ticket duration has just been updated but confirmation has not yet come from server.
            // In this case we assume that remaining time cached by previous modification
            // (stored in Ticket::__duration) takes effect as soon as response from server is received.
            
            remainingMs = _glympseTicket->getDuration();
        }
        
        // Add 15 minutes to current remaining time.
        remainingMs += 900000;
        
        // Modify the ticket. Ticket object will check whether platform is still running
        _glympseTicket->modify(remainingMs, NULL, NULL);
    }
}

- (void)modifyGlympse
{
    if ([self isTicketActive:_glympseTicket])
    {
        Glympse::GPlace destination = _glympseTicket->getDestination();
        Glympse::GString message = NULL;
        Glympse::GString destNameGStr = NULL;
        if (destination == NULL || destination->getName()->equals("Eiffel Tower"))
        {
            destNameGStr = Glympse::CoreFactory::createString("Seattle Space Needle");
            destination = Glympse::GlympseFactory::createPlace(47.620560, -122.349457, destNameGStr);
            message = Glympse::CoreFactory::createString("Hello, from Send Demo! I'm headed to Seattle.");
        }
        else
        {
            destNameGStr = Glympse::CoreFactory::createString("Eiffel Tower");
            destination = Glympse::GlympseFactory::createPlace(48.858532, 2.294748, destNameGStr);
            message = Glympse::CoreFactory::createString("Hello, from Send Demo! I'm headed to Paris.");
        }
        
        // Apply changed message and destination. Pass GC::DURATION_NO_CHANGE to leave duration unchanged.
        _glympseTicket->modify(Glympse::GC::DURATION_NO_CHANGE, message, destination);
        
        [self showStatusAlert:@"Modified Glympse"
                  withMessage:[NSString stringWithFormat:@"Changed Glympse destination to '%s' and message to '%s'",
                               destNameGStr->getBytes(), message->getBytes()]];
    }
}

#pragma mark - Glympse: View Helper Methods

#pragma mark -- Update UI in response to platform events
/**
 * Glympse: update ui to reflect current glympse ticket status ...
 *          + start a UI update timer if we have an active ticket
 */
- (void)updateGlympseTicketUi
{
    // If: we have an active glympse ticket going...
    if ([self isTicketActive:_glympseTicket])
    {
        // Disable config & send controls:
        self.sendGlympseBtn.enabled = NO;
        self.invitesCtrl.userInteractionEnabled = NO;
        self.messageCtrl.userInteractionEnabled = NO;
        self.configureCtrl.enabled = NO;

        // Unhide Active Ticket controls
        self.actionsCtrl.hidden = NO;
        self.watchedStatusCtrl.hidden = NO;
        self.durationStatusCtrl.hidden = NO;
        self.recipientsCtrl.hidden = NO;
        self.recipientsLbl.hidden = NO;
        
        // Launch a UI refresh timer: only if one isn't already started, and only if we have a valid active ticket.
        if (self.refreshTimer == nil)
        {
            self.refreshTimer = [NSTimer scheduledTimerWithTimeInterval:1.0
                                                                 target:self
                                                               selector:@selector(refreshDurationAndWatchers)
                                                               userInfo:nil
                                                                repeats:YES];
        }
        
        self.recipientsCtrl.text = [self getRecipientsAsString];
    }
    // Else If: we have a configurable glympse ticket (unsent)
    else if([self isTicketInConfig:_glympseTicket])
    {
        // Enable config & send controls:
        self.sendGlympseBtn.enabled = [GlympseWrapper instance].glympse->getHistoryManager()->isSynced();
        self.invitesCtrl.userInteractionEnabled = YES;
        self.messageCtrl.userInteractionEnabled = YES;
        self.configureCtrl.enabled = YES;

        // Hide Active Ticket controls
        self.actionsCtrl.hidden = YES;
        self.watchedStatusCtrl.hidden = YES;
        self.durationStatusCtrl.hidden = YES;
        self.recipientsCtrl.hidden = YES;
        self.recipientsLbl.hidden = YES;
        
        self.recipientsCtrl.text = [self getRecipientsAsString];
    }
    // Else: our ticket is in some transitional state, whether creating, sending, expiring, erroring, or NULL
    else
    {
        // Lock down the UI until ticket enters one of the previous two states
        
        // Disable config & send controls:
        self.sendGlympseBtn.enabled = NO;
        self.invitesCtrl.userInteractionEnabled = NO;
        self.messageCtrl.userInteractionEnabled = NO;
        self.configureCtrl.enabled = NO;
        
        self.actionsCtrl.hidden = YES;
        
        // Only hide "watched" and "duration" controls if we don't have a ticket object
        //  otherwise they will display the "expired" state and total, past "watched" count
        if (_glympseTicket == NULL)
        {
            self.watchedStatusCtrl.hidden = YES;
            self.durationStatusCtrl.hidden = YES;
            self.recipientsCtrl.hidden = YES;
            self.recipientsLbl.hidden = YES;
        }
    }
}

- (void)refreshDurationAndWatchers
{
    self.watchedStatusCtrl.text = [self getNumberWatchingAsString];
    self.durationStatusCtrl.text = [self getTimeRemainingAsString];
}

#pragma mark -- Derive status information from the active Glympse


- (NSString *)getRecipientsAsString
{
    if ([self isTicketActive:_glympseTicket])
    {
        NSMutableString *recipients = [[NSMutableString alloc] init];
        Glympse::GArray<Glympse::GInvite>::ptr invites = _glympseTicket->getInvites();
        for (Glympse::int32 i = 0; i < invites->length(); i++)
        {
            if (i > 0)
            {
                [recipients appendString:@", "];
            }
            
            Glympse::GInvite invite = invites->at(i);
            if (invite->getState() == Glympse::GC::INVITE_STATE_FAILED_TO_SEND)
            {
                [recipients appendString:@"[Send Failed: "];
            }
            if (invite->getType() == Glympse::GC::INVITE_TYPE_LINK && invite->getUrl() != NULL)
            {
                Glympse::GString mergedText = invite->getName()->append(": ")->append(invite->getUrl());
                
                [recipients appendString:[NSString stringWithUTF8String:mergedText->getBytes()]];
            }
            else if(invite->getAddress() != NULL)
            {
                if (invite->getName() != NULL)
                {
                    Glympse::GString mergedText = invite->getName()->append(": ")->append(invite->getAddress());
                    
                    [recipients appendString:[NSString stringWithUTF8String:mergedText->getBytes()]];
                }
                else
                {
                    [recipients appendString:[NSString stringWithUTF8String:invite->getAddress()->getBytes()]];
                }
            }
            else if (invite->getName() != NULL)
            {
                [recipients appendString:[NSString stringWithUTF8String:invite->getName()->getBytes()]];
            }
            else
            {
                [recipients appendFormat:@"Unnamed Invite (Type:%d)", invite->getType()];
            }
            if (invite->getState() == Glympse::GC::INVITE_STATE_FAILED_TO_SEND)
            {
                [recipients appendString:@"]"];
            }
        }
        if (invites->length() < 1)
        {
            [recipients appendString:@"(No Recipients)"];
            
        }
        return recipients;
    }
    else if([GlympseWrapper instance].glympse->getHistoryManager()->isSynced())
    {
        return @"No Active Glympse";
    }
    else
    {
        return @"Syncing with Glympse server...";
    }
}

- (NSString *)getNumberWatchingAsString
{
    if (_glympseTicket != NULL)
    {
        Glympse::int32 count = 0;
		Glympse::int64 currentTimeMs = [GlympseWrapper instance].glympse->getTime();
		Glympse::GArray<Glympse::GInvite>::ptr invites = _glympseTicket->getInvites();
        
        if (_glympseTicket->getExpireTime() > currentTimeMs)
        {
            for (Glympse::int32 i = 0; i < invites->length(); i++)
            {
                // If someone viewed it within the last 3 minutes, consider them actively "watching"
                if(currentTimeMs - invites->at(i)->getLastViewTime() < (3L * 60000))
                {
                    count++;
                }
            }
            return [NSString stringWithFormat:@"%d watching", count];
        }
        // else: ticket is expired, count total views.
        else
        {
            for (Glympse::int32 i = 0; i < invites->length(); i++)
            {
                count += invites->at(i)->getViewers();
            }
            return [NSString stringWithFormat:@"%d watched", count];
        }
    }
    else
    {
        return @"--";
    }
}

- (NSString *)getTimeRemainingAsString
{
    if (_glympseTicket != NULL)
    {
        Glympse::int64 currentTimeMs = [GlympseWrapper instance].glympse->getTime();
        Glympse::int64 expireTimeMs = _glympseTicket->getExpireTime();
        Glympse::int64 durationMs = expireTimeMs - currentTimeMs;
        if (durationMs <= 0)
        {
            return @"Expired";
        }
        else
        {
            return [self formatDuration:durationMs withPostfix:@" remaining"];
        }
    }
    else
    {
        return @"--";
    }
}

#pragma mark -- Misc. helper methods

- (NSString *)formatDuration:(Glympse::int64)durationMs withPostfix:(NSString *)postfix
{
    if (durationMs < 0)
    {
        durationMs = 0;
    }
    
    if (postfix == nil)
    {
        postfix = [NSString string];
    }
    
    static const int MS_PER_SECOND = 1000;
    static const int MS_PER_MINUTE = 60000;
    static const int MS_PER_HOUR   = 3600000;
    static const int MS_PER_DAY    = 86400000;
    
    int days    = (int)(durationMs / MS_PER_DAY   );
    int hours   = (int)(durationMs / MS_PER_HOUR  ) % 24;
    int minutes = (int)(durationMs / MS_PER_MINUTE) % 60;
    int seconds = (int)(durationMs / MS_PER_SECOND) % 60;
    
    // "10 days"
    if (days >= 10)
    {
        return [NSString stringWithFormat:@"%d days%@", days, postfix];
    }
    // "1 day, 2:33"
    else if (days > 0)
    {
        return [NSString stringWithFormat:@"%d day(s), %d hr(s), %d min%@", days, hours, minutes, postfix];
    }
    // "2:33:44"
    else if (hours > 0)
    {
        return [NSString stringWithFormat:@"%d hr(s), %d min, %d sec%@", hours, minutes, seconds, postfix];
    }
    // "33:44"
    return [NSString stringWithFormat:@"%d min, %d sec%@", minutes, seconds, postfix];
}

#pragma mark - GLYTimerVCDelegate methods

- (void)gTimerDone:(BOOL)wasCancelled withDuration:(NSTimeInterval)duration
{
    if (wasCancelled)
    {
        return;
    }
    // Convert seconds to milliseconds
    int durationMs = (int)(duration * 1000.0);
    
    // Update duration on ticket
    // NOTE: Passing NULL for other modify parameters signifies "do not change existing values"
    _glympseTicket->modify(durationMs, NULL, NULL);
    
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

#pragma mark - UIAlertViewDelegate method

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    [self clearGlympseTicket];
    [alertView autorelease];
}

#pragma mark - UITextViewDelegate methods

- (void) textViewDidBeginEditing:(UITextView *)textView
{
    if (textView == _invitesCtrl && [_invitesCtrl.text hasPrefix:@"email address"])
    {
        _invitesCtrl.text = @"";
        _invitesCtrl.textColor = [UIColor darkTextColor];
    }
}

- (void) textViewDidEndEditing:(UITextView *)textView
{
    if (textView == _invitesCtrl && [textView.text length] == 0)
    {
        _invitesCtrl.text = @"email addresses, phone numbers, groups";
        _invitesCtrl.textColor = [UIColor lightGrayColor];
    }
}

- (BOOL) textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text
{
    if([text isEqualToString:@"\n"])
    {
        [textView resignFirstResponder];
        return NO;
    }
    else
    {
        return YES;
    }
}

- (void)viewDidUnload {
    [self setRecipientsLbl:nil];
    [super viewDidUnload];
}
@end











