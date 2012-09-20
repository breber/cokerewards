//
//  LoginViewController.m
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import "LoginViewController.h"
#import "CokeRewardsRequest.h"

@interface LoginViewController ()

@property(nonatomic) CokeRewardsRequest *cokeRewards;

@end

@implementation LoginViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.cokeRewards = [CokeRewardsRequest sharedInstance];
    self.cokeRewards.delegate = self;
    
    if ([self.cokeRewards isLoggedIn]) {
        [self.navigationController performSegueWithIdentifier:@"loggedin" sender:self];
    }
}

- (IBAction)loginButtonPressed:(id)sender {
    // Perform login
    [self.cokeRewards getPoints:[username text] withPassword:[password text]];
}

- (void)userDidLogout {
    // TODO: show a message saying error message
}

- (void)pointCountDidUpdate:(int)points {
    [self.navigationController performSegueWithIdentifier:@"loggedin" sender:self];
}

@end
