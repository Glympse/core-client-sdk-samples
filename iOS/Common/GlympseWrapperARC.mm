//------------------------------------------------------------------------------
//
// Copyright (c) 2014 Glympse Inc.  All rights reserved.
//
//------------------------------------------------------------------------------

#import "GlympseWrapperARC.h"

@interface GlympseWrapper () <GLYEventListener>

@end

@implementation GlympseWrapper

@dynamic glympse;

#pragma mark - Singleton Initialization

static GlympseWrapper * sharedInstance = nil;

+ (GlympseWrapper *)instance
{
    static dispatch_once_t dispatchOncePredicate = 0;
    dispatch_once(&dispatchOncePredicate, ^{
        sharedInstance = [[super allocWithZone:NULL] init];
        [sharedInstance singletonInit];
    });
    return sharedInstance;
}

+ (id) allocWithZone:(NSZone *)zone
{
    // If coder misunderstands this is a singleton, behave properly with
    // ref count +1 on alloc anyway, and still return singleton!
    return [GlympseWrapper instance];
}

- (void)singletonInit
{
    _apiKey = Glympse::CoreFactory::createString("<< Your API key >>");
    _serverAddress = Glympse::CoreFactory::createString("api.glympse.com");
    
    if (_apiKey->equals("<< Your API key >>"))
    {
        NSLog(@"UNABLE TO RUN DEMO: You must pass the Glympse platform a valid API key.");
        assert(false);
    }
    
    // Create Glympse platform and pass in the server URL and API key.
    _glympse = Glympse::GlympseFactory::createGlympse(_serverAddress, _apiKey);
}

#pragma mark - Public Methods

- (void) startWithHistory:(BOOL)restoreHistory
{
    // Mark this application as exempt from needing user consent (since this is a dev app)
    // See GlympseCreateDemo for an example of handling user consent
    [self startWithHistory:restoreHistory withConsentExemption:true];
}

- (void) startWithHistory:(BOOL)restoreHistory withConsentExemption:(BOOL)isExempt
{
    if ( !_glympse->isStarted() )
    {
        [GLYGlympse subscribe:self onSink:_glympse];
        
        _glympse->setSmsSendMode(Glympse::GlympseConstants::SMS_SEND_MODE_DEVICE);
        _glympse->overrideLoggingLevels(Glympse::CC::INFO, Glympse::CC::INFO);
        _glympse->setEtaMode(Glympse::GC::ETA_MODE_INTERNAL);
        _glympse->setRestoreHistory(restoreHistory);
        _glympse->getConsentManager()->exemptFromConsent(isExempt);
        
        // Start the Glympse platform
        _glympse->start();
    }
}

- (void) stop
{
    [GLYGlympse unsubscribe:self onSink:_glympse];
    
    // Shutdown the Glympse platform.
    _glympse->stop();        
    _glympse = NULL;
    
    // Create Glympse platform and pass in the server URL and API key.
    _glympse = Glympse::GlympseFactory::createGlympse(_serverAddress, _apiKey);
}

/**
 * Respond to Glympse platform events
 */
- (void)glympseEvent:(const Glympse::GGlympse&)glympse
            listener:(int)listener
              events:(int)events
              object:(const Glympse::GCommon&)object
{
    // ALWAYS check listener code first, in addition to events code -- it's possible to have a collision on event-codes!
    if ( 0 != (listener & Glympse::GE::LISTENER_PLATFORM) )
    {
        if ( 0 != (events & Glympse::GE::PLATFORM_SYNCED_WITH_SERVER) )
        {
            Glympse::GUserManager userManager = _glympse->getUserManager();
            if (userManager == NULL)
            {
                // Never going to happen in this case, but you should always take care to check for NULL
                // before using a manager object from the platform.
                return;
            }
            
            // A user will typically want to set their own nickname & avatar, but we set them here for Demo purposes.
            
            // Only set nickname on startup if self-user has none!
            if ( userManager->getSelf()->getNickname() == NULL )
            {
                Glympse::GString demoNickname = Glympse::CoreFactory::createString("Demo user");
                userManager->getSelf()->setNickname(demoNickname);
            }
            
            // Only set avatar on startup if self-user has none!
            if ( userManager->getSelf()->getAvatar()->getUrl() == NULL )
            {
                //Largest avatar size is 320x320 @ 72 PPI -- anything larger is resized prior to upload.
                NSString *avatarUri = [[[NSBundle mainBundle] URLForResource:@"icon@2x"
                                                               withExtension:@"png"] absoluteString];
                // Attempt to load avatar from local file.
                Glympse::GDrawable demoAvatar =
                    Glympse::CoreFactory::createDrawable(Glympse::CoreFactory::createString([avatarUri UTF8String]), 0);
                userManager->getSelf()->setAvatar(demoAvatar);
            }
        }
        else if ( 0 != (events & Glympse::GE::PLATFORM_ACCOUNT_CREATE_FAILED) )
        {
            NSLog(@"UNABLE TO RUN DEMO -- PLATFORM_ACCOUNT_CREATE_FAILED");
            Glympse::GServerError error = object;
            if (error != NULL && error->getErrorDetails() != NULL)
            {
                NSLog(@"Error Details: %s", error->getErrorDetails()->getBytes());
            }
            assert(false);
        }
        else if ( 0 != (events & Glympse::GE::PLATFORM_LOGIN_FAILED) )
        {
            NSLog(@"UNABLE TO RUN DEMO -- PLATFORM_LOGIN_FAILED");
            Glympse::GServerError error = object;
            if (error != NULL && error->getErrorDetails() != NULL)
            {
                NSLog(@"Error Details: %s", error->getErrorDetails()->getBytes());
            }
            assert(false);
        }
    }
}

#pragma mark - Dynamic Property Implementation

- (Glympse::GGlympse)glympse
{
    return _glympse;
}

- (Glympse::GString)serverAddress
{
    return _serverAddress;
}

@end
