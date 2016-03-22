/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

/**
 * Implementation of {@link AnimationDriver} which provides a support for simple time-based
 * animations that are pre-calculate on the JS side. For each animation frame JS provides a value
 * from 0 to 1 that indicates a progress of the animation at that frame.
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

  @Override
  public void runAnimationStep(long frameTimeNanos) {
    if (mStartFrameTimeNanos < 0) {
      mStartFrameTimeNanos = frameTimeNanos;
      mFromValue = mAnimatedValue.mValue;
    }
    long timeFromStartNanos = (frameTimeNanos - mStartFrameTimeNanos);
    // frames are calculated at 60FPS, to get index by a given time offset from the start of the
    // animation, we take the time diff in millisecond and divide it by 60 frames per 1000ms.
    int frameIndex = (int) (timeFromStartNanos / 1000000L * 60L / 1000L);
    if (frameIndex < 0) {
      throw new IllegalStateException("Calculated frame index should never be lower than 0");
    } else if (mHasFinished) {
      // nothing to do here
      return;
    }
    double nextValue;
    if (frameIndex >= mFrames.length - 1) {
      // animation has completed, no more frames left
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
    mAnimatedValue.mValue = nextValue;
  }
}
