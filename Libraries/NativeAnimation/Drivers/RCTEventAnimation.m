/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import "RCTEventAnimation.h"

@implementation RCTEventAnimation
{
  NSArray<NSString *> *_eventPath;
  NSArray<NSArray *> *_filters;
}

- (instancetype)initWithEventPath:(NSArray<NSString *> *)eventPath
                          filters:(NSArray*)filters
                        valueNode:(RCTValueAnimatedNode *)valueNode
{
  if ((self = [super init])) {
    _eventPath = eventPath;
    _filters = filters;
    _valueNode = valueNode;
  }
  return self;
}

- (BOOL)testFilter:(NSArray *)filter withData:(id)data
{
  for (NSUInteger i = 0; i < filter.count - 1; i++) {
    NSString *key = filter[i];
    data = data[key];
  }
  id expectedValue = filter[filter.count - 1];
  return [expectedValue isEqual:data];
}

- (void)updateWithEvent:(id<RCTEvent>)event
{
  NSArray *args = event.arguments;
  // Supported events args are in the following order: viewTag, eventName, eventData.
  id currentValue = args[2];

  for (NSArray *filter in _filters) {
    if (![self testFilter:filter withData:(NSDictionary *)currentValue]) {
      return NO;
    }
  }

  for (NSString *key in _eventPath) {
    currentValue = [currentValue valueForKey:key];
  }

  _valueNode.value = ((NSNumber *)currentValue).doubleValue;
  [_valueNode setNeedsUpdate];
}

@end
