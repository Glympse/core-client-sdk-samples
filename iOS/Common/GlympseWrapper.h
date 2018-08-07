//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "GLYCommon/GLYSingleton.h"

#ifndef __cplusplus
    #error You must rename your files to .mm to use the GlympseAPI ObjC++ library.
#endif

#ifndef GLYMPSE_H__GLYMPSE__
    #error Glympse header undefined -- Be sure to import "Glympse.h" in your project's .pch file.
#endif

/**
 * Singleton class that provides convenience functions for managing the Glympse platform lifespan
 */
@interface GlympseWrapper : GLYSingleton

{
@private
    Glympse::GGlympse _glympse;
    
    Glympse::GString _serverAddress;
    Glympse::GString _apiKey;
}

/**
 * @returns Singleton instance of GlympseWrapper class
 */
+ (GlympseWrapper *)instance;


/**
 * Property to access this singleton's instance of the GGlympse C++ object.
 */
@property (nonatomic, readonly) Glympse::GGlympse glympse;

@property (nonatomic, readonly) Glympse::GString serverAddress;

/**
 * Glympse: platform creation and startup. Optionally requests server for history of expired glympses
 * See GGlympse documentation for getTickets() and setRestoreHistory(bool restore) for more information.
 * @param restoreHistory YES to include expired glympses in ticket array.
 */
- (void) startWithHistory:(BOOL)restoreHistory;

/**
 * Glympse: platform shutdown and cleanup
 */
- (void) stop;

@end

