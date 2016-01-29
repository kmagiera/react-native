  /**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.uimanager.animation;

import android.util.SparseArray;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.OnBatchCompleteListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.GuardedChoreographerFrameCallback;
import com.facebook.react.uimanager.ReactChoreographer;
import com.facebook.react.uimanager.UIImplementation;
import com.facebook.react.uimanager.UIManagerModule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class NativeAnimatedModule extends BaseJavaModule implements
    OnBatchCompleteListener, LifecycleEventListener {

  private static class UpdateViewData {
    int mViewTag;
    ReadableMap mProps;

    public UpdateViewData(int tag, ReadableMap props) {
      mViewTag = tag;
      mProps = props;
    }
  }

  private final SparseArray<AnimatedNode> mAnimatedNodes = new SparseArray<>();
  private final ArrayList<AnimationDriver> mActiveAnimations = new ArrayList<>();
  private final ArrayList<UpdateViewData> mEnqueuedUpdates = new ArrayList<>();
  private final ArrayList<AnimatedNode> mUpdatedNodes = new ArrayList<>();
  private final AnimatedFrameCallback mAnimatedFrameCallback;

  public NativeAnimatedModule(ReactContext reactContext) {
    mAnimatedFrameCallback = new AnimatedFrameCallback(reactContext);
  }

  @Override
  public void onHostResume() {

  }

  @Override
  public void onHostPause() {

  }

  @Override
  public void onHostDestroy() {

  }

  @Override
  public String getName() {
    return "NativeAnimatedModule";
  }

  @Override
  public void onBatchComplete() {
    // Do stuff...
  }

  @ReactMethod
  public void createAnimatedNode(int tag, ReadableMap config) {
    if (mAnimatedNodes.get(tag) != null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + tag +
        " already exists");
    }
    String type = config.getString("type");
    final AnimatedNode node;
    if ("style".equals(type)) {
      node = new StyleAnimatedNode(config, this);
    } else if ("value".equals(type)) {
      node = new ValueAnimatedNode(config);
      mUpdatedNodes.add(node);
    } else if ("transform".equals(type)) {
      node = new TransformAnimatedNode(config, this);
    } else if ("interpolation".equals(type)) {
      node = new InterpolationAnimatedNode(config);
    } else if ("props".equals(type)) {
      node = new PropsAnimatedNode(config, this);
    } else if ("addition".equals(type)) {
      node = new AdditionAnimatedNode(config, this);
    } else if ("multiplication".equals(type)) {
      node = new MultiplicationAnimatedNode(config, this);
    } else {
      throw new JSApplicationIllegalArgumentException("Unsupported node type: " + type);
    }
    node.mTag = tag;
    mAnimatedNodes.put(tag, node);
  }

  @ReactMethod
  public void dropAnimatedNode(int tag) {
    mAnimatedNodes.remove(tag);
  }

  @ReactMethod
  public void setAnimatedNodeValue(int tag, double value) {
    AnimatedNode node = mAnimatedNodes.get(tag);
    if (node == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + tag +
        " does not exists");
    }
    node.mValue = value;
    mUpdatedNodes.add(node);
  }

  @ReactMethod
  public void startAnimatingNode(
    int animatedNodeTag,
    ReadableMap animationConfig,
    Callback endCallback) {
    AnimatedNode node = mAnimatedNodes.get(animatedNodeTag);
    if (node == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + animatedNodeTag +
        " does not exists");
    }
    if (!(node instanceof ValueAnimatedNode)) {
      throw new JSApplicationIllegalArgumentException("Animated node should be of type " +
        ValueAnimatedNode.class.getName());
    }
    String type = animationConfig.getString("type");
    final AnimationDriver animation;
    if ("frames".equals(type)) {
      animation = new FrameBasedAnimationDriver(animationConfig);
    } else if ("spring".equals(type)) {
      animation = new SpringAnimationDriver(animationConfig);
    } else {
      throw new JSApplicationIllegalArgumentException("Unsupported animation type: " + type);
    }
    animation.mEndCallback = endCallback;
    animation.mAnimatedValue = (ValueAnimatedNode) node;
    mActiveAnimations.add(animation);
  }

  @ReactMethod
  public void connectAnimatedNodes(int parentNodeTag, int childNodeTag) {
    AnimatedNode parentNode = mAnimatedNodes.get(parentNodeTag);
    if (parentNode == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + parentNodeTag +
        " does not exists");
    }
    AnimatedNode childNode = mAnimatedNodes.get(childNodeTag);
    if (childNode == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + childNodeTag +
        " does not exists");
    }
    parentNode.addChild(childNode);
  }

  @ReactMethod
  public void disconnectAnimatedNodes(int parentNodeTag, int childNodeTag) {
    AnimatedNode parentNode = mAnimatedNodes.get(parentNodeTag);
    if (parentNode == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + parentNodeTag +
        " does not exists");
    }
    AnimatedNode childNode = mAnimatedNodes.get(childNodeTag);
    if (childNode == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + childNodeTag +
        " does not exists");
    }
    parentNode.removeChild(childNode);
  }

  @ReactMethod
  public void connectAnimatedNodeToView(int animatedNodeTag, int viewTag) {
    AnimatedNode node = mAnimatedNodes.get(animatedNodeTag);
    if (node == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + animatedNodeTag +
        " does not exists");
    }
    if (!(node instanceof PropsAnimatedNode)) {
      throw new JSApplicationIllegalArgumentException("Animated node connected to view should be" +
        "of type " + PropsAnimatedNode.class.getName());
    }
    PropsAnimatedNode propsAnimatedNode = (PropsAnimatedNode) node;
    if (propsAnimatedNode.mConnectedViewTag != -1) {
      throw new JSApplicationIllegalArgumentException("ANimated node " + animatedNodeTag + " is " +
        "already attached to a view");
    }
    propsAnimatedNode.mConnectedViewTag = viewTag;
  }

  public void disconnectAnimatedNodeFromView(int animatedNodeTag, int viewTag) {
    AnimatedNode node = mAnimatedNodes.get(animatedNodeTag);
    if (node == null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + animatedNodeTag +
        " does not exists");
    }
    if (!(node instanceof PropsAnimatedNode)) {
      throw new JSApplicationIllegalArgumentException("Animated node connected to view should be" +
        "of type " + PropsAnimatedNode.class.getName());
    }
    PropsAnimatedNode propsAnimatedNode = (PropsAnimatedNode) node;
    if (propsAnimatedNode.mConnectedViewTag == viewTag) {
      propsAnimatedNode.mConnectedViewTag = -1;
    }
  }

  public void runAnimationStep(long frameTimeNanos) {
    /* prepare */
    for (int i = 0; i < mAnimatedNodes.size(); i++) {
      AnimatedNode node = mAnimatedNodes.valueAt(i);
      node.mEnqueued = false;
      node.mActiveIncomingNodes = 0;
    }

    Queue<AnimatedNode> nodesQueue = new ArrayDeque<>();
    for (int i = 0; i < mUpdatedNodes.size(); i++) {
      AnimatedNode node = mUpdatedNodes.get(i);
      if (!node.mEnqueued) {
        node.mEnqueued = true;
        nodesQueue.add(node);
      }
    }

    List<AnimationDriver> finishedAnimations = null; /* lazy allocate this */
    for (int i = 0; i < mActiveAnimations.size(); i++) {
      AnimationDriver animation = mActiveAnimations.get(i);
      animation.runAnimationStep(frameTimeNanos);
      AnimatedNode valueNode = animation.mAnimatedValue;
      if (!valueNode.mEnqueued) {
        valueNode.mEnqueued = true;
        nodesQueue.add(valueNode);
      }
      if (animation.mHasFinished) {
        if (finishedAnimations == null) {
          finishedAnimations = new ArrayList<>();
        }
        finishedAnimations.add(animation);
      }
    }

    while (!nodesQueue.isEmpty()) {
      AnimatedNode nextNode = nodesQueue.poll();
      if (nextNode.mChildren != null) {
        for (int i = 0; i < nextNode.mChildren.size(); i++) {
          AnimatedNode child = nextNode.mChildren.get(i);
          child.mActiveIncomingNodes++;
          if (!child.mEnqueued) {
            child.mEnqueued = true;
            nodesQueue.add(child);
          }
        }
      }
    }

    nodesQueue.clear();
    for (int i = 0; i < mAnimatedNodes.size(); i++) {
      AnimatedNode node = mAnimatedNodes.valueAt(i);
      if (node.mEnqueued && node.mActiveIncomingNodes == 0) {
        node.mEnqueued = true;
        nodesQueue.add(node);
      } else {
        node.mEnqueued = false;
      }
    }
    /* run animations steps on animated nodes graph starting with active animations */

    ArrayList<PropsAnimatedNode> updatedPropNodes = new ArrayList<>();
    while (!nodesQueue.isEmpty()) {
      AnimatedNode nextNode = nodesQueue.poll();
      nextNode.runAnimationStep(frameTimeNanos);
      if (nextNode instanceof PropsAnimatedNode) {
        updatedPropNodes.add((PropsAnimatedNode) nextNode);
      }
      if (nextNode.mChildren != null) {
        for (int i = 0; i < nextNode.mChildren.size(); i++) {
          AnimatedNode child = nextNode.mChildren.get(i);
          child.feedDataFromUpdatedParent(nextNode);
          child.mActiveIncomingNodes--;
          if (!child.mEnqueued && child.mActiveIncomingNodes == 0) {
            child.mEnqueued = true;
            nodesQueue.add(child);
          }
        }
      }
    }

    /* collect updates */
    mEnqueuedUpdates.clear();
    for (int i = 0; i < updatedPropNodes.size(); i++) {
      PropsAnimatedNode propNode = updatedPropNodes.get(i);
      UpdateViewData data = propNode.createUpdateViewData();
      if (data.mViewTag > 0) {
        mEnqueuedUpdates.add(propNode.createUpdateViewData());
      }
    }

    /* cleanup finished animations */
    if (finishedAnimations != null && !finishedAnimations.isEmpty()) {
      for (int i = 0; i < finishedAnimations.size(); i++) {
        // TODO: do in O(1);
        AnimationDriver finishedAnimation = finishedAnimations.get(i);
        mActiveAnimations.remove(finishedAnimation);
        WritableMap endCallbackResponse = Arguments.createMap();
        endCallbackResponse.putBoolean("finished", true);
        finishedAnimation.mEndCallback.invoke(endCallbackResponse);
      }
    }
  }

  public void runUpdates(UIImplementation uiImplementation) {
    // Assert on native thread
    runAnimationStep(mLastFrameTimeNanos.get());
    for (int i = 0; i < mEnqueuedUpdates.size(); i++) {
      UpdateViewData data = mEnqueuedUpdates.get(i);
//      Log.e("CAT", "Update View " + data.mViewTag + ", " + data.mProps);
      uiImplementation.updateView(data.mViewTag, null, data.mProps);
    }
  }

  public void resumeFrameCallback() {
    ReactChoreographer.getInstance().postFrameCallback(
      ReactChoreographer.CallbackType.ANIMATIONS,
      mAnimatedFrameCallback);
  }

  public void pauseFrameCallback() {
    ReactChoreographer.getInstance().removeFrameCallback(
      ReactChoreographer.CallbackType.ANIMATIONS,
      mAnimatedFrameCallback);
  }

  private AtomicLong mLastFrameTimeNanos = new AtomicLong(0);

  private class AnimatedFrameCallback extends GuardedChoreographerFrameCallback {

    private final ReactContext mReactContext;

    protected AnimatedFrameCallback(ReactContext reactContext) {
      super(reactContext);
      mReactContext = reactContext;
    }

    @Override
    protected void doFrameGuarded(final long frameTimeNanos) {
      // It's too late for enqueueing UI updates for this frame
      mLastFrameTimeNanos.set(frameTimeNanos);
      // Enqueue runAnimationStep, this should ideally run on a separate thread
      mReactContext.runOnNativeModulesQueueThread(new Runnable() {
        @Override
        public void run() {
          mReactContext.getNativeModule(UIManagerModule.class).dispatchViewUpdatesIfNotInJSBatch();
        }
      });
      ReactChoreographer.getInstance().postFrameCallback(
        ReactChoreographer.CallbackType.ANIMATIONS,
        this);
    }
  }
}
