//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "Glympse.h"

#ifndef __cplusplus
    #error You must rename your files to .mm to use the GlympseAPI ObjC++ library.
#endif

#ifndef GLYMPSE_H__GLYMPSE__
    #error Glympse header undefined -- Be sure to import "Glympse.h" in your project's .pch file.
#endif

/**
 * Singleton class that provides convenience functions for managing the Glympse platform lifespan
 */
@interface GlympseWrapper : NSObject 
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
 * Glympse: platform creation and startup.
 *
 * Optionally requests server for history of expired glympses. See IGlympse::setRestoreHistory(bool restore)
 * and IHistoryManager::getTickets() for more information.
 *
 * @param restoreHistory YES to include expired glympses in ticket array.
 */
- (void) startWithHistory:(BOOL)restoreHistory;

/**
 * Glympse: platform creation and startup.
 *
 * Optionally requests server for history of expired glympses. See IGlympse::setRestoreHistory(bool restore)
 * and IHistoryManager::getTickets() for more information.
 *
 * @param restoreHistory YES to include expired glympses in ticket array.
 * @param isExempt NO should be set if the host application is handling user consent.
 * For testing purposes this can be set to YES, but it should be set to NO for applications
 * that will be used by real users.
 */
- (void) startWithHistory:(BOOL)restoreHistory withConsentExemption:(BOOL)isExempt;

/**
 * Glympse: platform shutdown and cleanup
 */
- (void) stop;

@end

