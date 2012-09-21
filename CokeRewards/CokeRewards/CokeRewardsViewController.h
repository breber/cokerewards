//
//  CokeRewardsViewController.h
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "CokeRewardsRequest.h"

@interface CokeRewardsViewController : UIViewController <CokeRewardsDelegate> {
    IBOutlet UILabel *welcomeLabel;
    IBOutlet UILabel *pointCountLabel;
    IBOutlet UITextField *codeTextField;
}

- (IBAction)submitCode:(id)sender;
- (IBAction)logoutButtonPressed:(id)sender;

@end
