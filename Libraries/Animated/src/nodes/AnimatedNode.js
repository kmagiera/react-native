/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule AnimatedNode
 * @flow
 * @format
 */
'use strict';

const NativeAnimatedHelper = require('../NativeAnimatedHelper');
const CoreAnimated = require('../CoreAnimated');

const invariant = require('fbjs/lib/invariant');

// Note(vjeux): this would be better as an interface but flow doesn't
// support them yet
class AnimatedNode {
  __attach(): void {}
  __detach(): void {
    if (this.__isNative && this.__nativeTag != null) {
      NativeAnimatedHelper.API.dropAnimatedNode(this.__nativeTag);
      this.__nativeTag = undefined;
    }
  }
  __getValue() {
    return CoreAnimated.evaluate(this);
  }
  __onEvaluate() {}
  __getProps(): any {
    return this.__getValue();
  }

  __getChildren(): Array<AnimatedNode> {
    return this.__children;
  }

  __addChild(child: AnimatedNode): void {
    if (this.__children.length === 0) {
      this.__attach();
    }
    this.__children.push(child);
    if (this.__isNative) {
      // Only accept "native" animated nodes as children
      child.__makeNative();
      NativeAnimatedHelper.API.connectAnimatedNodes(
        this.__getNativeTag(),
        child.__getNativeTag(),
      );
    }
  }

  __removeChild(child: AnimatedNode): void {
    const index = this.__children.indexOf(child);
    if (index === -1) {
      console.warn("Trying to remove a child that doesn't exist");
      return;
    }
    if (this.__isNative && child.__isNative) {
      NativeAnimatedHelper.API.disconnectAnimatedNodes(
        this.__getNativeTag(),
        child.__getNativeTag(),
      );
    }
    this.__children.splice(index, 1);
    if (this.__children.length === 0) {
      this.__detach();
    }
  }

  /* Methods and props used by native Animated impl */
  __lastLoopTs = 0;
  __memoizedValue = null;
  __isNative: boolean;
  __nativeTag: ?number;
  __children = [];
  __makeNative() {
    if (!this.__isNative) {
      throw new Error('This node cannot be made a "native" animated node');
    }
  }
  __getNativeTag(): ?number {
    NativeAnimatedHelper.assertNativeAnimatedModule();
    invariant(
      this.__isNative,
      'Attempt to get native tag from node not marked as "native"',
    );
    if (this.__nativeTag == null) {
      const nativeTag: ?number = NativeAnimatedHelper.generateNewNodeTag();
      NativeAnimatedHelper.API.createAnimatedNode(
        nativeTag,
        this.__getNativeConfig(),
      );
      this.__nativeTag = nativeTag;
    }
    return this.__nativeTag;
  }
  __getNativeConfig(): Object {
    throw new Error(
      'This JS animated node type cannot be used as native animated node',
    );
  }
  toJSON(): any {
    return this.__getValue();
  }
}

module.exports = AnimatedNode;
