//------------------------------------------------------------------------------
//
//  Copyright (c) 2013 Glympse. All rights reserved.
//
//------------------------------------------------------------------------------

#import "GLYTimerVC.h"
#import "GLYTimerControl.h"

@implementation GLYTimerVC

#pragma mark - Object Lifecycle

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self)
    {
        _duration = 0.5;
        _realTimeCountDown = NO;
    }
    return self;
}

#pragma mark - View Lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
}

- (void)viewWillAppear:(BOOL)animated
{
    _glympseTimerView.zeroMeansExpire = _realTimeCountDown;
    [_glympseTimerView setTime:_duration realTimeCountDown:_realTimeCountDown];

//    _glympseTimerView
}

- (void)viewDidUnload
{
    [self setBtnCreate:nil];
    [self setBtnCancel:nil];
    [self setGlympseTimerView:nil];
    [super viewDidUnload];
}


- (void)dealloc
{
    [_btnCreate release];
    [_btnCancel release];
    [_glympseTimerView release];
    [super dealloc];
}

- (IBAction)btnCreate_TouchUpInside:(id)sender
{
    _wasCancelled = FALSE;
    _duration = [_glympseTimerView getTime];
    
    if (_delegate && [_delegate respondsToSelector:@selector(gTimerDone:withDuration:)])
    {
        [_delegate gTimerDone:_wasCancelled withDuration:_duration];
    }
    
    [self dismissViewControllerAnimated:YES completion:NULL];
}

- (IBAction)btnCancel_TouchUpInside:(id)sender
{
    _wasCancelled = TRUE;
    _duration = [_glympseTimerView getTime];
    
    if (_delegate && [_delegate respondsToSelector:@selector(gTimerDone:withDuration:)])
    {
        [_delegate gTimerDone:_wasCancelled withDuration:_duration];
    }
    
    [self dismissViewControllerAnimated:YES completion:NULL];
}


@end
