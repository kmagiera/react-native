/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule AnimatedTransform
 * @flow
 * @format
 */
'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');
const NativeAnimatedHelper = require('../NativeAnimatedHelper');

function extractAnimatedParentNodes(transforms) {
  const parents = [];
  transforms.forEach(transform => {
    const result = {};
    for (const key in transform) {
      const value = transform[key];
      if (value instanceof AnimatedNode) {
        parents.push(value);
      }
    }
  });
  return parents;
}

class AnimatedTransform extends AnimatedWithInput {
  _transforms: Array<Object>;

  constructor(transforms: Array<Object>) {
    super(extractAnimatedParentNodes(transforms));
    this._transforms = transforms;
  }

  __makeNative() {
    super.__makeNative();
    this._transforms.forEach(transform => {
      for (const key in transform) {
        const value = transform[key];
        if (value instanceof AnimatedNode) {
          value.__makeNative();
        }
      }
    });
  }

  __getProps() {
    return this._transforms.map(transform => {
      const result = {};
      for (const key in transform) {
        const value = transform[key];
        if (value instanceof AnimatedNode) {
          result[key] = value.__getProps();
        } else {
          result[key] = value;
        }
      }
      return result;
    });
  }

  __onEvaluate() {
    return this._transforms.map(transform => {
      const result = {};
      for (const key in transform) {
        const value = transform[key];
        if (value instanceof AnimatedNode) {
          result[key] = value.__getValue();
        }
      }
      return result;
    });
  }

  __getNativeConfig(): any {
    const transConfigs = [];

    this._transforms.forEach(transform => {
      for (const key in transform) {
        const value = transform[key];
        if (value instanceof AnimatedNode) {
          transConfigs.push({
            type: 'animated',
            property: key,
            nodeTag: value.__getNativeTag(),
          });
        } else {
          transConfigs.push({
            type: 'static',
            property: key,
            value,
          });
        }
      }
    });

    NativeAnimatedHelper.validateTransform(transConfigs);
    return {
      type: 'transform',
      transforms: transConfigs,
    };
  }
}

module.exports = AnimatedTransform;
