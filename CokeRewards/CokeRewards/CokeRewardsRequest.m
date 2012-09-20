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
}

@end

@implementation CokeRewardsRequest

static CokeRewardsRequest *instance = nil;

- (id)init
{
    self = [super init];
    if (self) {
        keychainItem = [[KeychainItemWrapper alloc] initWithIdentifier:@"CokeRewards" accessGroup:nil];
    }
    return self;
}

+ (CokeRewardsRequest *)sharedInstance {
    if (instance == nil) {
        instance = [[CokeRewardsRequest alloc] init];
    }
    
    return instance;
}

- (BOOL)isLoggedIn {
    NSString *passwordText = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    NSString *usernameText = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    
    return ![@"" isEqualToString:usernameText] && ![@"" isEqualToString:passwordText];
}

- (void)getPoints:(NSString *)name withPassword:(NSString *)password {
    [keychainItem setObject:password forKey:(__bridge id)(kSecValueData)];
    [keychainItem setObject:name forKey:(__bridge id)(kSecAttrAccount)];
    
    [self getPoints];
}

- (void)getPoints {
    NSURL *url = [NSURL URLWithString:@"https://www.mycokerewards.com/xmlrpc"];
    XMLRPCRequest *request = [[XMLRPCRequest alloc] initWithURL:url];
    XMLRPCConnectionManager *manager = [XMLRPCConnectionManager sharedManager];
    
    NSString *username = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    NSString *password = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    
    // TODO: send screenName
    
    NSDictionary *params = [NSDictionary dictionaryWithObjectsAndKeys:username, @"emailAddress", password, @"password", @"4.1", @"VERSION", nil];
    [request setMethod:@"points.pointsBalance" withParameter:params];
    [manager spawnConnectionWithXMLRPCRequest:request delegate:self];
}

- (void)sendCode:(NSString *)code {
    NSURL *url = [NSURL URLWithString:@"https://www.mycokerewards.com/xmlrpc"];
    XMLRPCRequest *request = [[XMLRPCRequest alloc] initWithURL:url];
    XMLRPCConnectionManager *manager = [XMLRPCConnectionManager sharedManager];
    
    NSString *username = [keychainItem objectForKey:(__bridge id)(kSecAttrAccount)];
    NSString *password = [keychainItem objectForKey:(__bridge id)(kSecValueData)];
    
    // TODO: send screenName
    
    NSDictionary *params = [NSDictionary dictionaryWithObjectsAndKeys:username, @"emailAddress", password, @"password", @"4.1", @"VERSION", code, @"capCode", nil];
    [request setMethod:@"points.enterCode" withParameter:params];
    [manager spawnConnectionWithXMLRPCRequest:request delegate:self];
}

- (void)logout {
    [keychainItem resetKeychainItem];
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
    
    NSXMLParser *parser = [[NSXMLParser alloc] initWithData:[[response body] dataUsingEncoding:NSASCIIStringEncoding]];
    parser.delegate = self;
    [parser parse];
}

- (BOOL)request:(XMLRPCRequest *)request canAuthenticateAgainstProtectionSpace:(NSURLProtectionSpace *)protectionSpace {
    NSLog(@"canAuthenticateAgainstProtectionSpace");
    return NO;
}

// NSXMLParserDelegate


@end
