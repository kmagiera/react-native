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

  import com.facebook.react.bridge.BaseJavaModule;
  import com.facebook.react.bridge.Callback;
  import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
  import com.facebook.react.bridge.LifecycleEventListener;
  import com.facebook.react.bridge.ReactApplicationContext;
  import com.facebook.react.bridge.ReactContext;
  import com.facebook.react.bridge.ReactMethod;
  import com.facebook.react.bridge.ReadableMap;
  import com.facebook.react.uimanager.GuardedChoreographerFrameCallback;
  import com.facebook.react.uimanager.ReactChoreographer;
  import com.facebook.react.uimanager.UIImplementation;
  import com.facebook.systrace.Systrace;
  import com.facebook.systrace.SystraceMessage;

  import java.util.ArrayList;
  import java.util.concurrent.atomic.AtomicLong;

  import javax.annotation.concurrent.GuardedBy;

  public class NativeAnimatedModule extends BaseJavaModule implements LifecycleEventListener {

  private interface UIThreadOperation {
    void execute(NativeAnimatedNodesManager animatedNodesManager);
  }

  private final GuardedChoreographerFrameCallback mAnimatedFrameCallback;
  private final Object mOperationsCopyLock = new Object();
  private ArrayList<UIThreadOperation> mOperations = new ArrayList<>();
  private volatile boolean mAnimatedFrameCallbackEnqueued = false;
  private volatile @Nullable ArrayList<UIThreadOperation> mReadyOperations = null;

  /*
   * Nodes manager will only be accessed from the UI Thread
   */
  private final NativeAnimatedNodesManager mNodesManager;

  public NativeAnimatedModule(ReactContext reactContext, final UIImplementation uiImplementation) {
    mNodesManager = new NativeAnimatedNodesManager();
    mAnimatedFrameCallback = new GuardedChoreographerFrameCallback(reactContext) {
      @Override
      protected void doFrameGuarded(final long frameTimeNanos) {
        mAnimatedFrameCallbackEnqueued = false;

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
        mNodesManager.runUpdates(uiImplementation, frameTimeNanos);

        enqueueFrameCallbackIfNeeded();
      }
    };
  }

  public void dispatchUpdates() {
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
    enqueueFrameCallbackIfNeeded();
  }

  @Override
  public void onHostResume() {
    enqueueFrameCallbackIfNeeded();
  }

  @Override
  public void onHostPause() {
    clearFrameCallback();
  }

  @Override
  public void onHostDestroy() {
    clearFrameCallback();
  }

  @Override
  public String getName() {
    return "NativeAnimatedModule";
  }

  private void clearFrameCallback() {
    ReactChoreographer.getInstance().removeFrameCallback(
      ReactChoreographer.CallbackType.ANIMATIONS,
      mAnimatedFrameCallback);
  }

  private void enqueueFrameCallbackIfNeeded() {
    if (!mAnimatedFrameCallbackEnqueued &&
        (mNodesManager.hasActiveAnimations() || mReadyOperations != null)) {
      mAnimatedFrameCallbackEnqueued = true;
      ReactChoreographer.getInstance().postFrameCallback(
        ReactChoreographer.CallbackType.ANIMATIONS,
        mAnimatedFrameCallback);
    }
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
