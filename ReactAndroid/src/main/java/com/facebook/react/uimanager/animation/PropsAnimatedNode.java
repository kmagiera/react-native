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

  public UpdateViewData createUpdateViewData() {
    JavaOnlyMap propsMap = new JavaOnlyMap();
    for (String propKey : mPropMapping.keySet()) {
      // TODO: use entryset = optimize
      int nodeIndex = mPropMapping.get(propKey);
      AnimatedNode node = mNativeAnimatedNodesManager.getNodeById(nodeIndex);
      if (node != null) {
        node.saveInPropMap(propKey, propsMap);
      } else {
        throw new IllegalArgumentException("Mapped style node does not exists");
      }
    }
    return new UpdateViewData(mConnectedViewTag, propsMap);
  }
}
