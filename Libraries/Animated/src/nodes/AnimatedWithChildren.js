/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule AnimatedWithChildren
 * @flow
 * @format
 */
'use strict';

const AnimatedNode = require('./AnimatedNode');
const NativeAnimatedHelper = require('../NativeAnimatedHelper');

class AnimatedWithChildren extends AnimatedNode {
  _parentNodes;

  constructor(parentNodes) {
    super();
    this._parentNodes = parentNodes || [];
  }

  __attach(): void {
    super.__attach();
    this._parentNodes.forEach(node => node.__addChild(this));
  }

  __detach(): void {
    this._parentNodes.forEach(node => node.__removeChild(this));
    super.__detach();
  }
}

module.exports = AnimatedWithChildren;
