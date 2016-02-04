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
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.GuardedChoreographerFrameCallback;
import com.facebook.react.uimanager.ReactChoreographer;
import com.facebook.react.uimanager.UIImplementation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class NativeAnimatedModule extends BaseJavaModule implements LifecycleEventListener {

  private static class UpdateViewData {
    int mViewTag;
    ReadableMap mProps;

    public UpdateViewData(int tag, ReadableMap props) {
      mViewTag = tag;
      mProps = props;
    }
  }

  private final Object mAnimatedGraphMonitor = new Object();

  private final SparseArray<AnimatedNode> mAnimatedNodes = new SparseArray<>();
  private final ArrayList<AnimationDriver> mActiveAnimations = new ArrayList<>();
  private final ArrayList<UpdateViewData> mEnqueuedUpdates = new ArrayList<>();
  private final ArrayList<AnimatedNode> mUpdatedNodes = new ArrayList<>();
  private final AtomicLong mLastFrameTimeNanos = new AtomicLong(0);
  private final GuardedChoreographerFrameCallback mAnimatedFrameCallback;
  private boolean mAnimatedFrameCallbackEnqueued = false;
  private int mAnimatedGraphDFSColor = 0;

  public NativeAnimatedModule(final ReactContext reactContext) {
    mAnimatedFrameCallback = new GuardedChoreographerFrameCallback(reactContext) {
      @Override
      protected void doFrameGuarded(final long frameTimeNanos) {
        mAnimatedFrameCallbackEnqueued = false;
        // It's too late for enqueueing UI updates for this frame
        mLastFrameTimeNanos.set(frameTimeNanos);
        // Enqueue runAnimationStep, this should ideally run on a separate thread
//        mReactContext.runOnNativeModulesQueueThread(new Runnable() {
//          @Override
//          public void run() {
//            mReactContext.getNativeModule(UIManagerModule.class).dispatchViewUpdatesIfNotInJSBatch();
//          }
//        });

        enqueueFrameCallbackIfNeeded();
      }
    };
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
    if (!mAnimatedFrameCallbackEnqueued && hasActiveAnimations()) {
      mAnimatedFrameCallbackEnqueued = true;
      ReactChoreographer.getInstance().postFrameCallback(
        ReactChoreographer.CallbackType.ANIMATIONS,
        mAnimatedFrameCallback);
    }
  }

  private boolean hasActiveAnimations() {
    synchronized (mAnimatedGraphMonitor) {
      return !mActiveAnimations.isEmpty() || !mEnqueuedUpdates.isEmpty();
    }
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

  @ReactMethod
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

  /**
   * Animation loop performs two DFSes over the graph of animated nodes. We use incremented
   * {@code mAnimatedGraphDFSColor} to mark nodes as visited in each of the DFSes which saves
   * additional loops for clearing "visited" states.
   *
   * First DFS starts with nodes that are in {@code mUpdatedNodes} or directly attached to an active
   * animation (hence linked to objects from {@code mActiveAnimations}). In that step we calculate
   * an attribute {@code mActiveIncomingNodes}. The second DFS runs in topological order over the
   * subgraph of *active* nodes. This is done by adding node to the DFS queue only if all its
   * "predecessors" have already been visited.
   */
  private void runAnimationStep(long frameTimeNanos) {
    synchronized (mAnimatedGraphMonitor) {
      runAnimationStepSynchronized(frameTimeNanos);
    }
  }

  private void runAnimationStepSynchronized(long frameTimeNanos) {
    int activeNodesCount = 0;
    int updatedNodesCount = 0;
    boolean hasFinishedAnimations = false;

    // STEP 1.
    // DFS over graph of nodes starting from ones from `mUpdatedNodes` and ones that are attached to
    // active animations (from `mActiveANimations`. Update `mIncomingNodes` attribute for each node
    // during that DFS. Store number of visited nodes in `activeNodesCount`. We "execute" active
    // animations as a part of this step.

    mAnimatedGraphDFSColor++; /* use new color */
    if (mAnimatedGraphDFSColor == AnimatedNode.INITIAL_DFS_COLOR) {
      // value "0" is used as an initial color for a new node, using it in DFS may cause some nodes
      // to be skipped.
      mAnimatedGraphDFSColor++;
    }

    Queue<AnimatedNode> nodesQueue = new ArrayDeque<>();
    for (int i = 0; i < mUpdatedNodes.size(); i++) {
      AnimatedNode node = mUpdatedNodes.get(i);
      if (node.mDFSColor != mAnimatedGraphDFSColor) {
        node.mDFSColor = mAnimatedGraphDFSColor;
        activeNodesCount++;
        nodesQueue.add(node);
      }
    }

    for (int i = 0; i < mActiveAnimations.size(); i++) {
      AnimationDriver animation = mActiveAnimations.get(i);
      animation.runAnimationStep(frameTimeNanos);
      AnimatedNode valueNode = animation.mAnimatedValue;
      if (valueNode.mDFSColor != mAnimatedGraphDFSColor) {
        valueNode.mDFSColor = mAnimatedGraphDFSColor;
        activeNodesCount++;
        nodesQueue.add(valueNode);
      }
      if (animation.mHasFinished) {
        hasFinishedAnimations = true;
      }
    }

    while (!nodesQueue.isEmpty()) {
      AnimatedNode nextNode = nodesQueue.poll();
      if (nextNode.mChildren != null) {
        for (int i = 0; i < nextNode.mChildren.size(); i++) {
          AnimatedNode child = nextNode.mChildren.get(i);
          child.mActiveIncomingNodes++;
          if (child.mDFSColor != mAnimatedGraphDFSColor) {
            child.mDFSColor = mAnimatedGraphDFSColor;
            activeNodesCount++;
            nodesQueue.add(child);
          }
        }
      }
    }

    // STEP 2
    // DFS over the graph of active nodes in topological order -> visit node only when its all
    // "predecessors" in the graph have already been visited. It is important to visit nodes in that
    // order as they may often use values of their predecessors in order to calculate "next state"
    // of their own. We start by determining the starting set of nodes by looking for nodes with
    // `mActiveIncomingNodes = 0` (those can only be the ones that we start DFS in the previous
    // step). We store number of visited nodes in this step in `updatedNodesCount`

    nodesQueue.clear();
    mAnimatedGraphDFSColor++;
    if (mAnimatedGraphDFSColor == AnimatedNode.INITIAL_DFS_COLOR) {
      // see reasoning for this check a few lines above
      mAnimatedGraphDFSColor++;
    }

    // find nodes with zero "incoming nodes", those can be either nodes from `mUpdatedNodes` or
    // ones connected to active animations
    for (int i = 0; i < mUpdatedNodes.size(); i++) {
      AnimatedNode node = mUpdatedNodes.get(i);
      if (node.mActiveIncomingNodes == 0 && node.mDFSColor != mAnimatedGraphDFSColor) {
        node.mDFSColor = mAnimatedGraphDFSColor;
        updatedNodesCount++;
        nodesQueue.add(node);
      }
    }
    for (int i = 0; i < mActiveAnimations.size(); i++) {
      AnimationDriver animation = mActiveAnimations.get(i);
      AnimatedNode valueNode = animation.mAnimatedValue;
      if (valueNode.mActiveIncomingNodes == 0 && valueNode.mDFSColor != mAnimatedGraphDFSColor) {
        valueNode.mDFSColor = mAnimatedGraphDFSColor;
        updatedNodesCount++;
        nodesQueue.add(valueNode);
      }
    }

    // Run main "update" loop
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
          if (child.mDFSColor != mAnimatedGraphDFSColor && child.mActiveIncomingNodes == 0) {
            child.mDFSColor = mAnimatedGraphDFSColor;
            updatedNodesCount++;
            nodesQueue.add(child);
          }
        }
      }
    }

    // Verify that we've visited *all* active nodes. Throw otherwise as this would mean there is a
    // cycle in animated node graph. We also take advantage of the fact that all active nodes are
    // visited in the step above so that all the nodes properties `mActiveIncomingNodes` are set to
    // zero
    if (activeNodesCount != updatedNodesCount) {
      throw new IllegalStateException("Looks like animated nodes graph has cycles, there are "
        + activeNodesCount + " but toposort visited only " + updatedNodesCount);
    }

    // Collect UI updates
    for (int i = 0; i < updatedPropNodes.size(); i++) {
      PropsAnimatedNode propNode = updatedPropNodes.get(i);
      UpdateViewData data = propNode.createUpdateViewData();
      if (data.mViewTag > 0) {
        mEnqueuedUpdates.add(propNode.createUpdateViewData());
      }
    }

    // Cleanup finished animations. Iterate over the array of animations and override ones that has
    // finished, then resize `mActiveAnimations`.
    if (hasFinishedAnimations) {
      int dest = 0;
      for (int i = 0; i < mActiveAnimations.size(); i++) {
        AnimationDriver animation = mActiveAnimations.get(i);
        if (!animation.mHasFinished) {
          mActiveAnimations.set(dest++, animation);
        } else {
          WritableMap endCallbackResponse = Arguments.createMap();
          endCallbackResponse.putBoolean("finished", true);
          animation.mEndCallback.invoke(endCallbackResponse);
        }
      }
      for (int i = mActiveAnimations.size(); i >= dest; i--) {
        mActiveAnimations.remove(i);
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
    mEnqueuedUpdates.clear();
  }
}
