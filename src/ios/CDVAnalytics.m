//
//  CDVAnalytics.m
//  OUAnywhere
//

#import "CDVAnalytics.h"

@implementation CDVAnalytics {
    NSString *logFile;
}

-(void)log:(CDVInvokedUrlCommand *)command {
    NSString *callbackID = command.callbackId;
    
    NSMutableDictionary* options = [command.arguments objectAtIndex:0];
    
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
	
    // save the required parameters into user defaults ready for app delegate to use on start up
    
    [userDefaults setObject:[options objectForKey:@"server"] forKey:@"Server"];
	
    [userDefaults setObject:[options objectForKey:@"uri"] forKey:@"Uri"];
    
    [userDefaults setObject:[options objectForKey:@"oucu"] forKey:@"Oucu"];
    
    [userDefaults setObject:[options objectForKey:@"key"] forKey:@"Key"];
    
    // get full path to Documents
    
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    
    NSString *documentsDirectory = [paths objectAtIndex:0];
    
    // and build path to event log
    
    logFile = [documentsDirectory stringByAppendingPathComponent:[options objectForKey:@"logfile"]];
    
    // save to user defaults too...for app delegate
    
    [userDefaults setValue:logFile forKey:@"LogFilePath"];
    
    [userDefaults synchronize];

    // create the dictionary to xml
    
    NSString *string = [self DictionaryToXML:[command.arguments objectAtIndex:1]];
    
    // only if we actually have a string to process
    
    if (string) {
        
        // convert xml string to data
        
        NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
    
        // attempt to get a file handle on log file
        
        NSFileHandle *fileHandle = [NSFileHandle fileHandleForWritingAtPath:logFile];
   
        // if we got one
        
        if (fileHandle){
    
            // jump to end of ile
            
            [fileHandle seekToEndOfFile];
    
            // and append to it
            
            [fileHandle writeData:data];
        
            // done...
            
            [fileHandle closeFile];
        }
        else{
        
            // if no file handle just write data out to create it
            
            [data writeToFile:logFile atomically:NO];
        }
    }
}

-(NSString *)DictionaryToXML:(NSDictionary *)dict {
    
    // key will hold the value of each dictionary key in turn
    
    NSString *key;
    
    // start of xml
    
    NSString *event = @"<event ";

    // for each key searched if found append to xml
    
    key = [dict objectForKey:@"oucu"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"oucu=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"website"];
    
    if (key) {
    
        event = [event stringByAppendingString:[NSString stringWithFormat:@"website=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"time"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"time=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"action"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"action=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"info"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"info=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"resourceid"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"resourceid=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"filetype"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"filetype=\"%@\" ", key]];
    }
    
    key = [dict objectForKey:@"fileurl"];
    
    if (key) {
        
        event = [event stringByAppendingString:[NSString stringWithFormat:@"fileurl=\"%@\" ", key]];
    }
    
    // return xml or nil if none was processed...
    
    return [event isEqualToString:@"<event "] ? nil : [event stringByAppendingString:@"/>"];
 }

@end
