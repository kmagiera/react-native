package com.facebook.react.animated;


import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;

import java.util.HashMap;
import java.util.Map;

/*package*/ class TransformAnimatedNode extends AnimatedNode {

  private static JavaOnlyArray copyArrayOfNumbers(ReadableArray ary) {
    int size = ary.size();
    JavaOnlyArray result = new JavaOnlyArray();
    for (int i = 0; i < size; i++) {
      ReadableType type = ary.getType(i);
      switch (type) {
        case Number:
          result.pushDouble(ary.getDouble(i));
          break;
        default:
          throw new IllegalArgumentException("element type not supported: " + type);
      }
    }
    return result;
  }

  private final NativeAnimatedNodesManager mNativeAnimatedNodesManager;
  private final Map<String, Integer> mPropMapping;
  private final Map<String, Object> mStaticProps;

  TransformAnimatedNode(ReadableMap config, NativeAnimatedNodesManager nativeAnimatedNodesManager) {
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
          mStaticProps.put(propKey, copyArrayOfNumbers(statics.getArray(propKey)));
          break;
      }
    }
    mNativeAnimatedNodesManager = nativeAnimatedNodesManager;
  }

  public void collectViewUpdates(JavaOnlyMap propsMap) {
    JavaOnlyMap transformMap = new JavaOnlyMap();
    for (Map.Entry<String, Integer> entry : mPropMapping.entrySet()) {
      AnimatedNode node = mNativeAnimatedNodesManager.getNodeById(entry.getValue());
      if (node == null) {
        throw new IllegalArgumentException("Mapped style node does not exists");
      } else if (node instanceof ValueAnimatedNode) {
        transformMap.putDouble(entry.getKey(), ((ValueAnimatedNode) node).mValue);
      } else {
        throw new IllegalArgumentException("Unsupported type of node used as a transform child " +
          "node " + node.getClass());
      }
    }
    for (Map.Entry<String, Object> entry : mStaticProps.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Double) {
        transformMap.putDouble(entry.getKey(), (Double) value);
      } else if (value instanceof WritableArray) {
        transformMap.putArray(entry.getKey(), (WritableArray) value);
      }
    }
    propsMap.putMap("decomposedMatrix", transformMap);
  }
}
