/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.ReactStylesDiffMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Animated node that represents view properties. There is a special handling logic implemented for
 * the nodes of this type in {@link NativeAnimatedNodesManager} that is responsible for extracting
 * a map of updated properties, which can be then passed down to the view.
 */
/*package*/ class PropsAnimatedNode extends AnimatedNode {

  /*package*/ int mConnectedViewTag = -1;
  private final NativeAnimatedNodesManager mNativeAnimatedNodesManager;
  private final Map<String, Integer> mPropMapping;

  PropsAnimatedNode(ReadableMap config, NativeAnimatedNodesManager nativeAnimatedNodesManager) {
    ReadableMap props = config.getMap("props");
    ReadableMapKeySetIterator iter = props.keySetIterator();
    mPropMapping = new HashMap<>();
    while (iter.hasNextKey()) {
      String propKey = iter.nextKey();
      int nodeIndex = props.getInt(propKey);
      mPropMapping.put(propKey, nodeIndex);
    }
    mNativeAnimatedNodesManager = nativeAnimatedNodesManager;
  }

  public final void updateView(NativeViewHierarchyManager nativeViewHierarchyManager) {
    if (mConnectedViewTag == -1) {
      throw new IllegalStateException("Node has not been attached to a view");
    }
    JavaOnlyMap propsMap = new JavaOnlyMap();
    for (Map.Entry<String, Integer> entry : mPropMapping.entrySet()) {
      AnimatedNode node = mNativeAnimatedNodesManager.getNodeById(entry.getValue());
      if (node != null) {
        node.saveInPropMap(entry.getKey(), propsMap);
      } else {
        throw new IllegalArgumentException("Mapped style node does not exists");
      }
    }
    nativeViewHierarchyManager.updateProperties(
      mConnectedViewTag,
      new ReactStylesDiffMap(propsMap));
  }
}
