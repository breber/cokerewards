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

#define POINTS @"points"
#define LOGGED_IN @"loggedIn"
#define SCREEN_NAME @"screenName"
#define ENTER_CODE_RESULT @"enterCodeResult"
#define POINTS_EARNED_RESULT @"pointsEarnedResult"
#define MESSAGES_RESULT @"messagesResult"

@interface CokeRewardsRequest : NSObject <XMLRPCConnectionDelegate, NSXMLParserDelegate>

@property(nonatomic) id <CokeRewardsDelegate> delegate;

+ (CokeRewardsRequest *)sharedInstance;
- (void)getPoints;
- (void)sendCode:(NSString *)code;

- (BOOL)isLoggedIn;
- (void)login:(NSString *)name withPassword:(NSString *)password;
- (void)logout;

@end
