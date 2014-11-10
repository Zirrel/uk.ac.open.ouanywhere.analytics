//
//  CDVAnalytics.h
//  OUAnywhere
//

#import <Cordova/CDV.h>

@interface CDVAnalytics : CDVPlugin

-(void)log:(CDVInvokedUrlCommand *)command;

@end
