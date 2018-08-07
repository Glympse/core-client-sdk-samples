//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "GlympseTicketActionsDelegate.h"

@interface GlympseTicketTableCell : UITableViewCell<GLYEventListener>
{
    Glympse::GTicket _glympseTicket;
}

@property (assign, nonatomic) Glympse::GTicket glympseTicket; 

- (IBAction)actionsCtrl_ValueChanged:(id)sender;

- (void)refreshDuration;

@property (assign, nonatomic) id<GlympseTicketActionsDelegate> delegate;

@end
