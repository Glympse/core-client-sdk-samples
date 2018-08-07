//------------------------------------------------------------------------------
//
//  Copyright (c) 2014 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

@interface TriggersListViewController : UIViewController < UITableViewDataSource, UITableViewDelegate >

@property (strong, nonatomic) IBOutlet UITableView *tableViewTriggers;
@property (strong, nonatomic) IBOutlet UIButton *buttonExpireAll;

- (IBAction)buttonExpireAllPressed:(id)sender;

@end
