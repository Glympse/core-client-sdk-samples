//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "GLYUserBarCellViewController.h"

@interface GLYUserBarCellViewController ()

@end

@implementation GLYUserBarCellViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.

    UITapGestureRecognizer *singleFingerTap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleSingleTap:)];
    [self.view addGestureRecognizer:singleFingerTap];
    
    [self refesh];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void)setUser:(Glympse::GUser)user
{
    _user = user;
}

-(void)setSelected:(BOOL)selected
{
    _selected = selected;
}

- (void)setSelectionListener:(id<GLYUserBarCellListener>)delegate
{
    _delegate = delegate;
}

- (void)refesh
{
    if( NULL != _user )
    {
        Glympse::GString nickname = _user->getNickname();
        if( NULL != nickname )
        {
            NSString *nicknameString = [NSString stringWithUTF8String:nickname->getBytes()];
            [self.labelNickname setText:nicknameString];
        }
        
        [self.avatarView attachGImage:_user->getAvatar()];
    }
    if( _selected )
    {
        [self.viewContent setBackgroundColor:[UIColor orangeColor]];
    }
    else
    {
        [self.viewContent setBackgroundColor:[UIColor lightGrayColor]];
    }
}

- (void)handleSingleTap:(UITapGestureRecognizer *)recognizer
{
    if ( NULL != _delegate )
    {
        [_delegate userCellWasSelected:_user];
    }
}


@end
