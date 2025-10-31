#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(VideoTransformer, NSObject)

RCT_EXTERN_METHOD(rotateVideo:(NSString *)inputPath
                  angle:(NSInteger)angle
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
