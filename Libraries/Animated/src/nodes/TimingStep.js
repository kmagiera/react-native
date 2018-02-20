'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');

function val(valueOrNumber) {
  return valueOrNumber.__getValue ? valueOrNumber.__getValue() : valueOrNumber;
}

function timing(now, state, config, easing) {
  const time = state.time || now;
  const dt = (now - time);
  const frameTime = state.frameTime;
  const position = state.position;

  if (frameTime + dt >= config.duration) {
    state.position = config.toValue;
    // console.log("UPDATE FINISHED -> 1");
    state.finished = 1;
  } else {
    const lastProgress = easing(frameTime / config.duration)
    const dp = (easing((frameTime + dt) / config.duration) - lastProgress);
    const remaining = config.toValue - position;
    state.position = position + remaining * dp / (1 - lastProgress);
  }
  state.frameTime = frameTime + dt;
  state.time = now;
}

class TimingStep extends AnimatedWithInput {
  _state;
  _config;
  _easing;
  _clock;

  constructor(clock, state, config, easing) {
    super([clock]);
    this._clock = clock;
    this._state = state;
    this._config = config;
    this._easing = easing;
  }

  update() {
    this.__getValue();
  }

  __onEvaluate() {
    timing(this._clock.__getValue(), this._state, this._config, this._easing);
  }
}

module.exports = TimingStep;
