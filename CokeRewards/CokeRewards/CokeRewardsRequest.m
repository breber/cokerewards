//
//  CokeRewardsRequest.m
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import "CokeRewardsRequest.h"
#import "KeychainItemWrapper.h"
#import "XMLRPC.h"

@interface CokeRewardsRequest() {
    KeychainItemWrapper *keychainItem;
    NSUserDefaults *preferences;
}

@end

@implementation CokeRewardsRequest

static CokeRewardsRequest *instance = nil;

- (id)init
{
    self = [super init];
    if (self) {
        keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CokeRewards" accessGroup:nil];
        preferences = [NSUserDefaults standardUserDefaults];
    }
    return self;
}

+ (CokeRewardsRequest *)sharedInstance {
    if (instance == nil) {
        instance = [[CokeRewardsRequest alloc] init];
    }
    
    return instance;
}

- (void)initPreferences {
    [preferences setInteger:0 forKey:POINTS];
    [preferences setBool:NO forKey:LOGGED_IN];
    [preferences setObject:@"" forKey:SCREEN_NAME];
    [preferences setBool:NO forKey:ENTER_CODE_RESULT];
    [preferences setInteger:0 forKey:POINTS_EARNED_RESULT];
    [preferences setObject:@"" forKey:MESSAGES_RESULT];
}

- (BOOL)isLoggedIn {
    NSString *passwordText = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    NSString *usernameText = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    
    return ![@"" isEqualToString:usernameText] && ![@"" isEqualToString:passwordText];
}

- (void)login:(NSString *)name withPassword:(NSString *)password {
    [keychainItem setObject:password forKey:(__bridge id)(kSecValueData)];
    [keychainItem setObject:name forKey:(__bridge id)(kSecAttrAccount)];
    
    [self initPreferences];
    
    [self getPoints];
}

- (void)getPoints {
    NSURL *url = [NSURL URLWithString:@"https://www.mycokerewards.com/xmlrpc"];
    XMLRPCRequest *request = [[XMLRPCRequest alloc] initWithURL:url];
    XMLRPCConnectionManager *manager = [XMLRPCConnectionManager sharedManager];
    
    NSString *username = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    NSString *password = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    NSString *screenName = [preferences objectForKey:SCREEN_NAME];
    
    NSDictionary *params = [NSDictionary dictionaryWithObjectsAndKeys:username, @"emailAddress", password, @"password", @"4.1", @"VERSION", screenName, @"screenName", nil];
    [request setMethod:@"points.pointsBalance" withParameter:params];
    [manager spawnConnectionWithXMLRPCRequest:request delegate:self];
}

- (void)sendCode:(NSString *)code {
    NSURL *url = [NSURL URLWithString:@"https://www.mycokerewards.com/xmlrpc"];
    XMLRPCRequest *request = [[XMLRPCRequest alloc] initWithURL:url];
    XMLRPCConnectionManager *manager = [XMLRPCConnectionManager sharedManager];
    
    NSString *username = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    NSString *password = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    NSString *screenName = [preferences objectForKey:SCREEN_NAME];
        
    NSDictionary *params = [NSDictionary dictionaryWithObjectsAndKeys:username, @"emailAddress", password, @"password", @"4.1", @"VERSION", code, @"capCode", screenName, @"screenName", nil];
    [request setMethod:@"points.enterCode" withParameter:params];
    [manager spawnConnectionWithXMLRPCRequest:request delegate:self];
}

- (void)logout {
    [keychainItem resetKeychainItem];
    [self initPreferences];
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
