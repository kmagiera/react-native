package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

/**
 */
class FrameBasedAnimationDriver extends AnimationDriver {

  private long mStartFrameTimeNanos = -1;
  private final double[] mFrames;
  private final double mToValue;
  private double mFromValue;
  private boolean mHasToValue;

  FrameBasedAnimationDriver(ReadableMap config) {
    ReadableArray frames = config.getArray("frames");
    int numberOfFrames = frames.size();
    mFrames = new double[numberOfFrames];
    for (int i = 0; i < numberOfFrames; i++) {
      mFrames[i] = frames.getDouble(i);
    }
    if (config.hasKey("toValue")) {
      mHasToValue = true;
      mToValue = config.getDouble("toValue");
    } else {
      mHasToValue = false;
      mToValue = Double.NaN;
    }
  }

  public boolean runAnimationStep(long frameTimeNanos) {
    if (mStartFrameTimeNanos < 0) {
      // start!
      mStartFrameTimeNanos = frameTimeNanos;
      mFromValue = mAnimatedValue.mValue;
    }
    long timeFromStartNanos = (frameTimeNanos - mStartFrameTimeNanos);
    int frameIndex = (int) (timeFromStartNanos / 1000000L / 16L);
    if (frameIndex < 0) {
      // weird, next time nanos is smaller than start time
      return false;
    } else if (!mHasFinished) {
      final double nextValue;
      if (frameIndex >= mFrames.length - 1) {
        // animation has ended!
        mHasFinished = true;
        if (mHasToValue) {
          nextValue = mToValue;
        } else {
          nextValue = mFromValue + mFrames[mFrames.length - 1];
        }
      } else if (mHasToValue) {
        nextValue = mFromValue + mFrames[frameIndex] * (mToValue - mFromValue);
      } else {
        nextValue = mFromValue + mFrames[frameIndex];
      }
      boolean updated = mAnimatedValue.mValue != nextValue;
      mAnimatedValue.mValue = nextValue;
      return updated;
    }
    return false;
  }
}
