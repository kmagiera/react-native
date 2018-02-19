'use strict';

const AnimatedWithInput = require('../nodes/AnimatedWithInput');

class TimingStep extends AnimatedWithInput {
  constructor(what, value) {
    super([value]);
    this._what = what;
    this._value = value;
  }

  __onEvaluate() {
    const newValue = this._value.__getValue();
    this._what._updateValue(newValue);
    return newValue;
  }
}

module.exports = TimingStep;
