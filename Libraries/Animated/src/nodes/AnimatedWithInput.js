/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule AnimatedWithInput
 * @flow
 * @format
 */
'use strict';

const AnimatedNode = require('./AnimatedNode');
const NativeAnimatedHelper = require('../NativeAnimatedHelper');

class AnimatedWithInput extends AnimatedNode {
  __inputNodes;

  constructor(inputNodes) {
    super();
    this.__inputNodes = inputNodes;
  }

  __attach(): void {
    super.__attach();
    this.__inputNodes &&
      this.__inputNodes.forEach(node => node.__addChild(this));
  }

  __detach(): void {
    this.__inputNodes &&
      this.__inputNodes.forEach(node => node.__removeChild(this));
    super.__detach();
  }
}

module.exports = AnimatedWithInput;
