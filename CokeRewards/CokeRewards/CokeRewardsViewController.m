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

- (IBAction)logoutButtonPressed:(id)sender {
    [self userDidLogout];
}

- (void)userDidLogout {
    [self.cokeRewards logout];
    [self.navigationController popToRootViewControllerAnimated:YES];
}

- (void)pointCountDidUpdate:(int)points {
    // TODO: update ui
}

@end
