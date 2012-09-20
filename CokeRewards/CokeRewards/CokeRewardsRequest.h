//
//  CokeRewardsRequest.h
//  CokeRewards
//
//  Created by Brian Reber on 9/20/12.
//  Copyright (c) 2012 Brian Reber. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "XMLRPC.h"

@protocol CokeRewardsDelegate <NSObject>

- (void)userDidLogout;
- (void)pointCountDidUpdate:(int)points;

@end

@interface CokeRewardsRequest : NSObject <XMLRPCConnectionDelegate, NSXMLParserDelegate>

@property(nonatomic) id <CokeRewardsDelegate> delegate;

+ (CokeRewardsRequest *)sharedInstance;
- (BOOL)isLoggedIn;

- (void)getPoints;
- (void)getPoints:(NSString *)name withPassword:(NSString *)password;

- (void)sendCode:(NSString *)code;

- (void)logout;

@end
