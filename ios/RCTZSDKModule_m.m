//
//  RCTZSDKModule_m.m
//  ZSDKRCTDevDemo

#import <React/RCTBridgeModule.h>
#import <ExternalAccessory/ExternalAccessory.h>
#import "MfiBtPrinterConnection.h"
#import "ZebraPrinter.h"

@interface RCTZSDKModule_m : NSObject <RCTBridgeModule>
@end

@implementation RCTZSDKModule_m

RCT_EXPORT_MODULE(ZSDKModule);
RCT_EXPORT_METHOD(zsdkPrinterDiscoveryBluetooth:(RCTResponseSenderBlock)callback)
{
  EAAccessoryManager *manager = [EAAccessoryManager sharedAccessoryManager];
  NSArray<EAAccessory *> *connected = [manager connectedAccessories];
  NSMutableArray *lista = [NSMutableArray array];

  for (EAAccessory *acc in connected) {
    if ([acc.protocolStrings containsObject:@"com.zebra.rawport"]) {
      NSDictionary *info = @{
        @"name": acc.name ?: @"",
        @"serial": acc.serialNumber ?: @""
      };
      [lista addObject:info];
    }
  }

  NSError *error = nil;
  NSData *jsonData = [NSJSONSerialization dataWithJSONObject:lista options:0 error:&error];
  NSString *json = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
  if (!json) json = @"[]";
  callback(@[[NSNull null], json]);
}

RCT_EXPORT_METHOD(zsdkWriteBluetooth:(NSString *)serial
                  data:(NSString *)data
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  id<ZebraPrinterConnection, NSObject> connection =
    [[MfiBtPrinterConnection alloc] initWithSerialNumber:serial];

  if (![connection open]) {
    reject(@"open_error", @"No se pudo abrir la conexión con la impresora.", nil);
    return;
  }

  NSError *writeError = nil;
  NSData *payload = [data dataUsingEncoding:NSUTF8StringEncoding];
  [connection write:payload error:&writeError];
  [connection close];

  if (writeError) {
    reject(@"write_error", @"Error al enviar datos a la impresora.", writeError);
    return;
  }
  resolve(@"Impresión enviada");
}

RCT_EXPORT_METHOD(zsdkQueryBluetooth:(NSString *)serial
                  data:(NSString *)data
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  id<ZebraPrinterConnection, NSObject> connection =
    [[MfiBtPrinterConnection alloc] initWithSerialNumber:serial];

  if (![connection open]) {
    reject(@"open_error", @"No se pudo abrir la conexión con la impresora.", nil);
    return;
  }

  NSError *writeError = nil;
  NSData *payload = [data dataUsingEncoding:NSUTF8StringEncoding];
  [connection write:payload error:&writeError];
  if (writeError) {
    [connection close];
    reject(@"write_error", @"Error al enviar datos a la impresora.", writeError);
    return;
  }

  NSMutableData *buffer = [NSMutableData data];
  NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:1.5];

  while ([[NSDate date] compare:deadline] == NSOrderedAscending) {
    @autoreleasepool {
      NSError *readError = nil;
      NSData *chunk = nil;

      if ([connection respondsToSelector:@selector(read:)]) {
        NSMethodSignature *sig = [(NSObject *)connection methodSignatureForSelector:@selector(read:)];
        NSInvocation *inv = [NSInvocation invocationWithMethodSignature:sig];
        [inv setSelector:@selector(read:)];
        [inv setTarget:connection];
        NSError *tmpErr = nil;
        [inv setArgument:&tmpErr atIndex:2];
        [inv invoke];
        __unsafe_unretained NSData *ret = nil;
        [inv getReturnValue:&ret];
        chunk = ret;
        readError = tmpErr;
      }

      if (readError) { break; }
      if (chunk == nil || chunk.length == 0) { break; }
      [buffer appendData:chunk];

      if (buffer.length > 0) { break; }
    }
  }

  [connection close];

  NSString *respuesta = [[NSString alloc] initWithData:buffer encoding:NSUTF8StringEncoding];
  if (!respuesta) respuesta = @"";
  resolve(respuesta);
}

@end
