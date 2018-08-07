//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "CreateMainViewController.h"

#import "GLYTimerVC.h"

#import "GlympseWrapperARC.h"

@interface CreateMainViewController () <GLYEventListener, GLYTimerVCDelegate>

@end


@implementation CreateMainViewController

#pragma mark - Object Lifecycle

- (void)dealloc
{
    [self enableObservingGlympsePlatformEvents:NO];
    
    [_btnCreateGlympse release];
    [_tvGlympseUrl release];
    [super dealloc];
}

#pragma mark - View Lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self enableObservingGlympsePlatformEvents:YES];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
    [self showConsentDialogIfNeeded];
}

#pragma mark - View Event Handlers

- (IBAction)btnCreateGlympse_TouchUpInside:(id)sender
{
    [self createGlympse];
}

#pragma mark - View Helper Methods

- (void)showGlympseUrl:(NSString *)url
{
    self.tvGlympseUrl.text = url;
}

#pragma mark - Glympse Helper Methods

- (void)showConsentDialogIfNeeded
{
    if ( [GlympseWrapper instance].glympse->getConsentManager()->hasConsent() )
    {
        return;
    }
    
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:nil message:@"Glympse, Inc. collects your location data and other personal information you provide to Glympse, such as your name, email address and phone number, to enable you to share your location with others. Glympse disassociates (separates) your location data from your other personal data 48 hours after you share your location with others. Glympse then uses the disassociated location data in aggregate form to improve its services. Glympse shares your location information with third parties chosen by you and third-party technology providers that help power the Glympse solution." preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"Decline" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
        if ( [GlympseWrapper instance].glympse->hasUserAccount() )
        {
            // Glympse can continue to run if an account already exists but functionality
            // will be limited unless they grant consent.
            [GlympseWrapper instance].glympse->getConsentManager()->revokeConsent(0);
        }
        else
        {
            // If the user has no user account, denying consent means that an account
            // should not be created for them. Restart Glympse and ask for consent next
            // time the user wants to use Glympse features.
            [[GlympseWrapper instance] stop];
        }
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"I agree" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [GlympseWrapper instance].glympse->getConsentManager()->grantConsent(Glympse::GC::CONSENT_TYPE_SUBJECT(), 0);
    }]];
    [self presentViewController:alert animated:true completion:nil];
}

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

- (void)createGlympse
{
    [self showGlympseUrl:@"Creating Glympse..."];
    
    GLYTimerVC *timerVC = [[GLYTimerVC alloc] initWithNibName:@"GLYTimerVC" bundle:nil];
    timerVC.delegate = self;
    
    [self presentViewController:timerVC animated:YES completion:NULL];
    [timerVC release];
}

- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    if (0 != (listener & Glympse::GE::LISTENER_TICKET))
    {
        if (0 != (events & Glympse::GE::TICKET_INVITE_CREATED))
        {

            Glympse::GTicket ticket = (Glympse::GTicket)object;
            Glympse::GInvite invite = ticket->findInviteByType(Glympse::GC::INVITE_TYPE_LINK);
            if (invite != NULL && invite->getState() == Glympse::GC::INVITE_STATE_NEEDTOSEND)
            {
                // Update invite state to GC::INVITE_STATE_SUCCEEDED.
                invite->completeClientSideSend(true);
                
                NSString *inviteUrl = [NSString stringWithUTF8String:invite->getUrl()->getBytes()];
                [self showGlympseUrl:inviteUrl];
            }
        }
    }
}

- (void)gTimerDone:(BOOL)wasCancelled withDuration:(NSTimeInterval)duration
{
    if (wasCancelled)
    {
        [self showGlympseUrl:@"Create Glympse Cancelled."];
        return;
    }
    
    [self showGlympseUrl:@"Creating Glympse..."];
    NSLog(@"Glympse of duration: %f", duration);
    
    // Convert seconds to milliseconds
    int durationMs = (int)(duration * 1000.0);
    Glympse::GTicket ticket =
        Glympse::GlympseFactory::createTicket(durationMs, Glympse::CoreFactory::createString("Hello, world!"), NULL);
    
    // Listen to ticket object for events
    [GLYGlympse subscribe:self onSink:ticket];
    
    Glympse::GInvite invite = Glympse::GlympseFactory::createInvite(Glympse::GC::INVITE_TYPE_LINK, NULL, NULL);
    
    bool succeeded = ticket->addInvite(invite);
    if (!succeeded)
    {
        [self showGlympseUrl:@"Invite creation failed."];
    }
    
    succeeded = [GlympseWrapper instance].glympse->sendTicket(ticket);
    if (!succeeded)
    {
        [self showGlympseUrl:@"Ticket send failed."];
    }
}
@end











