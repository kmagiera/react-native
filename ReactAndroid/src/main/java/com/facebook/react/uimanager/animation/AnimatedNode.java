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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class for all Animated.js library node types that can be created on the "native" side.
 */
/*package*/ abstract class AnimatedNode {

  public static final int INITIAL_BFS_COLOR = 0;

  private static final int DEFAULT_ANIMATED_NODE_CHILD_COUNT = 1;

  /*package*/ @Nullable List<AnimatedNode> mChildren; /* lazy-initialized when a child is added */
  /*package*/ int mActiveIncomingNodes = 0;
  /*package*/ int mBFSColor = INITIAL_BFS_COLOR;
  /*package*/ int mTag = -1;

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
   * This method will be run in animation loop only once for each node. It will be executed on a
   * node only when all the node's parent has already been updated. Therefore it can be used to
   * calculate node's value as a product of the parent's values.
   *
   * Parameter {@param lastUpdatedParent} points to the node's parent that have just been updated
   * and it's update has triggered the current node update. Note that the method may not be called
   * for the nodes that have been updated by {@code setValue} call form JS. In which case their
   * state will be updated in a given animation loop, but not as a result of their parent being
   * updated. So in order to avoid their state being updated twice animation loop will not call
   * {@link #update} on those nodes. Similarily for the nodes that are directly hooked into
   * instances of {@link AnimationDriver}.
   *
   * @param lastUpdatedParent reference to a parent node of this node in animated graph that has
   *                          just been updated and triggered the update of current node
   * @param frameTimeNanos frame time in nanoseconds provided to the android choreographer callback
   */
  public void update(AnimatedNode lastUpdatedParent, long frameTimeNanos) {
  }
}
