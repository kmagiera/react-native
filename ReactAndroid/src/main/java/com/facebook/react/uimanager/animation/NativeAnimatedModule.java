/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.uimanager.animation;

import android.support.annotation.Nullable;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.OnBatchCompleteListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.GuardedChoreographerFrameCallback;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.ReactChoreographer;
import com.facebook.react.uimanager.UIImplementation;
import com.facebook.react.uimanager.UIManagerModule;

import java.util.ArrayList;

/**
 * Module that exposes interface for creating and managing animated nodes on the "native" side.
 */
public class NativeAnimatedModule extends ReactContextBaseJavaModule implements
    OnBatchCompleteListener, LifecycleEventListener {

  private interface UIThreadOperation {
    void execute(NativeAnimatedNodesManager animatedNodesManager);
  }

  private final Object mOperationsCopyLock = new Object();
  private @Nullable GuardedChoreographerFrameCallback mAnimatedFrameCallback;
  private @Nullable ReactChoreographer mReactChoreographer;
  private ArrayList<UIThreadOperation> mOperations = new ArrayList<>();
  private volatile @Nullable ArrayList<UIThreadOperation> mReadyOperations = null;

  // Nodes manager will only be accessed from the UI Thread
  private final NativeAnimatedNodesManager mNodesManager = new NativeAnimatedNodesManager();

  public NativeAnimatedModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public void initialize() {
    // Safe to acquire choreographer here, as initialize() is invoked from UI thread.
    mReactChoreographer = ReactChoreographer.getInstance();

    ReactApplicationContext reactCtx = getReactApplicationContext();
    final UIImplementation uiImplementation =
      reactCtx.getNativeModule(UIManagerModule.class).getUIImplementation();
    mAnimatedFrameCallback = new GuardedChoreographerFrameCallback(reactCtx) {
      @Override
      protected void doFrameGuarded(final long frameTimeNanos) {

        ArrayList<UIThreadOperation> operations;
        synchronized (mOperationsCopyLock) {
          operations = mReadyOperations;
          mReadyOperations = null;
        }

        if (operations != null) {
          for (int i = 0, size = operations.size(); i < size; i++) {
            operations.get(i).execute(mNodesManager);
          }
        }

        if (mNodesManager.hasActiveAnimations()) {
          mNodesManager.runUpdates(uiImplementation, frameTimeNanos);
        }

        Assertions.assertNotNull(mReactChoreographer).postFrameCallback(
          ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE,
          mAnimatedFrameCallback);
      }
    };
    reactCtx.addLifecycleEventListener(this);
  }

  @Override
  public void onBatchComplete() {
    // Note: The order of executing onBatchComplete handler (especially in terms of onBatchComplete
    // from the UIManagerModule) doesn't matter as we only enqueue operations for the UI thread to
    // be executed from here. Thanks to ReactChoreographer all the operations from here are going
    // to be executed *after* all the operations enqueued by UIManager as the callback type that we
    // use for ReactChoreographer (CallbackType.NATIVE_ANIMATED_MODULE) is run after callbacks that UIManager
    // use
    ArrayList<UIThreadOperation> operations = mOperations.isEmpty() ? null : mOperations;
    if (operations != null) {
      mOperations = new ArrayList<>();
      synchronized (mOperationsCopyLock) {
        if (mReadyOperations == null) {
          mReadyOperations = operations;
        } else {
          mReadyOperations.addAll(operations);
        }
      }
    }
  }

  @Override
  public void onHostResume() {
    enqueueFrameCallback();
  }

  @Override
  public void onHostPause() {
    clearFrameCallback();
  }

  @Override
  public void onHostDestroy() {
    // do nothing
  }

  @Override
  public String getName() {
    return "NativeAnimatedModule";
  }

  private void clearFrameCallback() {
    Assertions.assertNotNull(mReactChoreographer).removeFrameCallback(
      ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE,
      mAnimatedFrameCallback);
  }

  private void enqueueFrameCallback() {
    Assertions.assertNotNull(mReactChoreographer).postFrameCallback(
      ReactChoreographer.CallbackType.NATIVE_ANIMATED_MODULE,
      mAnimatedFrameCallback);
  }

  @ReactMethod
  public void createAnimatedNode(final int tag, final ReadableMap config) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.createAnimatedNode(tag, config);
      }
    });
  }

  @ReactMethod
  public void dropAnimatedNode(final int tag) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.dropAnimatedNode(tag);
      }
    });
  }

  @ReactMethod
  public void setAnimatedNodeValue(final int tag, final double value) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.setAnimatedNodeValue(tag, value);
      }
    });
  }

  @ReactMethod
  public void startAnimatingNode(
      final int animatedNodeTag,
      final ReadableMap animationConfig,
      final Callback endCallback) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.startAnimatingNode(
          animatedNodeTag,
          animationConfig,
          endCallback);
      }
    });
  }

  @ReactMethod
  public void connectAnimatedNodes(final int parentNodeTag, final int childNodeTag) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.connectAnimatedNodes(parentNodeTag, childNodeTag);
      }
    });
  }

  @ReactMethod
  public void disconnectAnimatedNodes(final int parentNodeTag, final int childNodeTag) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.disconnectAnimatedNodes(parentNodeTag, childNodeTag);
      }
    });
  }

  @ReactMethod
  public void connectAnimatedNodeToView(final int animatedNodeTag, final int viewTag) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.connectAnimatedNodeToView(animatedNodeTag, viewTag);
      }
    });
  }

  @ReactMethod
  public void disconnectAnimatedNodeFromView(final int animatedNodeTag, final int viewTag) {
    mOperations.add(new UIThreadOperation() {
      @Override
      public void execute(NativeAnimatedNodesManager animatedNodesManager) {
        animatedNodesManager.disconnectAnimatedNodeFromView(animatedNodeTag, viewTag);
      }
    });
  }
}
