#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(VideoTransformer, NSObject)

RCT_EXTERN_METHOD(rotateVideo:(NSString *)inputPath
                  angle:(NSInteger)angle
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(cropVideo:(NSString *)inputPath
                  aspectRatio:(NSString *)aspectRatio
                  position:(NSString *)position
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(cropAndRotateVideo:(NSString *)inputPath
                  aspectRatio:(NSString *)aspectRatio
                  position:(NSString *)position
                  angle:(NSInteger)angle
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)

@end
