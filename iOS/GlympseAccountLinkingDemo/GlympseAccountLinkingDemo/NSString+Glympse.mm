//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "NSString+Glympse.h"

@implementation NSString (Glympse)

+ (NSString *)stringWithGString:(const Glympse::GString &)gString
{
    if ( gString == NULL )
    {
        return @"";
    }
    
    const char * str = gString->toCharArray();
    if ( str )
    {
        return [NSString stringWithUTF8String:str];
    }
    else
    {
        return @"";
    }
}

@end

