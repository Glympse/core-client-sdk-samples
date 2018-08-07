//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------


#import <Foundation/Foundation.h>

@protocol GlympseTicketActionsDelegate

- (void)expireGlympse:(Glympse::GTicket)glympseTicket;
- (void)plusFifteenMins:(Glympse::GTicket)glympseTicket;
- (void)modifyGlympse:(Glympse::GTicket)glympseTicket;
- (void)showGlympseTimer:(Glympse::GTicket)glympseTicket;

@end
