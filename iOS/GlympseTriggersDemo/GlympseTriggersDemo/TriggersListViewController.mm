//------------------------------------------------------------------------------
//
//  Copyright (c) 2014 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "TriggersListViewController.h"
#import "GlympseWrapperARC.h"
#import "TriggerTableViewCell.h"
#import "DemoTriggers.h"

@interface TriggersListViewController () <GLYEventListener>

@end

@implementation TriggersListViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self)
    {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
    UINib *nib = [UINib nibWithNibName:@"TriggerTableViewCell" bundle:nil];
    [self.tableViewTriggers registerNib:nib forCellReuseIdentifier:@"TriggerCellIdentifier"];
    
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse];
    [GLYGlympse subscribe:self onSink:[GlympseWrapper instance].glympse->getTriggersManager()];
    [DemoTriggers populate:[GlympseWrapper instance].glympse];
    [self refreshTriggers];
}

- (void)viewDidUnload
{
    [GLYGlympse unsubscribe:self onSink:[GlympseWrapper instance].glympse->getTriggersManager()];
    [self setTableViewTriggers:nil];
    [self setButtonExpireAll:nil];
    [super viewDidUnload];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)expireAllTickets
{
    Glympse::GArray<Glympse::GTicket>::ptr tickets = [GlympseWrapper instance].glympse->getHistoryManager()->getTickets();
    if ( 0 == tickets->length() )
    {
        return;
    }
    for ( Glympse::GTicket ticket : tickets )
    {
        if ( ticket->isActive() )
        {
            ticket->expire();
        }
    }
}

- (void)refreshTriggers
{
    [self.tableViewTriggers reloadData];
}

- (void)triggerActivated:(const Glympse::GTrigger&)trigger
{
    NSString *message = @"An unnamed trigger was activated.";
    if( NULL != trigger->getName() )
    {
        message = [NSString stringWithFormat:@"The trigger named %@ was activated.", [NSString stringWithUTF8String:trigger->getName()->getBytes()]];
    }
    
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle: @"Trigger Activated" message: message delegate: nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
    [alert show];
}

- (IBAction)buttonExpireAllPressed:(id)sender
{
    [self expireAllTickets];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return 196.0;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
	return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
	return [GlympseWrapper instance].glympse->getTriggersManager()->getLocalTriggers()->length();
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
	TriggerTableViewCell* cell = (TriggerTableViewCell*)[tableView dequeueReusableCellWithIdentifier:@"TriggerCellIdentifier"];
    
    Glympse::GTrigger trigger = [GlympseWrapper instance].glympse->getTriggersManager()->getLocalTriggers()->at((Glympse::int32)indexPath.row);
    [cell setTrigger:trigger];
    
    return cell;
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
    if(listener == Glympse::GE::LISTENER_PLATFORM)
    {
        if (0 != (events & Glympse::GE::PLATFORM_SYNCED_WITH_SERVER))
        {
            [self refreshTriggers];
        }
    }
    if(listener == Glympse::GE::LISTENER_TRIGGERS)
    {
        if (0 != (events & (Glympse::GE::TRIGGERS_TRIGGER_ADDED | Glympse::GE::TRIGGERS_TRIGGER_REMOVED)))
        {
            [self refreshTriggers];
        }
        else if (0 != (events & Glympse::GE::TRIGGERS_TRIGGER_ACTIVATED))
        {
            Glympse::GTrigger trigger = (Glympse::GTrigger)object;
            [self triggerActivated:trigger];
        }
    }
}
@end
