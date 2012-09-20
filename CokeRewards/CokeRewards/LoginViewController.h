//
//  LoginViewController.h
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface LoginViewController : UIViewController {
    IBOutlet UITextField *username;
    IBOutlet UITextField *password;
}

- (IBAction)loginButtonPressed:(id)sender;

@end
