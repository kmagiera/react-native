package com.facebook.react.uimanager.animation;

import com.facebook.infer.annotation.Assertions;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 */
/*package*/ class AnimatedNode {

  private static final int DEFAULT_ANIMATED_NODE_CHILD_COUNT = 1;

  /*package*/ @Nullable List<AnimatedNode> mChildren; /* lazy-initialized when child is added */
  /*package*/ int mActiveIncomingNodes = 0;
  /*package*/ boolean mEnqueued = false;
  public int mTag = -1;

  double mValue = Double.NaN;

  public void addChild(AnimatedNode child) {
    if (mChildren == null) {
      mChildren = new ArrayList<>(DEFAULT_ANIMATED_NODE_CHILD_COUNT);
    }
    Assertions.assertNotNull(mChildren).add(child);
  }

  public void removeChild(AnimatedNode child) {
    if (mChildren == null) {
      return;
    }
    mChildren.remove(child);
  }

  public void feedDataFromUpdatedParent(AnimatedNode parent) {
  }

  public void runAnimationStep(long frameTimeNanos) {
  }

//  public void saveInPropMap(String key, SimpleMap propsMap) {
//    propsMap.putDouble(key, mValue);
//  }
}
