//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import <UIKit/UIKit.h>
@class GLYTimerControl;

@protocol GLYTimerVCDelegate <NSObject>

- (void)gTimerDone:(BOOL)wasCancelled withDuration:(NSTimeInterval)duration;

@end


@interface GLYTimerVC : UIViewController
{
    IBOutlet UILabel *          _lblDurationNumeric;
    IBOutlet UILabel *          _lblDurationUnits;
}
@property (nonatomic, assign) id<GLYTimerVCDelegate> delegate;
@property (nonatomic, assign) BOOL wasCancelled;
@property (nonatomic, assign) NSTimeInterval duration;
@property (nonatomic, assign) BOOL realTimeCountDown;

@property (retain, nonatomic) IBOutlet GLYTimerControl *glympseTimerView;
@property (retain, nonatomic) IBOutlet UIButton *btnCreate;
@property (retain, nonatomic) IBOutlet UIButton *btnCancel;

- (IBAction)btnCreate_TouchUpInside:(id)sender;
- (IBAction)btnCancel_TouchUpInside:(id)sender;

@end
