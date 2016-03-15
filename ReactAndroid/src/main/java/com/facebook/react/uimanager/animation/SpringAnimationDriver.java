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
import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringLooper;

/**
 * Implementation of {@link AnimationDriver} providing support for running spring-based animation
 * off the JS thread.
 *
 * It uses implementation of spring system from "rebound" library.
 */
class SpringAnimationDriver extends AnimationDriver {

  private static class NoopSpringLooper extends SpringLooper {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
  }

  private static class NoopSpringSystem extends BaseSpringSystem {
    public NoopSpringSystem() {
      super(new NoopSpringLooper());
    }
  }

  private final BaseSpringSystem mSpringSystem;
  private final Spring mSpring;
  private long mLastTime;
  private boolean mSpringStarted;

  SpringAnimationDriver(ReadableMap config) {
    boolean overshootClamping = config.getBoolean("overshootClamping");
    double restDisplacementThreshold = config.getDouble("restDisplacementThreshold");
    double restSpeedThreshold = config.getDouble("restSpeedThreshold");
    double tension = config.getDouble("tension");
    double friction = config.getDouble("friction");
    double toValue = config.getDouble("toValue");

    mSpringSystem = new NoopSpringSystem();
    mSpring = mSpringSystem.createSpring()
      .setSpringConfig(new SpringConfig(tension, friction))
      .setEndValue(toValue)
      .setOvershootClampingEnabled(overshootClamping)
      .setRestDisplacementThreshold(restDisplacementThreshold)
      .setRestSpeedThreshold(restSpeedThreshold);
  }

  @Override
  public boolean runAnimationStep(long frameTimeNanos) {
    long frameTimeMillis = frameTimeNanos / 1000000;
    if (!mSpringStarted) {
      mLastTime = frameTimeMillis;
      mSpring.setCurrentValue(mAnimatedValue.mValue, false);
      mSpringStarted = true;
    }
    long ts = frameTimeMillis - mLastTime;
    mSpringSystem.loop(frameTimeMillis - mLastTime);
    mLastTime = frameTimeMillis;
    mAnimatedValue.mValue = mSpring.getCurrentValue();
    mHasFinished = mSpring.isAtRest();
    return true;
  }
}
