//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "HistoryMainViewController.h"

#import "GLYTimerVC.h"
#import "GLYSMSJob.h"
#import "GlympseWrapperARC.h"
#import "GlympseTicketTableCell.h"

@interface HistoryMainViewController ()
<GLYEventListener, GLYTimerVCDelegate, GLYSMSJobDelegate, GlympseTicketActionsDelegate>
{
    Glympse::GTicket _pendingGlympseTicket;
    Glympse::GTicket _timerTicket; //ticket obj being modified by the GLYTimerControl: could be active, could be unsent
    NSMutableArray * _wrappedTickets;
}
@end

@implementation HistoryMainViewController

#pragma mark - Object Lifecycle

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self)
    {
        _wrappedTickets = [[NSMutableArray alloc] init];
    }
    return self;
}

- (void)dealloc
{
    [self enableObservingGlympsePlatformEvents:NO];
    
    _pendingGlympseTicket = NULL;
    _timerTicket = NULL;
    
    if (self.refreshTimer != nil)
    {
        [self.refreshTimer invalidate];
        self.refreshTimer = nil;
    }
    [_wrappedTickets release];
    [_sendGlympseBtn release];
    [_invitesCtrl release];
    [_messageCtrl release];
    [_configureCtrl release];
    [_glympseTableView release];
    [super dealloc];
}

#pragma mark - View Lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self enableObservingGlympsePlatformEvents:YES];
    
}

#pragma mark - View/Control Event Handlers

- (IBAction)sendGlympseBtn_TouchUpInside:(id)sender
{
    [self hideKeyboard];
    
    _sendGlympseBtn.enabled = NO; //prevent mistaken re-send during send process
    
    BOOL succeeded = [self sendGlympseTicket];

    _sendGlympseBtn.enabled = !succeeded; //re-enable if send failed
}

- (IBAction)configureCtrl_ValueChanged:(id)sender
{
    [self hideKeyboard];
    
    switch (_configureCtrl.selectedSegmentIndex)
    {
        case 0:
            [self showGlympseTimer:_pendingGlympseTicket];
            break;
        case 1:
            if ([[_configureCtrl titleForSegmentAtIndex:1] hasPrefix:@"+"])
            {
                [self addHardcodedInvites:_pendingGlympseTicket];
                [_configureCtrl setTitle:@"- Invites Were Added -" forSegmentAtIndex:1];
            }
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
    if (listener == Glympse::GE::LISTENER_TICKET)
    {
        if (0 != (events & Glympse::GE::TICKET_CREATED))
        {
            // If a ticket is added, rebuild the table to use Glympse platform's internal re-ordering of tickets
            [self rebuildGlympseTable];

            [self queueNewGlympseTicket];
            
            [self updateMainGlympseUi];
        }
        else if (0 != (events & Glympse::GE::TICKET_INVITE_CREATED))
        {
            // This event only happens for invites that need to be delivered by the client (e.g. GC::INVITE_TYPE_SMS)
            [self processUnsentInvites:object];
        }
        else if (0 != (events & Glympse::GE::TICKET_EXPIRED))
        {
            // If a ticket is expired, stop listening to old ticket object...
            [GLYGlympse unsubscribe:self onSink:object];

            // And rebuild the table to use Glympse platform's internal re-ordering of tickets
            [self rebuildGlympseTable];
        }
        else if (0 != (events & Glympse::GE::TICKET_FAILED))
        {
            if (_pendingGlympseTicket != NULL && _pendingGlympseTicket->equals(object))
            {
                if (0 != (_pendingGlympseTicket->getState() & Glympse::GC::TICKET_STATE_FAILED_TO_CREATE))
                {
                    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"Clearing Ticket"
                                                                        message:@"Ticket creation failed."
                                                                       delegate:self
                                                              cancelButtonTitle:nil
                                                              otherButtonTitles:@"OK", nil];
                    [alertView show];
                    [self queueNewGlympseTicket];
                }
            }
        }
        else
        {
            // Log intentionally unhandled events just to give a better picture of ticket event activity
            NSLog(@"Unhandled Ticket Event: %4x", events);
        }
        
    }
    else if(listener == Glympse::GE::LISTENER_PLATFORM)
    {
        if (0 != (events & Glympse::GE::PLATFORM_SYNCED_WITH_SERVER))
        {
            [self queueNewGlympseTicket];
            
            // Synced means the list of active tickets, and potentially history, is now available.
            [self rebuildGlympseTable];
            
            [self updateMainGlympseUi];
        }
        else if (0 != (events & Glympse::GE::PLATFORM_TICKET_REMOVED))
        {
            Glympse::GTicket glympseTicket = (Glympse::GTicket)object;
            
            [self removeTicketFromTable:glympseTicket];
            
            [self updateMainGlympseUi];
        }
    }
}

#pragma mark - Glympse: Ticket and Platform Methods
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
            [self queueNewGlympseTicket];
            
            [self rebuildGlympseTable];
            
            [self updateMainGlympseUi];
        }
        
        [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    }
    else
    {
        [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse];
    }
}

/**
 * Convenience method for determining whether a ticket hasn't been sent, and thus is still being configured
 */
- (BOOL)isTicketInConfig:(Glympse::GTicket)aTicket

{
    return ( aTicket != NULL && aTicket->getState() == Glympse::GC::TICKET_STATE_NONE );
}

/**
 * Convenience method for determining whether a ticket is "active" -- aka was sent to server and is either actively  
 *  being shared, or at least about to be shared because we just sent it.
 */
- (BOOL)isTicketActive:(Glympse::GTicket)aTicket
{
    return ( aTicket != NULL && aTicket->isActive() );
}


/**
 * Handles sending of invites that couldn't be sent by server -- e.g. SMS on iPhone, or invites with ambiguous 
 *  recipients such as INVITE_TYPE_CLIPBOARD where the user can paste at-will, or INVITE_TYPE_LINK for use in
 *  custom/3rd-party programmatic invite sharing scenarios.
 * @param glympseTicket ticket object to process unsent invites for.
 */
- (void)processUnsentInvites:(Glympse::GTicket)glympseTicket
{
    Glympse::GArray<Glympse::GInvite>::ptr invites = glympseTicket->getInvites();
    int count = invites->length();
    for ( int index = 0 ; index < count ; ++index )
    {
        Glympse::GInvite invite = invites->at(index);
        if (Glympse::GC::INVITE_STATE_NEEDTOSEND == invite->getState() )
        {
            if(invite->getType()==Glympse::GC::INVITE_TYPE_SMS)
            {
                [GLYSMSJob schedule:invite forTicket:glympseTicket asRequest:NO delegate:self];
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

/**
 * Setup a new ticket object and UI with default Demo values
 */
- (void)queueNewGlympseTicket

{
    // Release old ticket ref. by setting to NULL
    _pendingGlympseTicket = NULL;
    
    int startingDurationMs = 1000 * 60 * 5; // for this demo we pre-set duration of new ticket to 5 minutes
    // Create new empty ticket object for configuration
    _pendingGlympseTicket = Glympse::GlympseFactory::createTicket(startingDurationMs, NULL, NULL);
    assert([self isTicketInConfig:_pendingGlympseTicket]);
    
    // Listen to new ticket object for events
    [GLYGlympse subscribe:self onSink:_pendingGlympseTicket];
    
    // Clear out previous ticket config states from UI
    _messageCtrl.text = [NSString stringWithFormat:@"Hello, from History Demo in %@ mode!", ([GLYGlympse is64bit]?@"64-bit":@"32-bit")];
    _invitesCtrl.text = @"email addresses, phone numbers, groups";
    _invitesCtrl.textColor = [UIColor lightGrayColor];
    [_configureCtrl setTitle:@"+ Hardcoded Invites" forSegmentAtIndex:1];
    _sendGlympseBtn.enabled = YES;
}

/**
 * Begin location sharing and send invites
 */
- (BOOL)sendGlympseTicket
{
    // if we dont have a valid, unsent ticket: bail
    if ( ![self isTicketInConfig:_pendingGlympseTicket] )
    {
        return NO;
    }
    
    // Set invites: parse invite control
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
            else
            {
                added = _pendingGlympseTicket->addInvite(invite);
                if (!added)
                {
                    NSLog(@"Unable to add invite: an invite with matching address probably already exists in the ticket.");
                }
            }
        }
    }
    
    if (_pendingGlympseTicket->getInvites()->length() <= 0)
    {
        NSLog(@"Ticket send aborted: Had no invites!");
        [self showStatusAlert:@"Ticket Send Aborted" withMessage:@"Ticket had no invites!"];
        return NO;
    }
    
    // Set message: pull from message control
    Glympse::GString message = Glympse::CoreFactory::createString([self.messageCtrl.text UTF8String]);
    //Specify GC::DURATION_NO_CHANGE to leave original duration value, and NULL to leave destination alone
    _pendingGlympseTicket->modify(Glympse::GC::DURATION_NO_CHANGE, message, NULL);
    
    // Send ticket to server: this starts your glympse! Server will create codes for and deliver what invites it can.
    bool succeeded = [GlympseWrapper instance].glympse->sendTicket(_pendingGlympseTicket);
    if (!succeeded)
    {
        NSLog(@"Ticket send failed. Possible causes: Platform not started, NULL ticket passed, \
              ticket already sent, or ticket mid-send.");
        assert(false);
    }
    
    return succeeded;
}

#pragma mark -- Modify Unsent Ticket

/**
 * Note: While this method is only used for unsent tickets in this Demo, it is completely acceptable to add new invites
 * even AFTER a glympse ticket is sent, using the exact same "addInvite(...)" method on an active ticket.
 */
-(void)addHardcodedInvites:(Glympse::GTicket)ticket
{
    if (![self isTicketInConfig:ticket])
    {
        return; // For this Demo, this method is used for pre-send addition of invites only.
    }
    
    Glympse::GString recipientName = NULL;
    Glympse::GString recipientAddress = NULL;
    Glympse::GInvite invite = NULL;
    bool succeeded = false;

    recipientName = Glympse::CoreFactory::createString("LINK");
    invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_LINK, recipientName, NULL);
    
    succeeded = ticket->addInvite(invite);
    if (!succeeded)
    {
        NSLog(@"LINK Invite creation failed.");
    }

    recipientName = Glympse::CoreFactory::createString("CLIPBOARD");
    invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_CLIPBOARD, recipientName, NULL);
    
    succeeded = ticket->addInvite(invite);
    if (!succeeded)
    {
        NSLog(@"CLIPBOARD Invite creation failed.");
    }
    
    recipientAddress = Glympse::CoreFactory::createString("!test");
    invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_GROUP, NULL, recipientAddress);
    
    succeeded = ticket->addInvite(invite);
    if (!succeeded)
    {
        NSLog(@"GROUP Invite creation failed.");
    }
    [self showStatusAlert:@"Added Hardcoded invites"
              withMessage:@"Added LINK, CLIPBOARD (try pasting the link into safari), and GROUP (!test) invites"];
}
#pragma mark - GlympseTicketActionsDelegate protocol impl.
#pragma mark -- Modify Sent or Unsent Tickets

/**
 * Display "G-Timer" control to user for easy method of setting/changing duration of glympse ticket
 * @param ticket ticket object to alter the duration of
 */
- (void)showGlympseTimer:(Glympse::GTicket)ticket

{
    if (ticket == NULL || _timerTicket != NULL)
    {
        return;
    }
    
    _timerTicket = ticket;
    
    GLYTimerVC *timerVC = [[GLYTimerVC alloc] initWithNibName:@"GLYTimerVC" bundle:nil];
    timerVC.delegate = self;
    
    //For active tickets, get time remaining rather than starting duration
    if ([self isTicketActive:_timerTicket])
    {
        Glympse::int64 currentTimeMs = [GlympseWrapper instance].glympse->getTime();
        Glympse::int64 expireTimeMs = _timerTicket->getExpireTime();
        Glympse::int64 durationMs = expireTimeMs - currentTimeMs;
        timerVC.duration = (NSTimeInterval)durationMs/1000.0;
        timerVC.realTimeCountDown = YES;
    }
    else
    {
        timerVC.duration = (NSTimeInterval)_timerTicket->getDuration()/1000.0; //convert from millisec to seconds
        timerVC.realTimeCountDown = NO;
    }
    
    [self presentViewController:timerVC animated:YES completion:NULL];
}

- (void)expireGlympse:(Glympse::GTicket)activeTicket
{
    if ([self isTicketActive:activeTicket])
    {
        activeTicket->modify(0, NULL, NULL);
    }
}

- (void)plusFifteenMins:(Glympse::GTicket)activeTicket
{
    if ([self isTicketActive:activeTicket])
    {
        // Calculate remaining time.
        int remainingMs = 0;
        if ( activeTicket->getId() == NULL )
        {
            // Ticket creation is already scheduled but request has not been received from server yet.
            // We already have Ticket::_expireTime populated (even though it is not yet synced with server).
            // In this case network lags can affect the accuracy of calculations below.
            Glympse::int64 currentTimeMs = [GlympseWrapper instance].glympse->getTime();
            remainingMs = (int)( activeTicket->getExpireTime() - currentTimeMs );
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
            
            remainingMs = activeTicket->getDuration();
        }
        
        // Add 15 minutes to current remaining time.
        remainingMs += 900000;
        
        // Modify the ticket. Ticket object will check whether platform is still running
        activeTicket->modify(remainingMs, NULL, NULL);
    }
}

#pragma mark -- Modify Sent Tickets
/**
 * Note: While this method is only used for sent tickets in this Demo, it is completely acceptable to add a destination
 * before a glympse ticket is sent, using the exact same "modify(...)" method on an unsent ticket.
 */
- (void)modifyGlympse:(Glympse::GTicket)activeTicket
{
    if ([self isTicketActive:activeTicket])
    {
        Glympse::GPlace destination = activeTicket->getDestination();
        Glympse::GString message = NULL;
        Glympse::GString destNameGStr = NULL;
        if (destination == NULL || destination->getName()->equals("Eiffel Tower"))
        {
            destNameGStr = Glympse::CoreFactory::createString("Seattle Space Needle");
            destination = Glympse::GlympseFactory::createPlace(47.620560, -122.349457, destNameGStr);
            message = Glympse::CoreFactory::createString("Hello, from History Demo! I'm headed to Seattle.");
        }
        else
        {
            destNameGStr = Glympse::CoreFactory::createString("Eiffel Tower");
            destination = Glympse::GlympseFactory::createPlace(48.858532, 2.294748, destNameGStr);
            message = Glympse::CoreFactory::createString("Hello, from History Demo! I'm headed to Paris.");
        }
        
        // Apply changed message and destination. Pass GC::DURATION_NO_CHANGE to leave duration unchanged.
        activeTicket->modify(Glympse::GC::DURATION_NO_CHANGE, message, destination);
        
        [self showStatusAlert:@"Modified Glympse"
                  withMessage:[NSString stringWithFormat:@"Changed Glympse destination to '%s' and message to '%s'",
                               destNameGStr->getBytes(), message->getBytes()]];
    }
}

#pragma mark - GLYTimerVCDelegate protocol impl.

- (void)gTimerDone:(BOOL)wasCancelled withDuration:(NSTimeInterval)duration
{
    if (wasCancelled || _timerTicket == NULL)
    {
        _timerTicket = NULL;
        return;
    }
    
    // Convert seconds to milliseconds
    int durationMs = (int)(duration * 1000.0);
    
    // Update duration on ticket
    // NOTE: Passing NULL for other modify parameters signifies "do not change existing values"
    _timerTicket->modify(durationMs, NULL, NULL);
    
    // Release ref. so we allow other tickets to be passed in
    _timerTicket = NULL;
}

#pragma mark - Glympse: View Helper Methods

#pragma mark -- Update UI in response to platform events

- (void)updateMainGlympseUi
{
    _sendGlympseBtn.enabled = [GlympseWrapper instance].glympse->getHistoryManager()->isSynced();
    
    // Launch a duration refresh timer: only if one isn't already started, and only if we have a valid ticket.
    if (self.refreshTimer == nil && _wrappedTickets.count > 0)
    {
        self.refreshTimer = [NSTimer scheduledTimerWithTimeInterval:1.0
                                                             target:self
                                                           selector:@selector(refreshAllDurations)
                                                           userInfo:nil
                                                            repeats:YES];
    }
    // else: stop timer if it exists and we have NO tickets
    else if(self.refreshTimer != nil && _wrappedTickets.count == 0)
    {
        [self.refreshTimer invalidate];
        self.refreshTimer = nil;
    }
}

- (void)rebuildGlympseTable
{
    [_wrappedTickets removeAllObjects];
    
    Glympse::GArray<Glympse::GTicket>::ptr tickets = [GlympseWrapper instance].glympse->getHistoryManager()->getTickets();
    
    for (int i = 0; i < tickets->length(); i++)
    {
        Glympse::GTicket glympseTicket = tickets->at(i);
        id<GCommonWrapper> wrappedTicket = [GLYGlympse wrapGCommon:glympseTicket];
        [_wrappedTickets addObject:wrappedTicket];
    }
    [self.glympseTableView reloadData];
}

- (void)removeTicketFromTable:(Glympse::GTicket)glympseTicket
{
    for (int i = 0; i < _wrappedTickets.count; i++)
    {
        if ([_wrappedTickets[i] unwrap] == glympseTicket)
        {
            [_wrappedTickets removeObjectAtIndex:i];
            [self.glympseTableView reloadData];
            break;
        }
    }
}

#pragma mark -- Update UI in response to timer events

/**
 * Updates the time-remaining or time-since-expire on all visible ticket cells
 */
- (void)refreshAllDurations

{
    NSArray *visibleCells = [[self.glympseTableView visibleCells] retain];
    for (UITableViewCell *cell in visibleCells)
    {
        if ([cell isKindOfClass:[GlympseTicketTableCell class]])
        {
            GlympseTicketTableCell *ticketCell = (GlympseTicketTableCell *)cell;
            [ticketCell refreshDuration];
        }
    }
    [visibleCells release];
}

#pragma mark - UITableViewDataSource protocol impl.

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return _wrappedTickets.count == 0 ? 1 : _wrappedTickets.count;
}

// Row display. Implementers should *always* try to reuse cells by setting each cell's reuseIdentifier and querying for available reusable cells with dequeueReusableCellWithIdentifier:
// Cell gets various attributes set automatically based on table (separators) and data source (accessory views, editing controls)

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.row >= _wrappedTickets.count)
    {
        if (_wrappedTickets.count == 0)
        {
            UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"NoTicketsCell"];
            if (cell == nil)
            {
                cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"NoTicketsCell"] autorelease];
            }
            if ([GlympseWrapper instance].glympse->getHistoryManager()->isSynced())
            {
                cell.textLabel.text = @"No History - Tap Send!";
            }
            else
            {
                cell.textLabel.text = @"Syncing with Glympse Server...";
            }
            
            return cell;
        }
        return nil;
    }
    
    GlympseTicketTableCell *cell = [tableView dequeueReusableCellWithIdentifier:@"TicketCell"];
    if (cell == nil)
    {
        NSArray * nibViews = [[NSBundle mainBundle] loadNibNamed:@"GlympseTicketTableCell" owner:self options:nil];
        for (id currentObject in nibViews)
        {
            if ([currentObject isKindOfClass:[GlympseTicketTableCell class]])
            {
                cell = (GlympseTicketTableCell *)currentObject;
                cell.delegate = self;
                break;
            }
        }
    }
    cell.glympseTicket = [_wrappedTickets[indexPath.row] unwrap];
    return cell;
}

#pragma mark - UITableViewDelegate protocol impl.

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.row < _wrappedTickets.count)
    {
        if ([self isTicketActive:[_wrappedTickets[indexPath.row] unwrap]])
        {
            return 170.0f;
        }
        else
        {
            return 135.0f;
        }
    }
    else
    {
        return 60.0f;
    }
}


#pragma mark - GLYSMSJobDelegate protocol impl.

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

#pragma mark - UIAlertViewDelegate protocol impl.

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    [alertView autorelease];
}

#pragma mark - UITextViewDelegate protocol impl.

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

@end











