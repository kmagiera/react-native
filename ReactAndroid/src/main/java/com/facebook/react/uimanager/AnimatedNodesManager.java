package com.facebook.react.uimanager;

import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
/*package*/ class AnimatedNodesManager {

  private static final int DEFAULT_ANIMATED_NODE_CHILD_COUNT = 1;

  private static class AnimatedNode {
    private @Nullable List<AnimatedNode> mChildren; /* lazy-initialized when child is added */
    private int mVisitedIncommingNodes = 0;
    private int mIncommingNodes = 0;

    public void addChild(AnimatedNode child) {
      if (mChildren == null) {
        mChildren = new ArrayList<>(DEFAULT_ANIMATED_NODE_CHILD_COUNT);
      }
      child.mIncommingNodes++;
      Assertions.assertNotNull(mChildren).add(child);
    }

    public final void prepareAnimationStep() {
      mVisitedIncommingNodes = 0;
    }

    public void feedDataFromUpdatedParent(AnimatedNode parent) {
    }

    public void runAnimationStep(long frameTimeNanos) {
    }
  }

  private static class StyleAnimatedNode extends AnimatedNode {
  }

  private static class ValueAnimatedNode extends AnimatedNode {
  }

  private static class PropsAnimatedNode extends AnimatedNode {
    private int mConnectedViewTag = -1;

    public void setConnectedView(int viewTag) {
      mConnectedViewTag = viewTag;
    }

    public UpdateViewData createUpdateViewData() {
      return null;
    }
  }

  private static class InterpolationAnimatedNode extends AnimatedNode {

    InterpolationAnimatedNode(ReadableMap config) {

    }
  }

  private static class AnimationDriver extends AnimatedNode {
  }

  private static class FrameBasedAnimation extends AnimationDriver {

    private int lastFrameIndex = -1;
    private long lastFrameTime = 0;
    private double[] mFrames;

    FrameBasedAnimation(ReadableMap config) {
      ReadableArray frames = config.getArray("frames");
      int numberOfFrames = frames.size();
      mFrames = new double[numberOfFrames];
      for (int i = 0; i < numberOfFrames; i++) {
        mFrames[i] = frames.getDouble(i);
      }
    }

    @Override
    public void runAnimationStep(long frameTimeNanos) {
      super.runAnimationStep(frameTimeNanos);
    }
  }

  private static class UpdateViewData {
    private int mViewTag;
    private ReadableMap mProps;

    public UpdateViewData(int tag, ReadableMap props) {
      mViewTag = tag;
      mProps = props;
    }
  }

  private final SparseArray<AnimatedNode> mAnimatedNodes = new SparseArray<>();
  private final ArrayList<AnimationDriver> mActiveAnimations = new ArrayList<>();
  private final ArrayList<UpdateViewData> mEnqueuedUpdates = new ArrayList<>();
  private final AnimatedFrameCallback mAnimatedFrameCallback;

  public AnimatedNodesManager(ReactContext reactContext) {
    mAnimatedFrameCallback = new AnimatedFrameCallback(reactContext);
  }

  public void createAnimatedNode(int tag, ReadableMap config) {
    if (mAnimatedNodes.get(tag) != null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + tag +
              " already exists");
    }
    String type = config.getString("type");
    final AnimatedNode node;
    if ("style".equals(type)) {
      node = new StyleAnimatedNode();
    } else if ("value".equals(type)) {
      node = new ValueAnimatedNode();
    } else if ("interpolation".equals(type)) {
      node = new InterpolationAnimatedNode(config);
    } else {
      throw new JSApplicationIllegalArgumentException("Unsupported node type: " + type);
    }
    mAnimatedNodes.put(tag, node);
  }

  public void startAnimatingNode(int animatedNodeTag, ReadableMap animationConfig) {
    AnimatedNode node = mAnimatedNodes.get(animatedNodeTag);
    if (node != null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + animatedNodeTag +
              " does not exists");
    }
    String type = animationConfig.getString("type");
    final AnimationDriver animation;
    if ("frames".equals(type)) {
      animation = new FrameBasedAnimation(animationConfig);
    } else {
      throw new JSApplicationIllegalArgumentException("Unsupported animation type: " + type);
    }
    animation.addChild(node);
    mActiveAnimations.add(animation);
  }

  public void connectAnimatedNodes(int parentNodeTag, int childNodeTag) {
    AnimatedNode parentNode = mAnimatedNodes.get(parentNodeTag);
    if (parentNode != null) {
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

  public void connectAnimatedNodeToView(int animatedNodeTag, int viewTag) {
    AnimatedNode node = mAnimatedNodes.get(animatedNodeTag);
    if (node != null) {
      throw new JSApplicationIllegalArgumentException("Animated node with tag " + animatedNodeTag +
              " does not exists");
    }
    if (!(node instanceof PropsAnimatedNode)) {
      throw new JSApplicationIllegalArgumentException("Animated node connected to view should be" +
              "of type " + PropsAnimatedNode.class.getName());
    }
    ((PropsAnimatedNode) node).setConnectedView(viewTag);
  }

  public void runAnimationStep(long frameTimeNanos) {
    /* prepare */
    for (int i = 0; i < mActiveAnimations.size(); i++) {
      mActiveAnimations.get(i).prepareAnimationStep();
    }
    for (int i = 0; i < mAnimatedNodes.size(); i++) {
      mAnimatedNodes.valueAt(i).prepareAnimationStep();
    }
    /* run animations steps on animated nodes graph starting with active animations */
    ArrayList<AnimatedNode> nodesToProcess = new ArrayList<AnimatedNode>(mActiveAnimations);
    ArrayList<PropsAnimatedNode> updatedPropNodes = new ArrayList<>();
    while (!nodesToProcess.isEmpty()) {
      AnimatedNode nextNode = nodesToProcess.remove(nodesToProcess.size() - 1);
      nextNode.runAnimationStep(frameTimeNanos);
      if (nextNode instanceof PropsAnimatedNode) {
        updatedPropNodes.add((PropsAnimatedNode) nextNode);
      } else if (nextNode instanceof AnimationDriver) {
        AnimationDriver animation = (AnimationDriver) nextNode;
        // check if animation has finished
      }
      for (int i = 0; i < nextNode.mChildren.size(); i++) {
        AnimatedNode child = nextNode.mChildren.get(i);
        child.feedDataFromUpdatedParent(nextNode);
        child.mVisitedIncommingNodes++;
        if (child.mVisitedIncommingNodes == child.mIncommingNodes) {
          nodesToProcess.add(child);
        }
      }
    }
    /* collect updates */
    mEnqueuedUpdates.clear();
    for (int i = 0; i < updatedPropNodes.size(); i++) {
      PropsAnimatedNode propNode = updatedPropNodes.get(i);
      mEnqueuedUpdates.add(propNode.createUpdateViewData());
    }
  }

  public void runUpdates(UIImplementation uiImplementation) {
    // Assert on native thread
    for (int i = 0; i < mEnqueuedUpdates.size(); i++) {
      UpdateViewData data = mEnqueuedUpdates.get(i);
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
          runAnimationStep(frameTimeNanos);
        }
      });
      ReactChoreographer.getInstance().postFrameCallback(
              ReactChoreographer.CallbackType.ANIMATIONS,
              this);
    }
  }
}
