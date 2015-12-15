package com.facebook.react.uimanager;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class AnimationsModule extends ReactContextBaseJavaModule {

  private static abstract class AnimatedNode {

    public abstract void update();
  }

  public AnimationsModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return "Animated";
  }

  @ReactMethod
  public void createAnimatedNode(String type, ReadableMap typeProps) {

  }
}
