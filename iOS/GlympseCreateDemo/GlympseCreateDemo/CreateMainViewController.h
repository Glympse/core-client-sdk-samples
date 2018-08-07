//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>

@interface CreateMainViewController : UIViewController

@property (retain, nonatomic) IBOutlet UIButton *btnCreateGlympse;

@property (retain, nonatomic) IBOutlet UITextView *tvGlympseUrl;

- (IBAction)btnCreateGlympse_TouchUpInside:(id)sender;

- (void)showGlympseUrl:(NSString *)url;

- (void)createGlympse;

@end
