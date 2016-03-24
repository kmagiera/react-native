package com.facebook.react.animated;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringLooper;

/**
 */
/*package*/ class SpringAnimation extends AnimationDriver {

  // We define no-op spring looper and no-op spring system as we don't directly use android
  // Choreographer to run "reboud" animations. Instead we have a separate animation loop and
  // only request updates from the Spring when #runAnimationStep gets called
  private static class MySpringLooper extends SpringLooper {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
  }

  private static class MySpringSystem extends BaseSpringSystem {
    public MySpringSystem() {
      super(new MySpringLooper());
    }
  }

  private final BaseSpringSystem mSpringSystem;
  private final Spring mSpring;
  private long mLastTime;
  private boolean mSpringStarted;

  SpringAnimation(ReadableMap config) {
    boolean overshootClamping = config.getBoolean("overshootClamping");
    double restDisplacementThreshold = config.getDouble("restDisplacementThreshold");
    double restSpeedThreshold = config.getDouble("restSpeedThreshold");
    double tension = config.getDouble("tension");
    double friction = config.getDouble("friction");
    double initialVelocity = config.getDouble("initialVelocity");
    double toValue = config.getDouble("toValue");

    mSpringSystem = new MySpringSystem();
    mSpring = mSpringSystem.createSpring()
      .setSpringConfig(new SpringConfig(tension, friction))
      .setEndValue(toValue)
      .setVelocity(initialVelocity)
      .setOvershootClampingEnabled(overshootClamping)
      .setRestDisplacementThreshold(restDisplacementThreshold)
      .setRestSpeedThreshold(restSpeedThreshold);
  }

  @Override
  public void runAnimationStep(long frameTimeNanos) {
    long frameTimeMillis = frameTimeNanos / 1000000;
    if (!mSpringStarted) {
      mLastTime = frameTimeMillis;
      mSpring.setCurrentValue(mAnimatedValue.mValue, false);
      mSpringStarted = true;
    }
    mSpringSystem.loop(frameTimeMillis - mLastTime);
    mLastTime = frameTimeMillis;
    mAnimatedValue.mValue = mSpring.getCurrentValue();
    mHasFinished = mSpring.isAtRest();
  }
}
