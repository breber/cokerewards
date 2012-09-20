//
//  LoginViewController.m
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import "LoginViewController.h"
#import "KeychainItemWrapper.h"
#import "XMLRPC.h"

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
    
    if (![@"" isEqualToString:usernameText] && ![@"" isEqualToString:passwordText]) {
        [self loginButtonPressed:self];
    }
}

- (IBAction)loginButtonPressed:(id)sender {
    [keychainItem setObject:[password text] forKey:(__bridge id)(kSecValueData)];
    [keychainItem setObject:[username text] forKey:(__bridge id)(kSecAttrAccount)];
    
    // Perform login
    
    NSURL *url = [NSURL URLWithString:@"https://www.mycokerewards.com/xmlrpc"];
    XMLRPCRequest *request = [[XMLRPCRequest alloc] initWithURL:url];
    XMLRPCConnectionManager *manager = [XMLRPCConnectionManager sharedManager];

    NSDictionary *params = [NSDictionary dictionaryWithObjectsAndKeys:[username text], @"emailAddress", [password text], @"password", @"4.1", @"VERSION", nil];
    [request setMethod:@"points.pointsBalance" withParameter:params];
    
    NSLog(@"Request: %@", [request body]);
    
    [manager spawnConnectionWithXMLRPCRequest:request delegate:self];
}

- (void)request:(XMLRPCRequest *)request didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge {
    NSLog(@"didReceiveAuthenticationChallenge");
}

- (void)request:(XMLRPCRequest *)request didCancelAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge {
    NSLog(@"didCancelWithAuthenticationChallenge");
}

- (void)request:(XMLRPCRequest *)request didFailWithError:(NSError *)error {
    NSLog(@"didFailWithError: %@", [error localizedDescription]);
}

- (void)request:(XMLRPCRequest *)request didReceiveResponse:(XMLRPCResponse *)response {
    NSLog(@"didReceiveResponse: %@", [response body]);
}

- (BOOL)request:(XMLRPCRequest *)request canAuthenticateAgainstProtectionSpace:(NSURLProtectionSpace *)protectionSpace {
    NSLog(@"canAuthenticateAgainstProtectionSpace");
    return NO;
}

@end
