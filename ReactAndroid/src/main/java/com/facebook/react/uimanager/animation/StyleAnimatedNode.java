package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.util.HashMap;
import java.util.Map;

/**
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
  }
}
