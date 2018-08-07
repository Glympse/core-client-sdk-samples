//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

@interface RequestMainViewController : UIViewController

@property (retain, nonatomic) IBOutlet UIButton *requestGlympseBtn;

@property (retain, nonatomic) IBOutlet UITextField *requestAddressCtrl;
@property (retain, nonatomic) IBOutlet UITextField *viewerAddressCtrl;

@property (retain, nonatomic) IBOutlet UITextView *groupUrlCtrl;

@property (retain, nonatomic) IBOutlet UILabel *explainGroupTypeLbl;
@property (retain, nonatomic) IBOutlet UILabel *explainPrivateTypeLbl;
@property (retain, nonatomic) IBOutlet UISegmentedControl *selectTypeCtrl;

- (IBAction)requestGlympseBtn_TouchUpInside:(id)sender;
- (IBAction)selectTypeCtrl_ValueChanged:(id)sender;

- (IBAction)tapRecognizer_selector:(UITapGestureRecognizer *)sender;

@end
