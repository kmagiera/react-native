package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

/**
 */
class MultiplicationAnimatedNode extends AnimatedNode {
  private final NativeAnimatedModule mNativeAnimatedModule;
  private final int[] mInputNodes;

  MultiplicationAnimatedNode(ReadableMap config, NativeAnimatedModule nativeAnimatedModule) {
    mNativeAnimatedModule = nativeAnimatedModule;
    ReadableArray inputNodes = config.getArray("input");
    mInputNodes = new int[inputNodes.size()];
    for (int i = 0; i < mInputNodes.length; i++) {
      mInputNodes[i] = inputNodes.getInt(i);
    }
  }

  @Override
  public void runAnimationStep(long frameTimeNanos) {
    mValue = 1;
    for (int i = 0; i < mInputNodes.length; i++) {
      mValue *= mNativeAnimatedModule.mAnimatedNodes.get(mInputNodes[i]).mValue;
    }
  }
}
