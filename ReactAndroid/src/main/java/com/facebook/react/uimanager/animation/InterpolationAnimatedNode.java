package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

/**
 */
/*package*/ class InterpolationAnimatedNode extends AnimatedNode {

  private static double[] fromDoubleArray(ReadableArray ary) {
    double[] res = new double[ary.size()];
    for (int i = 0; i < res.length; i++) {
      res[i] = ary.getDouble(i);
    }
    return res;
  }

  private final double mInputRange[];
  private final double mOutputRange[];

  InterpolationAnimatedNode(ReadableMap config) {
    mInputRange = fromDoubleArray(config.getArray("inputRange"));
    mOutputRange = fromDoubleArray(config.getArray("outputRange"));
  }

  @Override
  public void feedDataFromUpdatedParent(AnimatedNode parent) {
    int rangeIndex = findRangeIndex(parent.mValue, mInputRange);
    mValue = interpolate(
      parent.mValue,
      mInputRange[rangeIndex],
      mInputRange[rangeIndex + 1],
      mOutputRange[rangeIndex],
      mOutputRange[rangeIndex + 1]);
  }

  private static double interpolate(
    double value,
    double inputMin,
    double inputMax,
    double outputMin,
    double outputMax) {
    return outputMin + (outputMax - outputMin) *
      (value - inputMin) / (inputMax - inputMin);
  }

  private static int findRangeIndex(double value, double[] ranges) {
    int index;
    for (index = 1; index < ranges.length - 1; index++) {
      if (ranges[index] >= value) {
        break;
      }
    }
    return index - 1;
  }
}
