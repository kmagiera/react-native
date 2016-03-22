/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableMap;

/**
 * Basic type of animated node that maps directly from {@code Animated.Value(x)} of Animated.js
 * library.
 */
class ValueAnimatedNode extends AnimatedNode {

  /*package*/ double mValue = Double.NaN;

  ValueAnimatedNode(ReadableMap config) {
    mValue = config.getDouble("value");
  }
}
