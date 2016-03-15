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
 * Native counterpart of style animated node (see AnimatedStyle class in AnimatedImplementation.js)
 */
/*package*/ class StyleAnimatedNode extends AnimatedNode {

  private final NativeAnimatedNodesManager mNativeAnimatedNodesManager;
  private final Map<String, Integer> mPropMapping;

  StyleAnimatedNode(ReadableMap config, NativeAnimatedNodesManager nativeAnimatedNodesManager) {
    ReadableMap style = config.getMap("style");
    ReadableMapKeySetIterator iter = style.keySetIterator();
    mPropMapping = new HashMap<>();
    while (iter.hasNextKey()) {
      String propKey = iter.nextKey();
      int nodeIndex = style.getInt(propKey);
      mPropMapping.put(propKey, nodeIndex);
    }
    mNativeAnimatedNodesManager = nativeAnimatedNodesManager;
  }

  @Override
  public void saveInPropMap(String key, JavaOnlyMap propsMap) {
  /* ignore key, style names are flattened */
    for (Map.Entry<String, Integer> entry : mPropMapping.entrySet()) {
      int nodeIndex = entry.getValue();
      AnimatedNode node = mNativeAnimatedNodesManager.getNodeById(nodeIndex);
      if (node != null) {
        node.saveInPropMap(entry.getKey(), propsMap);
      } else {
        throw new IllegalArgumentException("Mapped style node does not exists");
      }
    }
  }
}
