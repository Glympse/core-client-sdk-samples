//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "GlympseTicketTableCell.h"

#import "GlympseWrapperARC.h"

@interface GlympseTicketTableCell()
{
    IBOutlet UITextView *_recipientsCtrl;
    
    IBOutlet UITextField *_watchStatusCtrl;
    IBOutlet UITextField *_durationStatusCtrl;
    IBOutlet UITextField *_messageCtrl;
    IBOutlet UITextField *_destinationCtrl;
    IBOutlet UISegmentedControl *_actionsCtrl;
}

@end

@implementation GlympseTicketTableCell

@dynamic glympseTicket;

#pragma mark - Dynamic Property Implementation

- (void)setGlympseTicket:(Glympse::GTicket)glympseTicket
{
    if (_glympseTicket != NULL)
    {
        // Stop listening to old ticket object for events
        [GLYGlympse unsubscribe:self onSink:_glympseTicket];
    }
    
    // Assign to new object (Glympse::O<T> objs are smart pointers: they will release old ref. before assigning new one)
    _glympseTicket = glympseTicket;
    
    if (_glympseTicket != NULL)
    {
        // Listen to ticket object for events
        [GLYGlympse subscribe:self onSink:_glympseTicket];
    }
    
    [self enableTicketControls:[self hasActiveTicket]];
    [self refreshDuration];
    [self refreshWatchers];
    [self refreshRecipients];
    [self refreshMessage];
    [self refreshDestination];
}

- (Glympse::GTicket)glympseTicket
{
    return _glympseTicket;
}


#pragma mark - Control Event Handlers


- (IBAction)actionsCtrl_ValueChanged:(id)sender
{
    switch (_actionsCtrl.selectedSegmentIndex)
    {
        case 0:
            [_delegate expireGlympse:_glympseTicket];
            break;
        case 1:
            [_delegate plusFifteenMins:_glympseTicket];
            break;
        case 2:
            [_delegate modifyGlympse:_glympseTicket];
            break;
        case 3:
            [_delegate showGlympseTimer:_glympseTicket];
            break;
        default:
            break;
    }
}

#pragma mark - Glympse: Events (aka Notifications)

/**
 * Respond to Glympse platform events here
 * Glympse sends this message on the same thread you started it, so its safe to update the UI directly from here
 */
- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    /**
     * Tips for implementing GLYEventListener protocol handlers: 
     * - Do not unintentionally test the "events" param in "else if" statements: "events" can contain multiple flags
     * - Break "events" tests up by "listener" type: unlike "events", "listener" will only ever be ONE type per call
     * - Move larger pieces of logic into their own methods, outside of this method impl., to improve readability
     */
    
    if (_glympseTicket == NULL)
    {
        return; // This TableCell doesn't have a ticket object currently, so bail immediately.
    }

    // Ticket events:
    if (listener == Glympse::GE::LISTENER_TICKET)
    {
        // We don't handle TICKET_CREATED in the cell: the Table's DataSource handles this

        // Expired: disable UI, update duration & watchers text
        if (0 != (events & Glympse::GE::TICKET_EXPIRED))
        {
            // Manually update UI one last time to show Expired status
            [self enableTicketControls:NO];
            [self refreshDuration];
            [self refreshWatchers]; //put into past-tense
        }

        // Duration changed: update duration UI text immediately
        if (0 != (events & Glympse::GE::TICKET_DURATION_CHANGED))
        {
            [self refreshDuration];
        }
        
        // Message changed: update message UI text
        if (0 != (events & Glympse::GE::TICKET_MESSAGE_CHANGED))
        {
            [self refreshMessage];
        }
        
        // Destination changed: update destination UI text
        if (0 != (events & Glympse::GE::TICKET_DESTINATION_CHANGED))
        {
            [self refreshDestination];
        }
        
        // Glympse was viewed for first time: update watchers UI text
        if (0 != (events & Glympse::GE::TICKET_FIRST_VIEWED))
        {
            [self refreshWatchers];
        }

        // Invite "Updated" aka someone viewed/is-viewing your glympse: update watchers UI text
        if (0 != (events & Glympse::GE::TICKET_INVITE_UPDATED))
        {
            [self refreshWatchers];
        }
        //else: handle TICKET_INVITE_... CREATED, SENT, FAILED, REMOVED.
        else if (0 != (events & Glympse::GE::TICKET_INVITE_CHANGED))//TICKET_INVITE_CHANGED has all TICKET_INVITE_ flags
        {
            // Since this tablecell won't exist until after the ticket is created, handling this event will catch
            //  newly added invites from a "Modify" and failed invites.
            [self refreshRecipients];
        }
    }
}


#pragma mark - Glympse: View Helper Methods


#pragma mark -- Update UI in response to platform events

/**
 * Update enabled/visual state of various UI controls to reflect current glympse ticket status
 */
- (void)enableTicketControls:(BOOL)hasActiveTicket
{
    if (hasActiveTicket)
    {
        _actionsCtrl.hidden = NO;
        
        _watchStatusCtrl.backgroundColor = [UIColor whiteColor];
        _durationStatusCtrl.backgroundColor = [UIColor whiteColor];
        _recipientsCtrl.backgroundColor = [UIColor whiteColor];
    }
    else
    {
        _actionsCtrl.hidden = YES;
        
        _watchStatusCtrl.backgroundColor = [UIColor lightGrayColor];
        _durationStatusCtrl.backgroundColor = [UIColor lightGrayColor];
        _recipientsCtrl.backgroundColor = [UIColor lightGrayColor];
    }
}

- (void)refreshWatchers
{
    _watchStatusCtrl.text = [self getNumberWatchingAsString];
}

- (void)refreshDuration
{
    
    _durationStatusCtrl.text = [self getTimeRemainingAsString];
}

- (void)refreshRecipients
{
    _recipientsCtrl.text = [self getRecipientsAsString];
}

- (void)refreshMessage
{
    if (_glympseTicket != NULL && _glympseTicket->getMessage() != NULL)
    {
        _messageCtrl.text = [NSString stringWithFormat:@"Msg: %s", _glympseTicket->getMessage()->toCharArray()];
    }
    else
    {
        _messageCtrl.text = @"(No Message)";
    }
}

- (void)refreshDestination
{
    _destinationCtrl.text = [self getDestinationAsString];
}

#pragma mark -- Derive status information from the active Glympse

- (BOOL)hasActiveTicket
{
    // Check for valid ticket object AND whether it has expired
    return _glympseTicket != NULL && _glympseTicket->getExpireTime() > [GlympseWrapper instance].glympse->getTime();
}

- (NSString *)getRecipientsAsString
{
    if (_glympseTicket != NULL)
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
                
                [recipients appendString:[NSString stringWithUTF8String:mergedText->toCharArray()]];
            }
            else if(invite->getAddress() != NULL)
            {
                if (invite->getName() != NULL)
                {
                    Glympse::GString mergedText = invite->getName()->append(": ")->append(invite->getAddress());
                    
                    [recipients appendString:[NSString stringWithUTF8String:mergedText->toCharArray()]];
                }
                else
                {
                    [recipients appendString:[NSString stringWithUTF8String:invite->getAddress()->toCharArray()]];
                }
            }
            else if (invite->getName() != NULL)
            {
                [recipients appendString:[NSString stringWithUTF8String:invite->getName()->toCharArray()]];
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
    else
    {
        return @"--";
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
            return [self formatDuration:llabs(durationMs) withPrefix:@"Expired " withPostfix:@" ago"];
        }
        else
        {
            return [self formatDuration:durationMs withPrefix:@"" withPostfix:@" remaining"];
        }
    }
    else
    {
        return @"--";
    }
}

- (NSString *)getDestinationAsString
{
    if (_glympseTicket != NULL &&
        _glympseTicket->getDestination() != NULL &&      // destination is optional
        _glympseTicket->getDestination()->hasLocation()) // checks for valid lat+long values
    {
        Glympse::GPlace destination = _glympseTicket->getDestination();

        if (destination->getName() != NULL)
        {
            return [NSString stringWithFormat:@"Dest: %s", destination->getName()->toCharArray()];
        }
        else
        {
            return [NSString stringWithFormat:@"Dest: %3.5f,%3.5f",
                    destination->getLatitude(), destination->getLongitude()];
        }
    }
    else
    {
        return @"(No Destination)";
    }
}

#pragma mark -- Misc. helper methods

- (NSString *)formatDuration:(Glympse::int64)durationMs withPrefix:(NSString *)prefix withPostfix:(NSString *)postfix
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
        return [NSString stringWithFormat:@"%@%d days%@", prefix, days, postfix];
    }
    // "1 day, 2:33"
    else if (days > 0)
    {
        return [NSString stringWithFormat:@"%@%d day(s), %d hr(s)%@", prefix, days, hours, postfix];
    }
    // "2:33:44"
    else if (hours > 0)
    {
        return [NSString stringWithFormat:@"%@%d hr(s), %d min%@", prefix, hours, minutes, postfix];
    }
    // "33:44"
    return [NSString stringWithFormat:@"%@%d min, %d sec%@", prefix, minutes, seconds, postfix];
}

#pragma mark - View Lifecycle

- (void)dealloc
{
    if (_glympseTicket != NULL)
    {
        // Stop listening to old ticket object for events
        [GLYGlympse unsubscribe:self onSink:_glympseTicket];
        
        // Release ticket object ref. by setting to NULL
        _glympseTicket = NULL;
    }
    [_recipientsCtrl release];
    [_watchStatusCtrl release];
    [_durationStatusCtrl release];
    [_actionsCtrl release];
    [_destinationCtrl release];
    [_messageCtrl release];
    [super dealloc];
}


@end
