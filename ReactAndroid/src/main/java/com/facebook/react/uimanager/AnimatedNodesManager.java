package com.facebook.react.uimanager;

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
/*package*/ class AnimatedNodesManager {

  private static final int DEFAULT_ANIMATED_NODE_CHILD_COUNT = 1;

  private static class AnimatedNode {
    private @Nullable List<AnimatedNode> mChildren; /* lazy-initialized when child is added */
    private int mVisitedIncommingNodes = 0;
    private int mIncommingNodes = 0;

    double mValue = Double.NaN;

    public void addChild(AnimatedNode child) {
      if (mChildren == null) {
        mChildren = new ArrayList<>(DEFAULT_ANIMATED_NODE_CHILD_COUNT);
      }
      child.mIncommingNodes++;
      Assertions.assertNotNull(mChildren).add(child);
    }

    public void prepareAnimationStep() {
      mVisitedIncommingNodes = 0;
    }

    public void feedDataFromUpdatedParent(AnimatedNode parent) {
    }

    public void runAnimationStep(long frameTimeNanos) {
    }

    public void saveInPropMap(String key, SimpleMap propsMap) {
      propsMap.putDouble(key, mValue);
    }
  }

  private static class StyleAnimatedNode extends AnimatedNode {

    private final AnimatedNodesManager mNodesManager;
    private final Map<String, Integer> mPropMapping;

    StyleAnimatedNode(ReadableMap config, AnimatedNodesManager nodesManager) {
      ReadableMap style = config.getMap("style");
      ReadableMapKeySetIterator iter = style.keySetIterator();
      mPropMapping = new HashMap<>();
      while (iter.hasNextKey()) {
        String propKey = iter.nextKey();
        int nodeIndex = style.getInt(propKey);
        mPropMapping.put(propKey, nodeIndex);
      }
      mNodesManager = nodesManager;
    }

    @Override
    public void saveInPropMap(String key, SimpleMap propsMap) {
      /* ignore key, style names are flattened */
      for (String propKey : mPropMapping.keySet()) {
        // TODO: use entryset = optimize
        int nodeIndex = mPropMapping.get(propKey);
        AnimatedNode node = mNodesManager.mAnimatedNodes.get(nodeIndex);
        if (node != null) {
          node.saveInPropMap(propKey, propsMap);
        } else {
          throw new IllegalArgumentException("Mapped style node does not exists");
        }
      }
    }
  }

  private static class ValueAnimatedNode extends AnimatedNode {

    ValueAnimatedNode(ReadableMap config) {
      mValue = config.getDouble("value");
    }
  }

  private static class PropsAnimatedNode extends AnimatedNode {

    private int mConnectedViewTag = -1;
    private final AnimatedNodesManager mNodesManager;
    private final Map<String, Integer> mPropMapping;

    PropsAnimatedNode(ReadableMap config, AnimatedNodesManager nodesManager) {
      ReadableMap props = config.getMap("props");
      ReadableMapKeySetIterator iter = props.keySetIterator();
      mPropMapping = new HashMap<>();
      while (iter.hasNextKey()) {
        String propKey = iter.nextKey();
        int nodeIndex = props.getInt(propKey);
        mPropMapping.put(propKey, nodeIndex);
      }
      mNodesManager = nodesManager;
    }

    public void setConnectedView(int viewTag) {
      mConnectedViewTag = viewTag;
    }

    public UpdateViewData createUpdateViewData() {
      SimpleMap propsMap = new SimpleMap();
      for (String propKey : mPropMapping.keySet()) {
        // TODO: use entryset = optimize
        int nodeIndex = mPropMapping.get(propKey);
        AnimatedNode node = mNodesManager.mAnimatedNodes.get(nodeIndex);
        if (node != null) {
          node.saveInPropMap(propKey, propsMap);
        } else {
          throw new IllegalArgumentException("Mapped style node does not exists");
        }
      }
      return new UpdateViewData(mConnectedViewTag, propsMap);
    }
  }

  private static class InterpolationAnimatedNode extends AnimatedNode {

    private final double mInputRangeStart, mInputRangeEnd, mOutputRangeStart, mOutputRangeEnd;

    InterpolationAnimatedNode(ReadableMap config) {
      ReadableArray inputRange = config.getArray("inputRange");
      mInputRangeStart = inputRange.getDouble(0);
      mInputRangeEnd = inputRange.getDouble(1);
      ReadableArray outputRange = config.getArray("outputRange");
      mOutputRangeStart = outputRange.getDouble(0);
      mOutputRangeEnd = outputRange.getDouble(1);
    }

    @Override
    public void feedDataFromUpdatedParent(AnimatedNode parent) {
      mValue = interpolate(parent.mValue);
    }

    private double interpolate(double value) {
      return mOutputRangeStart + (mOutputRangeEnd - mOutputRangeStart) *
              (value - mInputRangeStart) / (mInputRangeEnd - mInputRangeStart);
    }
  }

  private static class TransformAnimatedNode extends AnimatedNode {

    private final AnimatedNodesManager mNodesManager;
    private final Map<String, Integer> mPropMapping;
    private final Map<String, Object> mStaticProps;

    TransformAnimatedNode(ReadableMap config, AnimatedNodesManager nodesManager) {
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
            mStaticProps.put(propKey, SimpleArray.copy(statics.getArray(propKey)));
            break;
        }
      }
      mNodesManager = nodesManager;
    }

    @Override
    public void saveInPropMap(String key, SimpleMap propsMap) {
      /* ignore key, style names are flattened */
      SimpleMap transformMap = new SimpleMap();
      for (String propKey : mPropMapping.keySet()) {
        // TODO: use entryset = optimize
        int nodeIndex = mPropMapping.get(propKey);
        AnimatedNode node = mNodesManager.mAnimatedNodes.get(nodeIndex);
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

  private static abstract class AnimationDriver {
    boolean mHasFinished = false;
    ValueAnimatedNode mAnimatedValue;
    public abstract boolean runAnimationStep(long frameTimeNanos);
  }

  private static class FrameBasedAnimation extends AnimationDriver {

    private long mStartFrameTimeNanos = -1;
    private final double[] mFrames;
    private final double mToValue;
    private double mFromValue;
    private boolean mHasToValue;

    FrameBasedAnimation(ReadableMap config) {
      ReadableArray frames = config.getArray("frames");
      int numberOfFrames = frames.size();
      mFrames = new double[numberOfFrames];
      for (int i = 0; i < numberOfFrames; i++) {
        mFrames[i] = frames.getDouble(i);
      }
      if (config.hasKey("toValue")) {
        mHasToValue = true;
        mToValue = config.getDouble("toValue");
      } else {
        mHasToValue = false;
        mToValue = Double.NaN;
      }
    }

    public boolean runAnimationStep(long frameTimeNanos) {
      if (mStartFrameTimeNanos < 0) {
        // start!
        mStartFrameTimeNanos = frameTimeNanos;
        mFromValue = mAnimatedValue.mValue;
      }
      long timeFromStartNanos = (frameTimeNanos - mStartFrameTimeNanos);
      int frameIndex = (int) (timeFromStartNanos / 1000000L / 16L);
      if (frameIndex < 0) {
        // weird, next time nanos is smaller than start time
        return false;
      } else if (!mHasFinished) {
        final double nextValue;
        if (frameIndex >= mFrames.length - 1) {
          // animation has ended!
          mHasFinished = true;
          if (mHasToValue) {
            nextValue = mToValue;
          } else {
            nextValue = mFromValue + mFrames[mFrames.length - 1];
          }
        } else if (mHasToValue) {
          nextValue = mFromValue + mFrames[frameIndex] * (mToValue - mFromValue);
        } else {
          nextValue = mFromValue + mFrames[frameIndex];
        }
        boolean updated = mAnimatedValue.mValue != nextValue;
        mAnimatedValue.mValue = nextValue;
        return updated;
      }
      return false;
    }
  }

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
      node = new StyleAnimatedNode(config, this);
    } else if ("value".equals(type)) {
      node = new ValueAnimatedNode(config);
    } else if ("transform".equals(type)) {
      node = new TransformAnimatedNode(config, this);
    } else if ("interpolation".equals(type)) {
      node = new InterpolationAnimatedNode(config);
    } else if ("props".equals(type)) {
      node = new PropsAnimatedNode(config, this);
    } else {
      throw new JSApplicationIllegalArgumentException("Unsupported node type: " + type);
    }
    mAnimatedNodes.put(tag, node);
  }

  public void startAnimatingNode(int animatedNodeTag, ReadableMap animationConfig) {
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
      animation = new FrameBasedAnimation(animationConfig);
    } else {
      throw new JSApplicationIllegalArgumentException("Unsupported animation type: " + type);
    }
    animation.mAnimatedValue = (ValueAnimatedNode) node;
    mActiveAnimations.add(animation);
  }

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
    ((PropsAnimatedNode) node).setConnectedView(viewTag);
  }

  private static Comparator<AnimatedNode> NODE_COMPARATOR = new Comparator<AnimatedNode>() {
    @Override
    public int compare(AnimatedNode lhs, AnimatedNode rhs) {
      return (lhs.mIncommingNodes - lhs.mVisitedIncommingNodes) -
              (rhs.mIncommingNodes - rhs.mVisitedIncommingNodes);
    }
  };

  public void runAnimationStep(long frameTimeNanos) {
    /* prepare */
    for (int i = 0; i < mAnimatedNodes.size(); i++) {
      mAnimatedNodes.valueAt(i).prepareAnimationStep();
    }
    /* run animations steps on animated nodes graph starting with active animations */
    PriorityQueue<AnimatedNode> nodesQueue = new PriorityQueue<>(4, NODE_COMPARATOR);
    for (int i = 0; i < mActiveAnimations.size(); i++) {
      AnimationDriver animation = mActiveAnimations.get(i);
      if (animation.runAnimationStep(frameTimeNanos)) {
        nodesQueue.add(animation.mAnimatedValue);
//        nodesToProcess.add(animation.mAnimatedValue);
      }
      if (animation.mHasFinished) {
        // TODO: Do this in O(1)
        mActiveAnimations.remove(animation);
      }
    }

    ArrayList<PropsAnimatedNode> updatedPropNodes = new ArrayList<>();
    while (!nodesQueue.isEmpty()) {
      AnimatedNode nextNode = nodesQueue.poll();
//      AnimatedNode nextNode = nodesToProcess.remove(nodesToProcess.size() - 1);
      nextNode.runAnimationStep(frameTimeNanos);
//      Log.e("CAT", "Run step " + nextNode + ", " + nextNode.mValue);
      if (nextNode instanceof PropsAnimatedNode) {
        updatedPropNodes.add((PropsAnimatedNode) nextNode);
      }
      if (nextNode.mChildren != null) {
        for (int i = 0; i < nextNode.mChildren.size(); i++) {
          AnimatedNode child = nextNode.mChildren.get(i);
          child.feedDataFromUpdatedParent(nextNode);
          child.mVisitedIncommingNodes++;
          nodesQueue.remove(child); /* Update key! */
          nodesQueue.add(child);
//          if (child.mVisitedIncommingNodes == child.mIncommingNodes) {
//            nodesToProcess.add(child);
//          }
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
    runAnimationStep(mLastFrameTimeNanos.get());
    for (int i = 0; i < mEnqueuedUpdates.size(); i++) {
      UpdateViewData data = mEnqueuedUpdates.get(i);
//      Log.e("CAT", "Update " + data.mViewTag + ", " + data.mProps);
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
//          runAnimationStep(frameTimeNanos);
          mReactContext.getNativeModule(UIManagerModule.class).dispatchViewUpdatesIfNotInJSBatch();
        }
      });
      ReactChoreographer.getInstance().postFrameCallback(
              ReactChoreographer.CallbackType.ANIMATIONS,
              this);
    }
  }
}
