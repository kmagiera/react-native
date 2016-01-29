package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableMap;

/**
 */
class ValueAnimatedNode extends AnimatedNode {

  ValueAnimatedNode(ReadableMap config) {
    mValue = config.getDouble("value");
  }
}
