/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.uimanager.animation;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.JavaOnlyMap;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class for all Animated.js library node types that can be created on the "native" side.
 */
/*package*/ class AnimatedNode {

  private static final int DEFAULT_ANIMATED_NODE_CHILD_COUNT = 1;
  /*package*/ static final int INITIAL_DFS_COLOR = 0;

  /*package*/ @Nullable List<AnimatedNode> mChildren; /* lazy-initialized when a child is added */
  /*package*/ int mActiveIncomingNodes = 0;
  /*package*/ int mDFSColor = INITIAL_DFS_COLOR;
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

  /**
   * Sub-classes may override this method to provide a custom handler for the event when the
   * node's parent has been updated.
   *
   * @param parent a reference to the node's "parent" in animated node graph that has just been
   *               updated
   */
  public void feedDataFromUpdatedParent(AnimatedNode parent) {
  }

  /**
   * This method will be run in animation loop once all the node's predecessor nodes have been
   * visited. This method can be overridden by subclasses to support use-cases when taking the input
   * from a single "parent" is not sufficient (e.g. addition node).
   *
   * @param frameTimeNanos frame time in nanoseconds provided to the android choreographer callback
   */
  public void runAnimationStep(long frameTimeNanos) {
  }

  /**
   * This method is called by {@link PropsAnimatedNode} in order to collect map of the properties
   * that needs to be updated in a view. Some nodes may override this method if they should map to
   * a more complex value structure than a simple numeric value (e.g. styles node, transform node).
   *
   * @param key key of the property which should be used as a key in the output property map
   * @param propsMap view property map that will be used in the view update. This method should
   *                 store its value in this map
   */
  public void saveInPropMap(String key, JavaOnlyMap propsMap) {
    propsMap.putDouble(key, mValue);
  }
}
