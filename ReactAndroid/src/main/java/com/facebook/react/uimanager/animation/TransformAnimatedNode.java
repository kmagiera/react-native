package com.facebook.react.uimanager.animation;

import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;

import java.util.HashMap;
import java.util.Map;

/**
 */
/*package*/ class TransformAnimatedNode extends AnimatedNode {

  private final NativeAnimatedModule mNativeAnimatedModule;
  private final Map<String, Integer> mPropMapping;
  private final Map<String, Object> mStaticProps;

  TransformAnimatedNode(ReadableMap config, NativeAnimatedModule nativeAnimatedModule) {
    ReadableMap transforms = config.getMap("animated");
    ReadableMapKeySetIterator iter = transforms.keySetIterator();
    mPropMapping = new HashMap<>();
    while (iter.hasNextKey()) {
      String propKey = iter.nextKey();
      int nodeIndex = transforms.getInt(propKey);
      mPropMapping.put(propKey, nodeIndex);
    }
    ReadableMap statics = config.getMap("statics");
    iter = statics.keySetIterator();
    mStaticProps = new HashMap<>();
    while (iter.hasNextKey()) {
      String propKey = iter.nextKey();
      ReadableType type = statics.getType(propKey);
      switch (type) {
        case Number:
          mStaticProps.put(propKey, statics.getDouble(propKey));
          break;
        case Array:
          mStaticProps.put(propKey, statics.getArray(propKey));
          break;
      }
    }
    this.mNativeAnimatedModule = nativeAnimatedModule;
  }

  @Override
  public void saveInPropMap(String key, JavaOnlyMap propsMap) {
  /* ignore key, style names are flattened */
    JavaOnlyMap transformMap = new JavaOnlyMap();
    for (String propKey : mPropMapping.keySet()) {
      // TODO: use entryset = optimize
      int nodeIndex = mPropMapping.get(propKey);
      AnimatedNode node = mNativeAnimatedModule.mAnimatedNodes.get(nodeIndex);
      if (node != null) {
        node.saveInPropMap(propKey, transformMap);
      } else {
        throw new IllegalArgumentException("Mapped style node does not exists");
      }
    }
    for (String propKey : mStaticProps.keySet()) {
      // TODO: use entryset = optimize
      Object value = mStaticProps.get(propKey);
      if (value instanceof Double) {
        transformMap.putDouble(propKey, (Double) value);
      } else if (value instanceof WritableArray) {
        transformMap.putArray(propKey, (WritableArray) value);
      }
    }
    propsMap.putMap("decomposedMatrix", transformMap);
  }
}
