'use strict';

const AnimatedValue = require('../nodes/AnimatedValue');
const AnimatedNode = require('../nodes/AnimatedNode');
const AnimatedValueXY = require('../nodes/AnimatedValueXY');
const TimingStep = require('../nodes/TimingStep');
const Animation = require('./Animation');

const {clock} = require('../nodes/AnimatedClock');
const AnimatedOnChange = require('../nodes/AnimatedOnChange');
const AnimatedDetach = require('../nodes/AnimatedDetach');

const {shouldUseNativeDriver} = require('../NativeAnimatedHelper');

let _easeInOut;
function easeInOut() {
  if (!_easeInOut) {
    const Easing = require('Easing');
    _easeInOut = Easing.inOut(Easing.ease);
  }
  return _easeInOut;
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

class TimingAnimation extends Animation {
  _startTime;
  _fromValue;
  _toValue;
  _duration;
  _delay;
  _easing;
  _onUpdate;
  _animationFrame;
  _timeout;
  _useNativeDriver;

  _finished;

  constructor(config) {
    super();
    this._config = {...config};
    this._toValue = config.toValue;
    this._easing = config.easing !== undefined ? config.easing : easeInOut();
    this._duration = config.duration !== undefined ? config.duration : 500;
    this._delay = config.delay !== undefined ? config.delay : 0;
    this.__iterations = config.iterations !== undefined ? config.iterations : 1;
    this.__isInteraction =
      config.isInteraction !== undefined ? config.isInteraction : true;
    this._useNativeDriver = shouldUseNativeDriver(config);
  }

  start(value) {
    this._finished = new AnimatedValue(0);
    const state = proxyAnimatedState({
      finished: this._finished,
      position: value,
      time: 0,
      frameTime: 0,
    });

    const config = {
      duration: this._duration,
      toValue: this._toValue,
    };

    const step = new TimingStep(clock, state, config, this._easing);
    new AnimatedOnChange(this._finished, new AnimatedDetach(step)).__attach();
    step.__attach();
  }

  stop() {
    this._finished && this._finished.setValue(1);
  }
}

module.exports = TimingAnimation;
