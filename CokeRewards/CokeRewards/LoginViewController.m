//
//  LoginViewController.m
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import "LoginViewController.h"
#import "KeychainItemWrapper.h"

@interface LoginViewController () {
    KeychainItemWrapper *keychainItem;
}

@end

@implementation LoginViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CokeRewards" accessGroup:nil];

    NSString *passwordText = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    NSString *usernameText = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    
    NSLog(@"Username: %@ Password: %@", usernameText, passwordText);
}

- (IBAction)loginButtonPressed:(id)sender {
    [keychainItem setObject:[password text] forKey:(__bridge id)(kSecValueData)];
    [keychainItem setObject:[username text] forKey:(__bridge id)(kSecAttrAccount)];
    
    // Perform login
}

@end
