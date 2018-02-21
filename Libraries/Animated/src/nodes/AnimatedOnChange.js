'use strict';

const AnimatedNode = require('./AnimatedNode');
const AnimatedWithInput = require('./AnimatedWithInput');

class AnimatedOnChange extends AnimatedWithInput {
  _value;
  _what;
  _lastValue = null;

  constructor(value, what) {
    super([value]);
    this._value = value;
    this._what = what;
    this._lastValue = value.__getValue();
  }

  update() {
    // side effects
    this.__getValue();
  }

  __onEvaluate() {
    const newValue = this._value.__getValue();
    if (newValue !== this._lastValue) {
      this._what.__getValue();
      this._lastValue = newValue;
    }
  }
}

module.exports = AnimatedOnChange;
