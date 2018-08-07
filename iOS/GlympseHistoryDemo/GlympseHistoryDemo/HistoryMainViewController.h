//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>


@interface HistoryMainViewController : UIViewController<UITableViewDataSource, UITableViewDelegate, UITextViewDelegate, UIAlertViewDelegate>
{
}

@property (retain, nonatomic) IBOutlet UITextView *invitesCtrl;
@property (retain, nonatomic) IBOutlet UITextView *messageCtrl;

@property (retain, nonatomic) IBOutlet UISegmentedControl *configureCtrl;
@property (retain, nonatomic) IBOutlet UIButton *sendGlympseBtn;
@property (retain, nonatomic) IBOutlet UITableView *glympseTableView;

@property (retain, nonatomic) NSTimer *refreshTimer;

- (IBAction)sendGlympseBtn_TouchUpInside:(id)sender;

- (IBAction)configureCtrl_ValueChanged:(id)sender;

- (IBAction)tapRecognizer_selector:(UITapGestureRecognizer *)sender;
#pragma mark - Glympse: Platform Methods

/**
 * Glympse: toggle notification observing
 */
- (void)enableObservingGlympsePlatformEvents:(BOOL)doObserve;

- (BOOL)isTicketActive:(Glympse::GTicket)aTicket;

#pragma mark - Glympse: View-Helper Methods

#pragma mark -- Active Glympse actions
- (void)expireGlympse:(Glympse::GTicket)activeTicket;
- (void)plusFifteenMins:(Glympse::GTicket)activeTicket;
- (void)modifyGlympse:(Glympse::GTicket)activeTicket;

#pragma mark -- Update UI in response to platform events
- (void)updateMainGlympseUi;


@end
