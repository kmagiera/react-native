/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 * @providesModule SpringAnimation
 * @flow
 * @format
 */
'use strict';

const AnimatedValue = require('../nodes/AnimatedValue');
const AnimatedNode = require('../nodes/AnimatedNode');
const AnimatedValueXY = require('../nodes/AnimatedValueXY');
const Animation = require('./Animation');
const SpringConfig = require('../SpringConfig');
const SpringNode = require('../nodes/SpringNode');
const AnimatedOnChange = require('../nodes/AnimatedOnChange');
const AnimatedDetach = require('../nodes/AnimatedDetach');
const AnimatedOp = require('../nodes/AnimatedOp');

const {clock} = require('../nodes/AnimatedClock');

const invariant = require('fbjs/lib/invariant');
const {shouldUseNativeDriver} = require('../NativeAnimatedHelper');

import type {AnimationConfig, EndCallback} from './Animation';

export type SpringAnimationConfig = AnimationConfig & {
  toValue: number | AnimatedValue | {x: number, y: number} | AnimatedValueXY,
  overshootClamping?: boolean,
  restDisplacementThreshold?: number,
  restSpeedThreshold?: number,
  velocity?: number | {x: number, y: number},
  bounciness?: number,
  speed?: number,
  tension?: number,
  friction?: number,
  stiffness?: number,
  damping?: number,
  mass?: number,
  delay?: number,
};

export type SpringAnimationConfigSingle = AnimationConfig & {
  toValue: number | AnimatedValue,
  overshootClamping?: boolean,
  restDisplacementThreshold?: number,
  restSpeedThreshold?: number,
  velocity?: number,
  bounciness?: number,
  speed?: number,
  tension?: number,
  friction?: number,
  stiffness?: number,
  damping?: number,
  mass?: number,
  delay?: number,
};

function withDefault<T>(value: ?T, defaultValue: T): T {
  if (value === undefined || value === null) {
    return defaultValue;
  }
  return value;
}

function proxyAnimatedState(target) {
  const handler = {
    get(target, key) {
      const value = target[key];
      if (value instanceof AnimatedNode) {
        return value.__getValue();
      }
      return value;
    },
    set(target, key, val) {
      const value = target[key];
      if (value instanceof AnimatedNode) {
        return value._updateValue(val);
      } else {
        target[key] = val;
      }
      return true;
    },
  };
  return new Proxy(target, handler);
}

class SpringAnimation extends Animation {
  _overshootClamping: boolean;
  _restDisplacementThreshold: number;
  _restSpeedThreshold: number;
  _lastVelocity: number;
  _startPosition: number;
  _lastPosition: number;
  _fromValue: number;
  _toValue: any;
  _stiffness: number;
  _damping: number;
  _mass: number;
  _initialVelocity: number;
  _delay: number;
  _timeout: any;
  _startTime: number;
  _lastTime: number;
  _frameTime: number;
  _onUpdate: (value: number) => void;
  _animationFrame: any;
  _useNativeDriver: boolean;

  constructor(config: SpringAnimationConfigSingle) {
    super();

    this._overshootClamping = withDefault(config.overshootClamping, false);
    this._restDisplacementThreshold = withDefault(
      config.restDisplacementThreshold,
      0.001,
    );
    this._restSpeedThreshold = withDefault(config.restSpeedThreshold, 0.001);
    this._initialVelocity = withDefault(config.velocity, 0);
    this._lastVelocity = withDefault(config.velocity, 0);
    this._toValue = config.toValue;
    this._delay = withDefault(config.delay, 0);
    this._useNativeDriver = shouldUseNativeDriver(config);
    this.__isInteraction =
      config.isInteraction !== undefined ? config.isInteraction : true;
    this.__iterations = config.iterations !== undefined ? config.iterations : 1;

    if (
      config.stiffness !== undefined ||
      config.damping !== undefined ||
      config.mass !== undefined
    ) {
      invariant(
        config.bounciness === undefined &&
          config.speed === undefined &&
          config.tension === undefined &&
          config.friction === undefined,
        'You can define one of bounciness/speed, tension/friction, or stiffness/damping/mass, but not more than one',
      );
      this._stiffness = withDefault(config.stiffness, 100);
      this._damping = withDefault(config.damping, 10);
      this._mass = withDefault(config.mass, 1);
    } else if (config.bounciness !== undefined || config.speed !== undefined) {
      // Convert the origami bounciness/speed values to stiffness/damping
      // We assume mass is 1.
      invariant(
        config.tension === undefined &&
          config.friction === undefined &&
          config.stiffness === undefined &&
          config.damping === undefined &&
          config.mass === undefined,
        'You can define one of bounciness/speed, tension/friction, or stiffness/damping/mass, but not more than one',
      );
      const springConfig = SpringConfig.fromBouncinessAndSpeed(
        withDefault(config.bounciness, 8),
        withDefault(config.speed, 12),
      );
      this._stiffness = springConfig.stiffness;
      this._damping = springConfig.damping;
      this._mass = 1;
    } else {
      // Convert the origami tension/friction values to stiffness/damping
      // We assume mass is 1.
      const springConfig = SpringConfig.fromOrigamiTensionAndFriction(
        withDefault(config.tension, 40),
        withDefault(config.friction, 7),
      );
      this._stiffness = springConfig.stiffness;
      this._damping = springConfig.damping;
      this._mass = 1;
    }

    invariant(this._stiffness > 0, 'Stiffness value must be greater than 0');
    invariant(this._damping > 0, 'Damping value must be greater than 0');
    invariant(this._mass > 0, 'Mass value must be greater than 0');
  }

  start(value) {
    this._finished = new AnimatedValue(0);
    const state = proxyAnimatedState({
      finished: this._finished,
      velocity: this._initialVelocity,
      position: value,
      time: 0,
    });

    const config = {
      damping: this._damping,
      mass: this._mass,
      stiffness: this._stiffness,
      toValue: this._toValue,
      overshootClamping: this._overshootClamping,
      restSpeedThreshold: this._restSpeedThreshold,
      restDisplacementThreshold: this._restDisplacementThreshold,
    };

    const step = new SpringNode(clock, state, config);
    const detach = new AnimatedDetach(step);
    const clb = finished => {
      console.log('FINISHED', finished);
    };
    const call = new AnimatedOp([this._finished], ([finished]) =>
      clb(finished),
    );
    const block = new AnimatedOp([detach, call], () => {});
    new AnimatedOnChange(this._finished, block).__attach();
    step.__attach();
  }

  stop() {
    this._finished && this._finished.setValue(1);
  }
}

module.exports = SpringAnimation;
