//------------------------------------------------------------------------------
//
//  Copyright (c) 2014 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "DemoTriggers.h"

@implementation DemoTriggers

static Glympse::GPlace HOME_LOCATION = Glympse::GlympseFactory::createPlace(47.622245445765564, -122.33471397310495, Glympse::CoreFactory::createString("Home"));
static double HOME_RADIUS = 100;

static NSString *CONTACT_ADDRESS_1 = @"<< Enter email or phone number here >>";
static NSString *CONTACT_ADDRESS_2 = @"<< Enter email or phone number here >>";


+ (void)populate:(Glympse::GGlympse)glympse
{
    Glympse::GTriggersManager triggersManager = glympse->getTriggersManager();
    if ( triggersManager->getLocalTriggers()->length() > 0 )
    {
        return;
    }
    
    Glympse::GTicket ticket = Glympse::GlympseFactory::createTicket(3600000, Glympse::CoreFactory::createString("Leaving home..."), NULL);
    ticket->addInvite(Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_UNKNOWN, NULL, Glympse::CoreFactory::createString([CONTACT_ADDRESS_1 UTF8String])));
    ticket->addInvite(Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_UNKNOWN, NULL, Glympse::CoreFactory::createString([CONTACT_ADDRESS_2 UTF8String])));
    
    Glympse::GTrigger trigger = Glympse::GlympseFactory::createGeoTrigger(Glympse::CoreFactory::createString("Leaving Home"), true, ticket, HOME_LOCATION, HOME_RADIUS, Glympse::CC::GEOFENCE_TRANSITION_EXIT);
    
    triggersManager->addLocalTrigger(trigger);
}

@end
