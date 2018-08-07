//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>


@interface SendMainViewController : UIViewController
{
}

@property (retain, nonatomic) IBOutlet UITextView *invitesCtrl;
@property (retain, nonatomic) IBOutlet UITextView *messageCtrl;

@property (retain, nonatomic) IBOutlet UISegmentedControl *configureCtrl;
@property (retain, nonatomic) IBOutlet UIButton *sendGlympseBtn;
@property (retain, nonatomic) IBOutlet UITextView *recipientsCtrl;
@property (retain, nonatomic) IBOutlet UITextField *durationStatusCtrl;
@property (retain, nonatomic) IBOutlet UITextField *watchedStatusCtrl;
@property (retain, nonatomic) IBOutlet UISegmentedControl *actionsCtrl;
@property (retain, nonatomic) IBOutlet UILabel *recipientsLbl;

@property (retain, nonatomic) NSTimer *refreshTimer;

- (IBAction)sendGlympseBtn_TouchUpInside:(id)sender;

- (IBAction)configureCtrl_ValueChanged:(id)sender;
- (IBAction)actionsCtrl_ValueChanged:(id)sender;

- (IBAction)tapRecognizer_selector:(UITapGestureRecognizer *)sender;
#pragma mark - Glympse: Platform Methods

/**
 * Glympse: toggle notification observing
 */
- (void)enableObservingGlympsePlatformEvents:(BOOL)doObserve;

- (BOOL)isTicketActive:(Glympse::GTicket)aTicket;

#pragma mark - Glympse: View-Helper Methods

#pragma mark -- Active Glympse actions
- (void)expireGlympse;
- (void)plusFifteenMins;
- (void)modifyGlympse;

#pragma mark -- Update UI in response to platform events
- (void)updateGlympseTicketUi;
- (void)refreshDurationAndWatchers;
- (BOOL)restoreOrCreateTicketForUse;

#pragma mark -- Derive status information from the active Glympse

- (NSString *)getRecipientsAsString;
- (NSString *)getNumberWatchingAsString;
- (NSString *)getTimeRemainingAsString;


@end
