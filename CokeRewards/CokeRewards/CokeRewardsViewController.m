//
//  CokeRewardsViewController.m
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import "CokeRewardsViewController.h"
#import "CokeRewardsRequest.h"

@interface CokeRewardsViewController ()

@property(nonatomic) CokeRewardsRequest *cokeRewards;

@end

@implementation CokeRewardsViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.cokeRewards = [CokeRewardsRequest sharedInstance];
    self.cokeRewards.delegate = self;
}

- (IBAction)submitCode:(id)sender {
    // TODO: submit code
}

- (IBAction)logoutButtonPressed:(id)sender {
    [self userDidLogout];
}

- (void)userDidLogout {
    [self.cokeRewards logout];
    [self.navigationController popToRootViewControllerAnimated:YES];
}

- (void)pointCountDidUpdate:(int)points {
    // TODO: update ui
    NSUserDefaults *prefs = [NSUserDefaults standardUserDefaults];
    
    NSString *name = [prefs objectForKey:SCREEN_NAME];
    if (![@"" isEqualToString:name]) {
        [welcomeLabel setText:[NSString stringWithFormat:@"Welcome %@!", name]];
    } else {
        // TODO: get from keystore
        [welcomeLabel setText:[NSString stringWithFormat:@"Welcome %@!", @"Anonymous"]];
    }
    
    NSInteger pointCount = [prefs integerForKey:POINTS];
    [pointCountLabel setText:[NSString stringWithFormat:@"%d", pointCount]];
    
    
}

@end
