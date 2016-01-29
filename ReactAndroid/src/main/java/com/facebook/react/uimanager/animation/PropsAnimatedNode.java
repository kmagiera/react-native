package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import java.util.HashMap;
import java.util.Map;

/**
 */
/*package*/ class PropsAnimatedNode extends AnimatedNode {

  /*package*/ int mConnectedViewTag = -1;
  private final NativeAnimatedModule mNativeAnimatedModule;
  private final Map<String, Integer> mPropMapping;

  PropsAnimatedNode(ReadableMap config, NativeAnimatedModule nativeAnimatedModule) {
    ReadableMap props = config.getMap("props");
    ReadableMapKeySetIterator iter = props.keySetIterator();
    mPropMapping = new HashMap<>();
    while (iter.hasNextKey()) {
      String propKey = iter.nextKey();
      int nodeIndex = props.getInt(propKey);
      mPropMapping.put(propKey, nodeIndex);
    }
    mNativeAnimatedModule = nativeAnimatedModule;
  }

  public NativeAnimatedModule.UpdateViewData createUpdateViewData() {
    SimpleMap propsMap = new SimpleMap();
    for (String propKey : mPropMapping.keySet()) {
      // TODO: use entryset = optimize
      int nodeIndex = mPropMapping.get(propKey);
      AnimatedNode node = mNativeAnimatedModule.mAnimatedNodes.get(nodeIndex);
      if (node != null) {
        node.saveInPropMap(propKey, propsMap);
      } else {
        throw new IllegalArgumentException("Mapped style node does not exists");
      }
    }
    return new NativeAnimatedModule.UpdateViewData(mConnectedViewTag, propsMap);
  }
}
