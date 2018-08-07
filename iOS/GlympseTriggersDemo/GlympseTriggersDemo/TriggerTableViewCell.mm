//------------------------------------------------------------------------------
//
//  Copyright (c) 2014 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "TriggerTableViewCell.h"

@implementation TriggerTableViewCell

- (void)awakeFromNib
{
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

- (void)setTrigger:(const Glympse::GTrigger&)trigger
{
    self.labelName.text = [self nameStringForTrigger:trigger];
    self.lableType.text = [self typeStringForTrigger:trigger];
    self.labelAutoSend.text = [self autoSendStringForTrigger:trigger];
    self.labelRegion.text = [self regionStringForTrigger:trigger];
    self.labelMessage.text = [self messageStringForTrigger:trigger];
    self.labelDestination.text = [self destinationStringForTrigger:trigger];
    self.labelRecipients.text = [self recipientsStringForTrigger:trigger];
}

- (NSString *)nameStringForTrigger:(const Glympse::GTrigger&)trigger
{
    Glympse::GString name = trigger->getName();
    if( NULL == name )
    {
        return @"< No Name >";
    }
    
    return [NSString stringWithUTF8String:trigger->getName()->getBytes()];
}

- (NSString *)typeStringForTrigger:(const Glympse::GTrigger&)trigger
{
    switch ( trigger->getType() )
    {
        case Glympse::GlympseConstants::TRIGGER_TYPE_GEO:
        {
            return @"Type: Geo";
            break;
        }
        default:
        {
            return @"Type: Unknown";
            break;
        }
    }
}

- (NSString *)autoSendStringForTrigger:(const Glympse::GTrigger&)trigger
{
    if ( trigger->autoSend() )
    {
        return @"Auto send: Yes";
    }
    
    return @"Auto send: No";
}

- (NSString *)regionStringForTrigger:(const Glympse::GTrigger&)trigger
{
    Glympse::GGeoTrigger geoTrigger = (Glympse::GGeoTrigger)trigger;
    if( NULL != geoTrigger )
    {
        Glympse::GRegion region = geoTrigger->getRegion();
        if ( NULL == region )
        {
            return @"< No Region >";
        }
        else
        {
            return [NSString stringWithFormat:@"(%0.4f, %0.4f) - %0.1fm", region->getLatitude(), region->getLongitude(), region->getRadius()];
        }
    }
    
    return @"";
}

- (NSString *)messageStringForTrigger:(const Glympse::GTrigger&)trigger
{
    Glympse::GTicket ticket = trigger->getTicket();
    if( NULL == ticket )
    {
        return @"< No Ticket Found >";
    }
        
    Glympse::GString message = ticket->getMessage();
    if (NULL == message)
    {
        return @"< No Message >";
    }
    
    return [NSString stringWithUTF8String:message->getBytes()];
}

- (NSString *)destinationStringForTrigger:(const Glympse::GTrigger&)trigger
{
    Glympse::GTicket ticket = trigger->getTicket();
    if( NULL == ticket )
    {
        return @"";
    }
    
    Glympse::GPlace destination = ticket->getDestination();
    if( NULL == destination )
    {
        return @"< No Destination >";
    }
    
    NSString *description = @"";
    Glympse::GString name = destination->getName();
    if( NULL != name )
    {
        description = [NSString stringWithUTF8String:name->getBytes()];
    }
    
    return [NSString stringWithFormat:@"%@ : (%0.4f, %0.4f)", description, destination->getLatitude(), destination->getLongitude()];
}

- (NSString *)recipientsStringForTrigger:(const Glympse::GTrigger&)trigger
{
    Glympse::GTicket ticket = trigger->getTicket();
    if( NULL == ticket )
    {
        return @"";
    }
    
    NSString *result = @"";
    for (Glympse::GInvite invite : ticket->getInvites() )
    {
        Glympse::GString address = invite->getAddress();
        if( NULL == address || address->length() == 0 )
        {
            address = Glympse::GlympseTools::inviteTypeEnumToString(invite->getType());
        }
        result = [NSString stringWithFormat:@"%@%@", result, [[NSString stringWithUTF8String:address->getBytes()] stringByAppendingString:@", "]];
    }
    
    return result;
}

@end
