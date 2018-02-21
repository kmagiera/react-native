'use strict';

const AnimatedWithInput = require('./AnimatedWithInput');

class AnimatedSet extends AnimatedWithInput {
  _what;
  _value;

  constructor(what, value) {
    super([value]);
    this._what = what;
    this._value = value;
  }

  __onEvaluate() {
    const newValue = this._value.__getValue();
    this._what._updateValue(newValue);
    // console.log("SET", newValue);
    return newValue;
  }
}

module.exports = AnimatedSet;
