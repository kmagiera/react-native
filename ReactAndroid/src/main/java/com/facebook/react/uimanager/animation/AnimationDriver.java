package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.Callback;

/**
 */
/*package*/ abstract class AnimationDriver {
  boolean mHasFinished = false;
  ValueAnimatedNode mAnimatedValue;
  Callback mEndCallback;

  public abstract boolean runAnimationStep(long frameTimeNanos);
}
